package jnu.ie.capstone.rtzr.service

import jakarta.websocket.DeploymentException
import jnu.ie.capstone.common.exception.server.InternalServerException
import jnu.ie.capstone.rtzr.cache.service.RtzrAccessTokenService
import jnu.ie.capstone.rtzr.client.RtzrSttClient
import jnu.ie.capstone.rtzr.dto.client.response.RtzrSttResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class RtzrSttService(
    private val client: RtzrSttClient,
    private val cacheService: RtzrAccessTokenService
) {
    private companion object {
        const val TOTAL_ATTEMPTS = 2
    }

    private val logger = KotlinLogging.logger {}

    suspend fun stt(
        voiceStream: Flow<ByteArray>,
        scope: CoroutineScope,
        rtzrReadySignal: CompletableDeferred<Unit>
    ): Flow<RtzrSttResponse> {
        return flow {
            var accessToken: String = cacheService.get()
                ?: cacheService.overwrite(client.auth().accessToken)

            for (attempt in 1..TOTAL_ATTEMPTS) {
                try {
                    emitAll(client.stt(voiceStream, accessToken, scope, rtzrReadySignal))
                    return@flow
                } catch (e: DeploymentException) {
                    val is401Error = e.message?.contains("401") ?: false

                    if (is401Error && attempt < TOTAL_ATTEMPTS) {
                        logger.warn(e) { "액세스 토큰 만료. 재발급 후 재시도합니다. (시도: $attempt)" }
                        accessToken = cacheService.overwrite(client.auth().accessToken)
                        continue
                    } else throw InternalServerException(e)

                } catch (e: Exception) {
                    throw InternalServerException(e)
                }
            }
            throw InternalServerException(IllegalStateException("STT 연결 실패"))
        }
    }
}