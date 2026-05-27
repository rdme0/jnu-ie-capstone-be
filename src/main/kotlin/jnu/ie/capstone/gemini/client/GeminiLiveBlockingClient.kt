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
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.jvm.optionals.getOrNull

@Component
class GeminiLiveBlockingClient(
    private val config: GeminiConfig,
    private val mapper: ObjectMapper
) : GeminiLiveBlockingGateway {
    private companion object {
        const val API_VERSION = "v1alpha"
        val logger = KotlinLogging.logger {}
    }

    private val client = Client.builder()
        .apiKey(config.apiKey)
        .httpOptions(HttpOptions.builder().apiVersion(API_VERSION).build())
        .build()

    override fun connect(
        prompt: String,
        model: GeminiModel
    ): GeminiLiveBlockingSession {
        return try {
            val session = client.async.live
                .connect(model.text, buildLiveConnectConfig(prompt))
                .get()

            val outputQueue = LinkedBlockingQueue<GeminiOutput>()
            val inputSTTBuffer = StringBuilder()
            val outputSTTBuffer = StringBuilder()

            session.receive { message ->
                onMessageReceived(message, outputQueue, inputSTTBuffer, outputSTTBuffer)
            }

            GeminiLiveBlockingSession(session, outputQueue)
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
                                    .voiceName(GeminiVoice.ZEPHYR.text)
                                    .build()
                            ).build()
                    )
            )
            .build()
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

    private fun onMessageReceived(
        message: LiveServerMessage,
        outputQueue: BlockingQueue<GeminiOutput>,
        inputSTTBuffer: StringBuilder,
        outputSTTBuffer: StringBuilder
    ) {
        logger.debug { "gemini blocking received -> $message" }
        processInputSTT(message, outputQueue, inputSTTBuffer)
        processOutputSTT(message, outputQueue, outputSTTBuffer)
        processOutputVoiceStream(message, outputQueue)
        processFunctionCall(message, outputQueue)
        processTurnComplete(message, outputQueue, outputSTTBuffer, inputSTTBuffer)
    }

    private fun processInputSTT(
        message: LiveServerMessage,
        outputQueue: BlockingQueue<GeminiOutput>,
        inputSTTBuffer: StringBuilder
    ) {
        message.serverContent().flatMap { it.inputTranscription() }
            .ifPresent {
                it.text().ifPresent { inputSTTChunk ->
                    inputSTTBuffer.append(inputSTTChunk)
                    outputQueue.offer(GeminiOutput.InputSTT(inputSTTChunk))
                }
            }
    }

    private fun processOutputSTT(
        message: LiveServerMessage,
        outputQueue: BlockingQueue<GeminiOutput>,
        outputSTTBuffer: StringBuilder
    ) {
        message.serverContent().flatMap { it.outputTranscription() }
            .ifPresent {
                it.text().ifPresent { outputSTTChunk ->
                    outputSTTBuffer.append(outputSTTChunk)
                    outputQueue.offer(GeminiOutput.OutputSTT(outputSTTChunk))
                }
            }
    }

    private fun processOutputVoiceStream(
        message: LiveServerMessage,
        outputQueue: BlockingQueue<GeminiOutput>
    ) {
        message.serverContent().flatMap { it.modelTurn() }.flatMap { it.parts() }
            .ifPresent { parts ->
                parts.forEach {
                    it.inlineData()
                        .map { blob -> blob.data()?.get() }
                        .ifPresent { data -> outputQueue.offer(GeminiOutput.VoiceStream(data)) }
                }
            }
    }

    private fun processFunctionCall(
        message: LiveServerMessage,
        outputQueue: BlockingQueue<GeminiOutput>
    ) {
        message.toolCall().getOrNull()?.functionCalls()?.ifPresent { call ->
            call.map { Triple(it.id()?.get(), it.name()?.get(), it.args()?.get()) }
                .forEach { (id, name, items) ->
                    logger.info { "blocking 함수 이름 : $name, items : $items" }

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

                    outputQueue.offer(
                        GeminiOutput.FunctionCall(
                            id = id ?: "",
                            signature = enum,
                            params = params
                        )
                    )
                }
        }
    }

    private fun processTurnComplete(
        message: LiveServerMessage,
        outputQueue: BlockingQueue<GeminiOutput>,
        outputSTTBuffer: StringBuilder,
        inputSTTBuffer: StringBuilder
    ) {
        val isTurnComplete = message.serverContent()
            .flatMap { it.generationComplete() }
            .orElse(false)

        if (isTurnComplete && outputSTTBuffer.isNotEmpty()) {
            val finalInputSTT = inputSTTBuffer.toString()
            val finalOutputSTT = outputSTTBuffer.toString()

            logger.info { "blocking gemini turn complete. input: [$finalInputSTT], output: [$finalOutputSTT]" }

            outputQueue.offer(GeminiOutput.EndOfGeminiTurn(finalInputSTT, finalOutputSTT))

            inputSTTBuffer.clear()
            outputSTTBuffer.clear()
        }
    }
}

class GeminiLiveBlockingSession(
    private val session: AsyncSession,
    private val outputQueue: BlockingQueue<GeminiOutput>
) : GeminiLiveBlockingSessionGateway {

    override fun send(input: GeminiInput) {
        when (input) {
            is GeminiInput.Audio -> {
                session.sendRealtimeInput(buildAudioContent(input.chunk)).get()
            }

            is GeminiInput.Text -> {
                session.sendRealtimeInput(buildTextContent(input.context)).get()
            }

            is GeminiInput.ToolResponse -> {
                val functionResponse = FunctionResponse.builder()
                    .id(input.id)
                    .name(input.functionName)
                    .response(mapOf("result" to input.result))
                    .build()

                val params = LiveSendToolResponseParameters.builder()
                    .functionResponses(functionResponse)
                    .build()

                session.sendToolResponse(params).get()
            }
        }
    }

    override fun takeOutput(): GeminiOutput = outputQueue.take()

    override fun close() {
        session.close()
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
