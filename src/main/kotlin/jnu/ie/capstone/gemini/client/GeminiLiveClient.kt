package jnu.ie.capstone.gemini.client

import com.google.genai.AsyncSession
import com.google.genai.Client
import com.google.genai.types.*
import jnu.ie.capstone.common.exception.server.InternalServerException
import jnu.ie.capstone.gemini.config.GeminiConfig
import jnu.ie.capstone.gemini.dto.client.request.GeminiInput
import jnu.ie.capstone.gemini.dto.client.response.GeminiOutput
import jnu.ie.capstone.gemini.constant.enums.GeminiModel
import jnu.ie.capstone.gemini.constant.enums.GeminiVoice
import jnu.ie.capstone.gemini.constant.function.GeminiFunction
import jnu.ie.capstone.gemini.dto.client.internal.Context
import jnu.ie.capstone.session.enums.SessionEvent
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class GeminiLiveClient(
    private val config: GeminiConfig
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val client = Client.builder().apiKey(config.apiKey).build()

    suspend fun getLiveResponse(
        inputData: Flow<GeminiInput>,
        prompt: String,
        model: GeminiModel = GeminiModel.GEMINI_2_5_FLASH_LIVE
    ): Flow<GeminiOutput> {
        return try {
            callbackFlow {
                val session = client.async.live
                    .connect(model.text, buildLiveConnectConfig(prompt))
                    .await()

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
            .tools(GeminiFunction.STATEMACHINE_TOOL)
            .responseModalities(Modality.Known.AUDIO)
            .inputAudioTranscription(AudioTranscriptionConfig.builder().build())
            .outputAudioTranscription(AudioTranscriptionConfig.builder().build())
            .realtimeInputConfig(buildRealTimeInputConfig())
            .systemInstruction(Content.fromParts(Part.fromText(prompt)))
            .speechConfig(
                SpeechConfig.builder().voiceConfig(
                    VoiceConfig.builder().prebuiltVoiceConfig(
                        PrebuiltVoiceConfig.builder().voiceName(GeminiVoice.ZEPHYR.text).build()
                    )
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

        message.serverContent().flatMap { it.inputTranscription() }
            .ifPresent {
                it.text()
                    .ifPresent { inputSTTChunk ->
                        inputSTTBuffer.append(inputSTTChunk)
                        trySend(GeminiOutput.InputSTT(inputSTTChunk))
                    }
            }

        message.serverContent().flatMap { it.outputTranscription() }
            .ifPresent {
                it.text()
                    .ifPresent { outputSTTChunk ->
                        outputSTTBuffer.append(outputSTTChunk)
                        trySend(GeminiOutput.OutputSTT(outputSTTChunk))
                    }
            }

        message.serverContent().flatMap { it.modelTurn() }.flatMap { it.parts() }
            .ifPresent { parts ->
                parts.forEach {
                    it.functionCall()
                        .map { call -> call.name()?.get() }
                        .map { functionName -> SessionEvent.fromText(functionName) }
                        .ifPresent { event -> trySend(GeminiOutput.OutputFunction(event)) }
                }
            }

        message.serverContent().flatMap { it.modelTurn() }.flatMap { it.parts() }
            .ifPresent { parts ->
                parts.forEach {
                    it.inlineData()
                        .map { blob -> blob.data()?.get() }
                        .ifPresent { data -> trySend(GeminiOutput.OutputVoiceStream(data)) }
                }
            }

        val isGeminiTurnComplete = message.serverContent().flatMap { it.turnComplete() }
            .orElse(false)

        if (isGeminiTurnComplete) {
            val finalInputSTT = inputSTTBuffer.toString()
            val finalOutputSTT = outputSTTBuffer.toString()

            logger.debug { "gemini가 대답을 완료했습니다. input STT: [$finalInputSTT], output STT: [$finalOutputSTT]" }

            if (finalInputSTT.isNotEmpty() || finalOutputSTT.isNotEmpty())
                trySend(GeminiOutput.EndOfGeminiTurn(finalInputSTT, finalOutputSTT))

            inputSTTBuffer.clear()
            outputSTTBuffer.clear()
        }
    }

    private suspend fun send(
        inputData: Flow<GeminiInput>,
        session: AsyncSession
    ) {
        inputData.collect {
            when (it) {
                is GeminiInput.Audio -> {
                    session.sendRealtimeInput(buildAudioContent(it.chunk))
                        .exceptionally { exception ->
                            logger.error(exception) { "audio chunk 보내는 중 에러 발생 -> ${exception.message}" }
                            return@exceptionally null
                        }.await()
                }

                is GeminiInput.Text -> {
                    session.sendRealtimeInput(
                        buildTextContent(it.context)
                    ).exceptionally {
                        logger.error(it) { "audio chunk 보내는 중 에러 발생 -> ${it.message}" }
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
                    .silenceDurationMs(config.silenceDurationMs)
            )
            .build()
    }

    private fun buildAudioContent(voiceChunk: ByteArray): LiveSendRealtimeInputParameters {
        return LiveSendRealtimeInputParameters.builder()
            .audio(Blob.builder().mimeType("audio/pcm").data(voiceChunk))
            .build()
    }

    private fun buildTextContent(context: Context): LiveSendRealtimeInputParameters {
        return LiveSendRealtimeInputParameters.builder()
            .text(context.toString())
            .build()
    }

}