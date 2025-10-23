package jnu.ie.capstone.e2e.session

import jakarta.websocket.ContainerProvider
import jnu.ie.capstone.Application
import jnu.ie.capstone.common.security.util.JwtUtil
import jnu.ie.capstone.common.websocket.constant.WebSocketConstant.BUFFER_SIZE
import jnu.ie.capstone.gemini.config.GeminiConfig
import jnu.ie.capstone.member.constant.MemberConstant.TEST_EMAIL
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.member.repository.MemberRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.io.ResourceLoader
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.BinaryWebSocketHandler
import java.net.URI
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@ExtendWith(SpringExtension::class)
class KioskAiSessionHandlerE2ETest(
    @param:LocalServerPort
    private var port: Int,
    private val resourceLoader: ResourceLoader,
    private val geminiConfig: GeminiConfig,
    private val memberRepository: MemberRepository,
    private val jwtUtil: JwtUtil
) {

    private companion object {
        const val STORE_ID = 1
    }

    private val client: WebSocketClient by lazy {
        val webSocketContainer = ContainerProvider.getWebSocketContainer()
        webSocketContainer.defaultMaxBinaryMessageBufferSize = BUFFER_SIZE
        webSocketContainer.defaultMaxTextMessageBufferSize = BUFFER_SIZE

        StandardWebSocketClient(webSocketContainer)
    }

    private val receivedMessages = ArrayBlockingQueue<ByteArray>(10)
    private lateinit var accessToken: String

    @BeforeEach
    fun setUp() {
        val member = memberRepository.findByEmail(TEST_EMAIL)
            ?: throw IllegalStateException("test user not found")

        accessToken = jwtUtil.generateToken(MemberInfo.from(member))
    }

    @AfterEach
    fun tearDown() {
        receivedMessages.clear()
    }

    @Test
    @DisplayName("클라이언트가 연결 후 바이너리 메시지를 전송하면 핸들러가 처리한다")
    fun e2e() {
        val latch = CompletableDeferred<Unit>()
        val headers = WebSocketHttpHeaders()
        headers.add("Sec-WebSocket-Protocol", "Bearer $accessToken")
        val session = getSession(latch, headers)

        runBlocking {
            latch.await()

            val resource = resourceLoader.getResource("classpath:test/아아.wav")
            val inputStream = resource.inputStream
            val totalBytes = inputStream.available()
            val halfwayPoint = totalBytes / 2
            val buffer = ByteArray(3200)

            val silenceMs = geminiConfig.silenceDurationMs.toLong() + 1000L
            val silentChunk = ByteArray(buffer.size)
            val silenceIterations = silenceMs / 100

            sendSilence(silenceIterations, session, silentChunk, silenceMs)

            var bytesSent = 0
            while (bytesSent < halfwayPoint) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead <= 0) break

                val chunkToSend = buffer.copyOf(bytesRead)
                session.sendMessage(BinaryMessage(chunkToSend))
                bytesSent += bytesRead
                delay(100)
            }


            logger.info { "음성 스트리밍 중간 지점 도달" }
//            sendSilence(silenceIterations, session, silentChunk, silenceMs)


            // 3. 나머지 파일 전송
            while (true) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead <= 0) break

                val chunkToSend = buffer.copyOf(bytesRead)
                session.sendMessage(BinaryMessage(chunkToSend))
                delay(100)
            }

            // 4. 끝 침묵
            logger.info { "음성 스트리밍 끝 지점 도달" }
            sendSilence(silenceIterations, session, silentChunk, silenceMs)

            // 5. 마무리
            logger.info { "음성 스트리밍 끝! delay를 시작합니다." }
            delay(10000)
            session.close()
        }
        logger.info { "음성 스트리밍 e2e 끝" }
    }

    private fun getSession(
        latch: CompletableDeferred<Unit>,
        headers: WebSocketHttpHeaders
    ): WebSocketSession = client.execute(object : BinaryWebSocketHandler() {
        override fun afterConnectionEstablished(session: WebSocketSession) {
            logger.info { "테스트 클라이언트 연결 성공: ${session.id}" }
            latch.complete(Unit)
        }

        override fun handleBinaryMessage(
            session: WebSocketSession,
            message: BinaryMessage
        ) {
            try {
                val bytes = ByteArray(message.payload.remaining())
                message.payload.get(bytes)
                receivedMessages.offer(bytes)
            } catch (e: Exception) {
                logger.error(e) { "테스트 클라이언트 바이너리 메시지 처리 중 에러" }
            }
        }

        override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
            try {
                logger.info { "테스트 클라이언트 수신 (Text): ${message.payload}" }
            } catch (e: Exception) {
                logger.error(e) { "테스트 클라이언트 텍스트 메시지 처리 중 에러" }
            }
        }

        override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
            logger.warn { "테스트 클라이언트 연결 종료: ${session.id}, status: $status" }
        }

        override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
            logger.error(exception) { "테스트 클라이언트 전송 에러: ${session.id}" }
        }
    }, headers, URI("ws://localhost:${port}/stores/${STORE_ID}/websocket/kioskSession"))
        .get(3, TimeUnit.SECONDS)

    private suspend fun sendSilence(
        silenceIterations: Long,
        session: WebSocketSession,
        silentChunk: ByteArray,
        silenceMs: Long
    ) {
        logger.info { "${silenceMs / 1000}초간 침묵 스트림 전송 시작" }

        repeat(silenceIterations.toInt()) {
            session.sendMessage(BinaryMessage(silentChunk))
            delay(100)
        }

        logger.info { "${silenceMs / 1000}초 침묵 스트림 전송 종료" }
    }
}