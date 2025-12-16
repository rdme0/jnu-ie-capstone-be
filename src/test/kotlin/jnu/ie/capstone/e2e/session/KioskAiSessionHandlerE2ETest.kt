import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.websocket.ContainerProvider
import jnu.ie.capstone.Application
import jnu.ie.capstone.common.security.util.JwtUtil
import jnu.ie.capstone.common.websocket.constant.WebSocketConstant.BUFFER_SIZE
import jnu.ie.capstone.gemini.config.GeminiConfig
import jnu.ie.capstone.member.constant.MemberConstant.TEST_EMAIL
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.member.repository.MemberRepository
import jnu.ie.capstone.session.dto.internal.ServerReadyDTO
import jnu.ie.capstone.session.dto.internal.ShoppingCartMenuDTO
import jnu.ie.capstone.session.dto.internal.ShoppingCartResponseDTO
import jnu.ie.capstone.session.dto.internal.StateChangeDTO
import jnu.ie.capstone.session.dto.response.WebSocketTextResponse
import jnu.ie.capstone.session.enums.MessageType.*
import jnu.ie.capstone.session.enums.SessionState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.io.ResourceLoader
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.socket.*
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.BinaryWebSocketHandler
import java.net.URI
import java.util.concurrent.TimeUnit

@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@ExtendWith(SpringExtension::class)
class KioskAiSessionHandlerE2ETest(
    @param:LocalServerPort private var port: Int,
    private val resourceLoader: ResourceLoader,
    private val geminiConfig: GeminiConfig,
    private val memberRepository: MemberRepository,
    private val jwtUtil: JwtUtil,
    private val mapper: ObjectMapper
) {

    private companion object {
        val logger = KotlinLogging.logger {}
        const val STORE_ID = 1L
    }

    private val client: WebSocketClient by lazy {
        val webSocketContainer = ContainerProvider.getWebSocketContainer()
        webSocketContainer.defaultMaxBinaryMessageBufferSize = BUFFER_SIZE
        webSocketContainer.defaultMaxTextMessageBufferSize = BUFFER_SIZE
        StandardWebSocketClient(webSocketContainer)
    }

    private lateinit var accessToken: String

    private lateinit var readyLatch: CompletableDeferred<Unit>
    private val turnEndChannel = Channel<Unit>(Channel.UNLIMITED)
    private lateinit var stateChangeLatch: CompletableDeferred<SessionState>

    private var myShoppingCart: List<ShoppingCartMenuDTO> = mutableListOf()
    private var nowState: SessionState? = null

    @Volatile private var metricsStartTime: Long = 0L
    @Volatile private var isFirstPacketReceived: Boolean = false

    @BeforeEach
    fun setUp() {
        val member = memberRepository.findByEmail(TEST_EMAIL)
            ?: throw IllegalStateException("테스트 유저가 DB에 존재하지 않습니다: $TEST_EMAIL")
        accessToken = jwtUtil.generateToken(MemberInfo.from(member))
    }

    @Test
    @DisplayName("음성 파일을 순차적으로 전송하며 여러 턴에 걸친 대화를 테스트한다")
    fun kioskConversationE2ETest() {
        val connectionLatch = CompletableDeferred<Unit>()
        readyLatch = CompletableDeferred()
        val headers = WebSocketHttpHeaders()
        val session = getSession(connectionLatch, headers)

        runBlocking {
            withTimeout(5000) { connectionLatch.await() }

            withTimeout(10000) { readyLatch.await() }

            logger.info { "서버 준비 완료!" }

            assertThat(nowState).isEqualTo(SessionState.MENU_SELECTION)

            logger.info { "Gemini의 첫 인사 대기 (혹은 패스)" }
            val firstGreeting = withTimeoutOrNull(10000) {
                turnEndChannel.receive()
                logger.info("Gemini가 먼저 인사함!")

                while (turnEndChannel.tryReceive().isSuccess) {
                }
            }
            if (firstGreeting == null) logger.info("Gemini가 먼저 인사하지 않음 (조용)")

            logger.info { "--- PHASE 1 : '아샷추' 1잔 주문 ---" }

            while (turnEndChannel.tryReceive().isSuccess) {
            }

            sendWavFile(session, "classpath:test/아샷추.wav")

            metricsStartTime = System.currentTimeMillis()
            isFirstPacketReceived = false
            logger.info { "⏱️ Latency 측정 시작 (WAV 전송 완료)" }

            waitForGeminiTurnToEnd(session)

            logger.info { "--- PHASE 1 완료 ---" }

            assertThat(myShoppingCart).hasSize(1)
            assertThat(myShoppingCart).allSatisfy { it.name == "아이스티" }
            assertThat(myShoppingCart)
                .extracting(
                    ShoppingCartMenuDTO::name,
                    { it.options.first().name }
                )
                .containsExactly(
                    tuple("아이스티", "샷 추가")
                )

            logger.info { "--- PHASE 1 성공! ---" }

            delay(500)

            logger.info("--- PHASE 2 : '아아' 주문 ---")

            while (turnEndChannel.tryReceive().isSuccess) {
            }

            sendWavFile(session, "classpath:test/아아.wav")

            metricsStartTime = System.currentTimeMillis()
            isFirstPacketReceived = false
            logger.info { "⏱️ Latency 측정 시작 (WAV 전송 완료)" }

            waitForGeminiTurnToEnd(session)

            logger.info { "--- PHASE 2 완료 ---" }

            assertThat(myShoppingCart).hasSize(3)
            val onlyPhase2 = myShoppingCart.filterNot { it.name == "아이스티" }

            assertThat(onlyPhase2).hasSize(2)
            assertThat(onlyPhase2).extracting(
                ShoppingCartMenuDTO::name,
                { it.options.firstOrNull()?.name }
            ).containsExactlyInAnyOrder(
                tuple("아이스 아메리카노", "샷 추가"),
                tuple("아이스 아메리카노", null)
            )

            logger.info { "PHASE 2 성공!" }

            delay(500)

            logger.info("--- PHASE 3 : '이대로 주문해줘' ---")
            while (turnEndChannel.tryReceive().isSuccess) {
            }

            stateChangeLatch = CompletableDeferred()
            sendWavFile(session, "classpath:test/이대로 주문해줘.wav")

            metricsStartTime = System.currentTimeMillis()
            isFirstPacketReceived = false
            logger.info { "⏱️ Latency 측정 시작 (WAV 전송 완료)" }

            waitForGeminiTurnToEnd(session)

            logger.info { "--- PHASE 3 완료 ---" }


            logger.info { "nowState -> $nowState" }

            assertThat(nowState).isEqualTo(SessionState.PAYMENT_CONFIRMATION)

            logger.info { "모든 E2E 테스트 시나리오 완료. 5초 후 세션을 종료합니다." }
            delay(5000)
            session.close()
        }
    }

    private fun getSession(
        connectionLatch: CompletableDeferred<Unit>,
        headers: WebSocketHttpHeaders
    ): WebSocketSession = client.execute(
        object : BinaryWebSocketHandler() {
            override fun afterConnectionEstablished(session: WebSocketSession) {
                logger.info { "테스트 클라이언트 연결 성공: ${session.id}" }
                connectionLatch.complete(Unit)
            }

            override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                try {
                    val payload = message.payload
                    logger.debug { "테스트 클라이언트 수신 (Text): $payload" }

                    val response = mapper.readValue<WebSocketTextResponse>(payload)

                    when (response.messageType) {
                        SERVER_READY -> {
                            logger.info { ">>> 서버 준비 완료 신호 수신 <<<" }
                            nowState = (response.content as ServerReadyDTO).state
                            readyLatch.complete(Unit)
                        }

                        OUTPUT_TEXT_RESULT -> {
                            logger.info { ">>> Gemini 턴 종료 메시지 수신 <<<" }
                            turnEndChannel.trySend(Unit)
                        }

                        UPDATE_SHOPPING_CART -> {
                            myShoppingCart = (response.content as ShoppingCartResponseDTO).menus
                        }

                        CHANGE_STATE -> {
                            val toState = (response.content as StateChangeDTO).to
                            logger.info { ">>> State 변경 메시지 수신 : $toState <<<" }
                            nowState = toState
                            stateChangeLatch.complete(toState)
                        }

                        else -> {}
                    }

                } catch (e: Exception) {
                    logger.error("텍스트 메시지 처리 중 에러", e)
                }
            }

            override fun handleBinaryMessage(session: WebSocketSession, message: BinaryMessage) {
                if (!isFirstPacketReceived && metricsStartTime != 0L) {
                    val endTime = System.currentTimeMillis()
                    val latency = endTime - metricsStartTime

                    logger.info { "🚀 [Latency 측정] 첫 음성 응답까지 소요 시간: ${latency}ms" }

                    isFirstPacketReceived = true
                }

                logger.debug { "바이너리 메세지 수신 -> ${message.payloadLength}바이트" }
            }

            override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
                logger.warn("테스트 클라이언트 연결 종료: ${session.id}, status: $status")
            }

            override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
                logger.error("테스트 클라이언트 전송 에러: ${session.id}", exception)
            }
        },
        headers,
        URI("ws://localhost:${port}/stores/${STORE_ID}/websocket/kioskSession?accessToken=${accessToken}")
    )
        .get(5, TimeUnit.SECONDS)


    private suspend fun sendWavFile(session: WebSocketSession, resourcePath: String) {
        logger.info("음성 파일 스트리밍 시작: $resourcePath")
        val resource = resourceLoader.getResource(resourcePath)
        resource.inputStream.use { inputStream ->
            val buffer = ByteArray(3200)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val chunkToSend = if (bytesRead < buffer.size) buffer.copyOf(bytesRead) else buffer
                session.sendMessage(BinaryMessage(chunkToSend))
                delay(100)
            }
        }
        logger.info("음성 파일 스트리밍 종료: $resourcePath")
    }

    private suspend fun waitForGeminiTurnToEnd(
        session: WebSocketSession
    ) {
        logger.info("Gemini 턴 종료 대기 시작. 침묵 스트림을 전송합니다.")
        val silenceMs = geminiConfig.silenceDurationMs.toLong()
        val silentChunk = ByteArray(3200)
        val silenceIterations = (silenceMs / 100) + 5

        try {
            withTimeout(15000) {
                val silenceJob = launch {
                    repeat(silenceIterations.toInt()) {
                        if (!isActive) return@repeat
                        session.sendMessage(BinaryMessage(silentChunk))
                        delay(100)
                    }
                }

                try {
                    turnEndChannel.receive()
                } finally {
                    silenceJob.cancel()
                    logger.debug("침묵 전송 Job 취소됨")
                }
            }
        } catch (e: Exception) {
            logger.error("Gemini 턴을 기다리는 도중 타임아웃 또는 에러 발생", e)
            throw e
        }
    }
}