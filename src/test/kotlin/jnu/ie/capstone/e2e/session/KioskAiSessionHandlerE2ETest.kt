package jnu.ie.capstone.e2e.session

import jnu.ie.capstone.Application
import jnu.ie.capstone.common.security.util.JwtUtil
import jnu.ie.capstone.member.service.MemberCoordinateService
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.io.ResourceLoader
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.socket.*
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
    private val memberService: MemberCoordinateService,
    private val jwtUtil: JwtUtil,
    private val resourceLoader: ResourceLoader
) {
    private val client = StandardWebSocketClient()
    private val receivedMessages = ArrayBlockingQueue<ByteArray>(10)
    private lateinit var token: String

    @BeforeEach
    fun setUp() {
        val member = memberService.get(1)
        token = jwtUtil.generateToken(member!!)
    }

    @AfterEach
    fun tearDown() {
        receivedMessages.clear()
    }

    @Test
    @DisplayName("클라이언트가 연결 후 바이너리 메시지를 전송하면 핸들러가 처리한다")
    fun e2e() {
        assertDoesNotThrow {
            val latch = CompletableDeferred<Unit>()
            val headers = WebSocketHttpHeaders()
            headers.add("Sec-WebSocket-Protocol", "Bearer $token")
            val session = client.execute(object : BinaryWebSocketHandler() {
                override fun afterConnectionEstablished(session: WebSocketSession) {
                    latch.complete(Unit)
                }

                override fun handleBinaryMessage(
                    session: WebSocketSession,
                    message: BinaryMessage
                ) {
                    val bytes = ByteArray(message.payload.remaining())
                    message.payload.get(bytes)
                    receivedMessages.offer(bytes)
                }
            }, headers, URI("ws://localhost:${port}/websocket/voice"))
                .get(3, TimeUnit.SECONDS)

            runBlocking {
                latch.await()

                // https://www.aihub.or.kr/aihubdata/data/view.do?currMenu=115&topMenu=100&aihubDataSe=realm&dataSetSn=123
                val resource = resourceLoader.getResource("classpath:test/sst-test.wav")

                val buffer = ByteArray(3200)

                resource.inputStream.use { stream ->
                    while (true) {
                        val bytesRead = stream.read(buffer)
                        if (bytesRead <= 0) break

                        val chunkToSend =
                            if (bytesRead < buffer.size) buffer.copyOf(bytesRead) else buffer
                        session.sendMessage(BinaryMessage(chunkToSend))

                        delay(100)
                    }
                }
                delay(10000)
                logger.info { "음성 스트리밍 끝!" }
                session.close()
            }
        }
        logger.info { "stt 성공" }
    }
}
