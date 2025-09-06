package jnu.ie.capstone.clova.service

import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import jnu.ie.capstone.grpc.NestConfig
import jnu.ie.capstone.grpc.NestData
import jnu.ie.capstone.grpc.NestRequest
import jnu.ie.capstone.grpc.NestResponse
import jnu.ie.capstone.grpc.NestServiceGrpc
import jnu.ie.capstone.grpc.RequestType
import jnu.ie.capstone.session.dto.request.VoiceRequestChunk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.stereotype.Service


private val logger = KotlinLogging.logger {}

@Service
class ClovaSpeechService(
    private val stub: NestServiceGrpc.NestServiceStub
) {

    suspend fun recognizeVoice(
        voiceStream: Flow<VoiceRequestChunk>,
        sendResult: suspend (String) -> Unit
    ) = coroutineScope {
        val cont = CompletableDeferred<Unit>()

        val responseObserver = object : StreamObserver<NestResponse> {
            override fun onNext(value: NestResponse) {
                val resultText = value.contents
                logger.info { "Clova partial result: $resultText" }

                launch {
                    sendResult(resultText)
                }
            }

            override fun onError(t: Throwable) {
                logger.error(t) { "Clova Speech gRPC error" }
                cont.completeExceptionally(t)
            }

            override fun onCompleted() {
                logger.info { "Clova Speech stream completed" }
                cont.complete(Unit)
            }
        }

        val requestObserver = stub.recognize(responseObserver)

        requestObserver.onNext(
            NestRequest.newBuilder()
                .setType(RequestType.CONFIG)
                .setConfig(
                    NestConfig.newBuilder()
                        .setConfig("""{"transcription":{"language":"ko"}}""")
                        .build()
                )
                .build()
        )

        launch {
            var seqId = 0
            voiceStream.collect { chunk ->
                requestObserver.onNext(
                    NestRequest.newBuilder()
                        .setType(RequestType.DATA)
                        .setData(
                            NestData.newBuilder()
                                .setChunk(ByteString.copyFrom(chunk.data))
                                .setExtraContents("""{"seqId":$seqId,"epFlag":false}""")
                                .build()
                        )
                        .build()
                )
                seqId++
            }

            requestObserver.onNext(
                NestRequest.newBuilder()
                    .setType(RequestType.DATA)
                    .setData(
                        NestData.newBuilder()
                            .setChunk(ByteString.EMPTY)
                            .setExtraContents("""{"seqId":-1,"epFlag":true}""")
                            .build()
                    )
                    .build()
            )
            requestObserver.onCompleted()
        }

        cont.await()
    }

}
