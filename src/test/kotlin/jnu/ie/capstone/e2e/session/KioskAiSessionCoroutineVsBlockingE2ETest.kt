package jnu.ie.capstone.e2e.session

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.websocket.ContainerProvider
import jnu.ie.capstone.Application
import jnu.ie.capstone.common.security.util.JwtUtil
import jnu.ie.capstone.common.websocket.constant.WebSocketConstant.BUFFER_SIZE
import jnu.ie.capstone.gemini.client.GeminiLiveBlockingGateway
import jnu.ie.capstone.gemini.client.GeminiLiveBlockingSessionGateway
import jnu.ie.capstone.gemini.client.GeminiLiveGateway
import jnu.ie.capstone.gemini.constant.enums.GeminiFunctionSignature.CONFIRM_PAYMENT
import jnu.ie.capstone.gemini.constant.enums.GeminiModel
import jnu.ie.capstone.gemini.dto.client.request.GeminiInput
import jnu.ie.capstone.gemini.dto.client.response.GeminiOutput
import jnu.ie.capstone.member.constant.MemberConstant.TEST_EMAIL
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.member.repository.MemberRepository
import jnu.ie.capstone.session.dto.internal.ServerReadyDTO
import jnu.ie.capstone.session.dto.response.WebSocketTextResponse
import jnu.ie.capstone.session.enums.MessageType.CHANGE_STATE
import jnu.ie.capstone.session.enums.MessageType.OUTPUT_TEXT_RESULT
import jnu.ie.capstone.session.enums.MessageType.SERVER_READY
import jnu.ie.capstone.session.enums.SessionState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.BinaryWebSocketHandler
import java.net.URI
import java.util.Collections
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

@SpringBootTest(
    classes = [Application::class, KioskAiSessionCoroutineVsBlockingE2ETest.FakeGeminiConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "kiosk.bench.sessions=50",
        "kiosk.bench.audio-chunks=30",
        "kiosk.bench.chunk-delay-ms=5"
    ]
)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KioskAiSessionCoroutineVsBlockingE2ETest(
    @param:LocalServerPort private var port: Int,
    private val memberRepository: MemberRepository,
    private val jwtUtil: JwtUtil,
    private val mapper: ObjectMapper
) {
    private companion object {
        val logger = KotlinLogging.logger {}
        const val STORE_ID = 1L
        const val CHUNK_SIZE = 3200
        const val SESSION_COUNT = 50
        const val AUDIO_CHUNKS = 30
        const val CHUNK_DELAY_MS = 5L
        const val SESSION_TIMEOUT_MS = 300_000L
        const val SWEEP_SESSION_TIMEOUT_MS = 60_000L
        val SWEEP_SESSION_COUNTS = listOf(50, 75, 100, 125, 150)
    }

    private val client: WebSocketClient by lazy {
        val webSocketContainer = ContainerProvider.getWebSocketContainer()
        webSocketContainer.defaultMaxBinaryMessageBufferSize = BUFFER_SIZE
        webSocketContainer.defaultMaxTextMessageBufferSize = BUFFER_SIZE
        StandardWebSocketClient(webSocketContainer)
    }

    private lateinit var accessToken: String

    @BeforeEach
    fun setUp() {
        val member = memberRepository.findByEmail(TEST_EMAIL)
            ?: throw IllegalStateException("테스트 유저가 DB에 존재하지 않습니다: $TEST_EMAIL")
        accessToken = jwtUtil.generateToken(MemberInfo.from(member))
    }

    @Test
    @DisplayName("fake Gemini로 코루틴 WebSocket 경로와 blocking WebSocket 경로를 다중 세션 부하 비교한다")
    fun compareCoroutineAndBlockingWithFakeGeminiMultiSession() {
        runBlocking {
            val coroutine = runLoad(
                label = "coroutine",
                path = "/stores/$STORE_ID/websocket/kioskSession",
                sessionCount = SESSION_COUNT
            )
            val blocking = runLoad(
                label = "blocking",
                path = "/bench/stores/$STORE_ID/websocket/kioskSession-blocking",
                sessionCount = SESSION_COUNT
            )

            logger.info {
                """
                [Coroutine vs Blocking Fake Gemini Load Metrics]
                sessions=$SESSION_COUNT, audioChunks=$AUDIO_CHUNKS, chunkDelayMs=$CHUNK_DELAY_MS
                coroutine = $coroutine
                blocking  = $blocking
                """.trimIndent()
            }

            assertThat(coroutine.successCount).isEqualTo(SESSION_COUNT)
            assertThat(blocking.successCount).isEqualTo(SESSION_COUNT)
        }
    }

    @Test
    @DisplayName("fake Gemini로 세션 수를 올리며 코루틴과 blocking 경로의 실패 시작점을 확인한다")
    fun sweepCoroutineAndBlockingCapacityWithFakeGemini() {
        runBlocking {
            val coroutinePath = "/stores/$STORE_ID/websocket/kioskSession"
            val blockingPath = "/bench/stores/$STORE_ID/websocket/kioskSession-blocking"

            SWEEP_SESSION_COUNTS.forEach { sessionCount ->
                val coroutine = runLoad(
                    label = "coroutine-$sessionCount",
                    path = coroutinePath,
                    sessionCount = sessionCount,
                    sessionTimeoutMs = SWEEP_SESSION_TIMEOUT_MS
                )
                val blocking = runLoad(
                    label = "blocking-$sessionCount",
                    path = blockingPath,
                    sessionCount = sessionCount,
                    sessionTimeoutMs = SWEEP_SESSION_TIMEOUT_MS
                )

                logger.info {
                    """
                    [Coroutine vs Blocking Capacity Sweep]
                    sessions=$sessionCount, timeoutMs=$SWEEP_SESSION_TIMEOUT_MS, audioChunks=$AUDIO_CHUNKS, chunkDelayMs=$CHUNK_DELAY_MS
                    coroutine = $coroutine
                    blocking  = $blocking
                    """.trimIndent()
                }
            }
        }
    }

    private suspend fun runLoad(
        label: String,
        path: String,
        sessionCount: Int,
        sessionTimeoutMs: Long = SESSION_TIMEOUT_MS
    ): LoadMetrics = coroutineScope {
        val startedAt = System.currentTimeMillis()
        val results = Collections.synchronizedList(mutableListOf<SessionMetrics>())

        (1..sessionCount).map { index ->
            async {
                val result = runSession(label, path, index, sessionTimeoutMs)
                results.add(result)
            }
        }.awaitAll()

        val sortedLatencies = results.asSequence()
            .filter { it.success }
            .map { it.firstResponseLatencyMs }
            .sorted()
            .toList()
        val totalMs = System.currentTimeMillis() - startedAt

        LoadMetrics(
            label = label,
            sessionCount = sessionCount,
            successCount = results.count { it.success },
            totalMs = totalMs,
            avgFirstResponseLatencyMs = sortedLatencies.average(),
            p95FirstResponseLatencyMs = percentile(sortedLatencies, 0.95),
            maxFirstResponseLatencyMs = sortedLatencies.lastOrNull() ?: -1
        )
    }

    private suspend fun runSession(
        label: String,
        path: String,
        index: Int,
        sessionTimeoutMs: Long
    ): SessionMetrics {
        val state = SessionStateHolder()
        val connectionLatch = CompletableDeferred<Unit>()
        val startedAt = System.currentTimeMillis()
        var session: WebSocketSession? = null

        return try {
            session = getSession(path, state, connectionLatch)
            withTimeout(sessionTimeoutMs.milliseconds) { connectionLatch.await() }
            withTimeout(sessionTimeoutMs.milliseconds) { state.readyLatch.await() }

            state.metricsStartTime = System.currentTimeMillis()
            repeat(AUDIO_CHUNKS) {
                session.sendMessage(BinaryMessage(ByteArray(CHUNK_SIZE) { index.toByte() }))
                delay(CHUNK_DELAY_MS.milliseconds)
            }

            withTimeout(sessionTimeoutMs.milliseconds) { state.turnEndChannel.receive() }

            val latency = state.firstResponseLatencyMs ?: -1
            logger.info { "[$label][$index] fake first response latency=${latency}ms" }

            SessionMetrics(
                success = state.nowState == SessionState.PAYMENT_CONFIRMATION,
                firstResponseLatencyMs = latency,
                totalMs = System.currentTimeMillis() - startedAt
            )
        } catch (exception: Exception) {
            logger.warn(exception) { "[$label][$index] fake session failed" }
            SessionMetrics(
                success = false,
                firstResponseLatencyMs = -1,
                totalMs = System.currentTimeMillis() - startedAt
            )
        } finally {
            session?.close()
        }
    }

    private fun getSession(
        path: String,
        state: SessionStateHolder,
        connectionLatch: CompletableDeferred<Unit>
    ): WebSocketSession = client.execute(
        object : BinaryWebSocketHandler() {
            override fun afterConnectionEstablished(session: WebSocketSession) {
                connectionLatch.complete(Unit)
            }

            override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                val response = mapper.readValue<WebSocketTextResponse>(message.payload)

                when (response.messageType) {
                    SERVER_READY -> {
                        state.nowState = (response.content as ServerReadyDTO).state
                        state.readyLatch.complete(Unit)
                    }
                    OUTPUT_TEXT_RESULT -> state.turnEndChannel.trySend(Unit)
                    CHANGE_STATE -> state.nowState =
                        (response.content as jnu.ie.capstone.session.dto.internal.StateChangeDTO).to
                    else -> {}
                }
            }

            override fun handleBinaryMessage(session: WebSocketSession, message: BinaryMessage) {
                if (!state.isFirstPacketReceived && state.metricsStartTime != 0L) {
                    state.firstResponseLatencyMs = System.currentTimeMillis() - state.metricsStartTime
                    state.isFirstPacketReceived = true
                }
            }
        },
        WebSocketHttpHeaders(),
        URI("ws://localhost:$port$path?accessToken=$accessToken")
    ).get(5, TimeUnit.SECONDS)

    private fun percentile(sorted: List<Long>, percentile: Double): Long {
        if (sorted.isEmpty()) return -1
        val index = ceil(sorted.size * percentile).toInt().coerceIn(1, sorted.size) - 1
        return sorted[index]
    }

    private class SessionStateHolder {
        val readyLatch = CompletableDeferred<Unit>()
        val turnEndChannel = Channel<Unit>(Channel.UNLIMITED)
        @Volatile var nowState: SessionState? = null
        @Volatile var metricsStartTime: Long = 0L
        @Volatile var isFirstPacketReceived: Boolean = false
        @Volatile var firstResponseLatencyMs: Long? = null
    }

    private data class SessionMetrics(
        val success: Boolean,
        val firstResponseLatencyMs: Long,
        val totalMs: Long
    )

    private data class LoadMetrics(
        val label: String,
        val sessionCount: Int,
        val successCount: Int,
        val totalMs: Long,
        val avgFirstResponseLatencyMs: Double,
        val p95FirstResponseLatencyMs: Long,
        val maxFirstResponseLatencyMs: Long
    )

    @TestConfiguration
    class FakeGeminiConfig {
        @Bean
        @Primary
        fun fakeGeminiLiveGateway(): GeminiLiveGateway = FakeCoroutineGeminiLiveGateway()

        @Bean
        @Primary
        fun fakeGeminiLiveBlockingGateway(): GeminiLiveBlockingGateway = FakeBlockingGeminiLiveGateway()
    }

    private class FakeCoroutineGeminiLiveGateway : GeminiLiveGateway {
        override suspend fun getLiveResponse(
            geminiReadySignal: CompletableDeferred<Unit>,
            inputData: Flow<GeminiInput>,
            prompt: String,
            model: GeminiModel
        ): Flow<GeminiOutput> = channelFlow {
            geminiReadySignal.complete(Unit)

            launch {
                var audioCount = 0
                var responded = false

                inputData.collect { input ->
                    if (input is GeminiInput.Audio) {
                        audioCount++
                    }

                    if (!responded && audioCount >= 10) {
                        responded = true
                        delay(25.milliseconds)
                        send(GeminiOutput.VoiceStream(ByteArray(512)))
                        send(GeminiOutput.FunctionCall("fake-confirm", CONFIRM_PAYMENT, jnu.ie.capstone.gemini.dto.client.response.GeminiFunctionParams.NoParams))
                        delay(5.milliseconds)
                        send(GeminiOutput.OutputSTT("결제 단계로 넘어갑니다."))
                        send(GeminiOutput.EndOfGeminiTurn("fake input", "결제 단계로 넘어갑니다."))
                    }
                }
            }
        }
    }

    private class FakeBlockingGeminiLiveGateway : GeminiLiveBlockingGateway {
        override fun connect(
            prompt: String,
            model: GeminiModel
        ): GeminiLiveBlockingSessionGateway = FakeBlockingGeminiSession()
    }

    private class FakeBlockingGeminiSession : GeminiLiveBlockingSessionGateway {
        private val audioCount = AtomicInteger(0)
        private val outputQueue = LinkedBlockingQueue<GeminiOutput>()
        @Volatile private var responded = false

        override fun send(input: GeminiInput) {
            if (input is GeminiInput.Audio && audioCount.incrementAndGet() >= 10 && !responded) {
                responded = true
                Thread.sleep(25)
                outputQueue.offer(GeminiOutput.VoiceStream(ByteArray(512)))
                outputQueue.offer(GeminiOutput.FunctionCall("fake-confirm", CONFIRM_PAYMENT, jnu.ie.capstone.gemini.dto.client.response.GeminiFunctionParams.NoParams))
                Thread.sleep(5)
                outputQueue.offer(GeminiOutput.OutputSTT("결제 단계로 넘어갑니다."))
                outputQueue.offer(GeminiOutput.EndOfGeminiTurn("fake input", "결제 단계로 넘어갑니다."))
            }
        }

        override fun takeOutput(): GeminiOutput = outputQueue.take()

        override fun close() {
        }
    }
}
