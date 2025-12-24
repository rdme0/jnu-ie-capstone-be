package jnu.ie.capstone.gemini.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.genai.AsyncSession
import com.google.genai.Client
import com.google.genai.types.*
import jnu.ie.capstone.common.exception.server.InternalServerException
import jnu.ie.capstone.gemini.config.GeminiConfig
import jnu.ie.capstone.gemini.constant.enums.GeminiFunctionSignature
import jnu.ie.capstone.gemini.constant.enums.GeminiModel
import jnu.ie.capstone.gemini.constant.enums.GeminiVoice
import jnu.ie.capstone.gemini.constant.function.GeminiFunctionDeclaration
import jnu.ie.capstone.gemini.dto.client.internal.Context
import jnu.ie.capstone.gemini.dto.client.request.GeminiInput
import jnu.ie.capstone.gemini.dto.client.response.GeminiFunctionParams
import jnu.ie.capstone.gemini.dto.client.response.GeminiOutput
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

@Component
class GeminiLiveClient(
    private val config: GeminiConfig,
    private val mapper: ObjectMapper
) {

    companion object {
        private const val API_VERSION = "v1alpha"
        private val logger = KotlinLogging.logger {}
    }

    private val client = Client.builder()
        .apiKey(config.apiKey)
        .httpOptions(HttpOptions.builder().apiVersion(API_VERSION).build())
        .build()

    suspend fun getLiveResponse(
        geminiReadySignal: CompletableDeferred<Unit>,
        inputData: Flow<GeminiInput>,
        prompt: String,
        model: GeminiModel = GeminiModel.GEMINI_2_5_FLASH_NATIVE_AUDIO
    ): Flow<GeminiOutput> {
        return try {
            callbackFlow {
                val session = client.async.live
                    .connect(model.text, buildLiveConnectConfig(prompt))
                    .await()

                geminiReadySignal.complete(Unit)

                logger.debug { "Gemini Live 세션이 연결되었습니다." }

                val inputSTTBuffer = StringBuilder()
                val outputSTTBuffer = StringBuilder()

                session.receive { onMessageReceived(it, inputSTTBuffer, outputSTTBuffer) }
                launch { send(inputData, session) }
                awaitClose { onClosed(session) }
            }
        } catch (e: Exception) {
            throw InternalServerException(e)
        }
    }

    private fun buildLiveConnectConfig(prompt: String): LiveConnectConfig {
        return LiveConnectConfig.builder()
            .tools(
                GeminiFunctionDeclaration.RAG_SEARCH_TOOL,
                GeminiFunctionDeclaration.STATEMACHINE_TOOL
            )
            .responseModalities(Modality.Known.AUDIO)
            .inputAudioTranscription(AudioTranscriptionConfig.builder().build())
            .outputAudioTranscription(AudioTranscriptionConfig.builder().build())
            .realtimeInputConfig(buildRealTimeInputConfig())
            .systemInstruction(Content.fromParts(Part.fromText(prompt)))
            .proactivity(ProactivityConfig.builder().proactiveAudio(true).build())
            .speechConfig(
                SpeechConfig.builder()
                    .languageCode("ko-KR")
                    .voiceConfig(
                        VoiceConfig.builder()
                            .prebuiltVoiceConfig(
                                PrebuiltVoiceConfig.builder()
                                    .voiceName(
                                        GeminiVoice.ZEPHYR.text
                                    ).build()
                            ).build()
                    )
            )
            .build()
    }

    private fun ProducerScope<GeminiOutput>.onMessageReceived(
        message: LiveServerMessage,
        inputSTTBuffer: StringBuilder,
        outputSTTBuffer: StringBuilder
    ) {
        logger.debug { "gemini received -> $message" }

        processInputSTT(message, inputSTTBuffer)
        processOutputSTT(message, outputSTTBuffer)
        processOutputVoiceStream(message)
        processFunctionCall(message)
        processTurnComplete(message, outputSTTBuffer, inputSTTBuffer)
    }

    private fun ProducerScope<GeminiOutput>.processInputSTT(
        message: LiveServerMessage,
        inputSTTBuffer: StringBuilder
    ) {
        message.serverContent().flatMap { it.inputTranscription() }
            .ifPresent {
                it.text()
                    .ifPresent { inputSTTChunk ->
                        inputSTTBuffer.append(inputSTTChunk)
                        trySend(GeminiOutput.InputSTT(inputSTTChunk))
                    }
            }
    }

    private fun ProducerScope<GeminiOutput>.processOutputSTT(
        message: LiveServerMessage,
        outputSTTBuffer: StringBuilder
    ) {
        message.serverContent().flatMap { it.outputTranscription() }
            .ifPresent {
                it.text()
                    .ifPresent { outputSTTChunk ->
                        outputSTTBuffer.append(outputSTTChunk)
                        trySend(GeminiOutput.OutputSTT(outputSTTChunk))
                    }
            }
    }

    private fun ProducerScope<GeminiOutput>.processOutputVoiceStream(
        message: LiveServerMessage
    ) {
        message.serverContent().flatMap { it.modelTurn() }.flatMap { it.parts() }
            .ifPresent { parts ->
                parts.forEach {
                    it.inlineData()
                        .map { blob -> blob.data()?.get() }
                        .ifPresent { data -> trySend(GeminiOutput.VoiceStream(data)) }
                }
            }
    }

    private fun ProducerScope<GeminiOutput>.processFunctionCall(
        message: LiveServerMessage
    ) {
        message.toolCall().getOrNull()?.functionCalls()?.ifPresent { call ->
            call.map { Triple(it.id()?.get(), it.name()?.get(), it.args()?.get()) }
                .forEach { (id, name, items) ->

                    logger.info { "함수 이름 : $name, items : $items" }

                    val enum = name
                        ?.let { GeminiFunctionSignature.fromText(it) }
                        ?: run {
                            logger.error { "function name error -> $name" }
                            return@forEach
                        }

                    val params = if (items.isNullOrEmpty()) {
                        GeminiFunctionParams.NoParams
                    } else {
                        runCatching {
                            mapper.convertValue(items, enum.paramsType.java)
                        }.getOrElse {
                            logger.error(it) { "function params convert error" }
                            return@forEach
                        }
                    }

                    val functionId = id ?: ""

                    val output = GeminiOutput
                        .FunctionCall(id = functionId, signature = enum, params = params)

                    trySend(output)
                }
        }
    }


    private fun ProducerScope<GeminiOutput>.processTurnComplete(
        message: LiveServerMessage,
        outputSTTBuffer: StringBuilder,
        inputSTTBuffer: StringBuilder
    ) {
        val isTurnComplete = message.serverContent()
            .flatMap { it.generationComplete() }
            .orElse(false)

        if (isTurnComplete && outputSTTBuffer.isNotEmpty()) {
            val finalInputSTT = inputSTTBuffer.toString()
            val finalOutputSTT = outputSTTBuffer.toString()

            logger.info { "gemini가 대답을 완료했습니다. input STT: [$finalInputSTT], output STT: [$finalOutputSTT]" }

            trySend(GeminiOutput.EndOfGeminiTurn(finalInputSTT, finalOutputSTT))

            inputSTTBuffer.clear()
            outputSTTBuffer.clear()
        }
    }


    private suspend fun send(
        inputData: Flow<GeminiInput>,
        session: AsyncSession
    ) {
        inputData.collect { input ->
            when (input) {
                is GeminiInput.Audio -> {
                    session.sendRealtimeInput(buildAudioContent(input.chunk))
                        .exceptionally { exception ->
                            logger.error(exception) { "audio chunk 보내는 중 에러 발생 -> ${exception.message}" }
                            return@exceptionally null
                        }
                        .await()
                }

                is GeminiInput.Text -> {
                    session.sendRealtimeInput(
                        buildTextContent(input.context)
                    ).exceptionally { exception ->
                        logger.error(exception) { "audio chunk 보내는 중 에러 발생 -> ${exception.message}" }
                        return@exceptionally null
                    }.await()
                }

                is GeminiInput.ToolResponse -> {
                    logger.info { "Tool Response 전송: ID=${input.id}, Result=${input.result}" }

                    val functionResponse = FunctionResponse.builder()
                        .id(input.id)
                        .name(input.functionName)
                        .response(mapOf("result" to input.result))
                        .build()

                    val params = LiveSendToolResponseParameters.builder()
                        .functionResponses(functionResponse)
                        .build()

                    session.sendToolResponse(params)
                        .exceptionally { e ->
                            logger.error(e) { "Tool Response 전송 실패" }
                            return@exceptionally null
                        }.await()
                }
            }
        }
    }

    private fun onClosed(session: AsyncSession) {
        logger.info { "Flow가 닫힙니다. 세션을 종료합니다." }
        session.close()
    }


    private fun buildRealTimeInputConfig(): RealtimeInputConfig {
        return RealtimeInputConfig.builder()
            .automaticActivityDetection(
                AutomaticActivityDetection.builder()
                    .startOfSpeechSensitivity(StartSensitivity.Known.START_SENSITIVITY_LOW)
                    .silenceDurationMs(config.silenceDurationMs)
            )
            .build()
    }

    private fun buildAudioContent(voiceChunk: ByteArray): LiveSendRealtimeInputParameters {
        return LiveSendRealtimeInputParameters.builder()
            .audio(Blob.builder().mimeType("audio/pcm;rate=16000").data(voiceChunk))
            .build()
    }

    private fun buildTextContent(context: Context): LiveSendRealtimeInputParameters {
        return LiveSendRealtimeInputParameters.builder()
            .text(context.toString())
            .build()
    }
}
