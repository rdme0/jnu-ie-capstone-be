package jnu.ie.capstone.clova.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import jnu.ie.capstone.clova.dto.internal.ExtraContents
import jnu.ie.capstone.clova.dto.internal.NestConfigDTO
import jnu.ie.capstone.clova.dto.internal.TranscriptionConfig
import jnu.ie.capstone.clova.enums.ClovaSpeechLanguage
import jnu.ie.capstone.grpc.NestConfig
import jnu.ie.capstone.grpc.NestData
import jnu.ie.capstone.grpc.NestRequest
import jnu.ie.capstone.grpc.NestResponse
import jnu.ie.capstone.grpc.NestServiceGrpc
import jnu.ie.capstone.grpc.RequestType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class ClovaSpeechService(
    private val stub: NestServiceGrpc.NestServiceStub,
    private val mapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}
    private val nestConfig = NestConfigDTO(TranscriptionConfig(language = ClovaSpeechLanguage.ko))

    suspend fun recognizeVoice(
        voiceStream: Flow<ByteArray>,
        sendResult: suspend (String) -> Unit
    ) = coroutineScope {
        val streamCompleted = CompletableDeferred<Unit>()

        val responseObserver = object : StreamObserver<NestResponse> {
            override fun onNext(value: NestResponse) {
                launch {
                    val resultText = value.contents
                    logger.info { "Clova partial result: $resultText" }
                    sendResult(resultText)
                }
            }

            override fun onError(t: Throwable) {
                logger.error(t) { "Clova Speech gRPC error" }
                streamCompleted.completeExceptionally(t)
            }

            override fun onCompleted() {
                logger.info { "Clova Speech stream completed" }
                streamCompleted.complete(Unit)
            }
        }

        val requestObserver = stub.recognize(responseObserver)

        requestObserver.sendConfig()

        launch {
            try {
                var seqId = 0L
                voiceStream.collect { chunk ->
                    requestObserver.sendData(chunk, seqId++, false)
                }
                requestObserver.sendData(ByteArray(0), -1, true)
                logger.info { "Clova Speech stream completed" }
                requestObserver.onCompleted()
            } catch (e: Exception) {
                logger.error(e) { "Error while collecting voice stream. Notifying server." }
                requestObserver.onError(e)
                throw e
            }
        }

        streamCompleted.await()
    }

    private fun StreamObserver<NestRequest>.sendConfig() {
        val configRequest = NestRequest.newBuilder()
            .setType(RequestType.CONFIG)
            .setConfig(
                NestConfig.newBuilder()
                    .setConfig(mapper.writeValueAsString(nestConfig))
                    .build()
            )
            .build()
        this.onNext(configRequest)
    }

    private fun StreamObserver<NestRequest>.sendData(
        chunk: ByteArray,
        seqId: Long,
        isEnd: Boolean
    ) {
        val extra = ExtraContents(seqId = seqId, epFlag = isEnd)
        val dataRequest = NestRequest.newBuilder()
            .setType(RequestType.DATA)
            .setData(
                NestData.newBuilder()
                    .setChunk(ByteString.copyFrom(chunk))
                    .setExtraContents(mapper.writeValueAsString(extra))
                    .build()
            )
            .build()
        this.onNext(dataRequest)
    }
}