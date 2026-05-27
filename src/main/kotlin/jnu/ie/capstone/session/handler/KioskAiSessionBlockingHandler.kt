package jnu.ie.capstone.session.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jnu.ie.capstone.common.security.dto.KioskUserDetails
import jnu.ie.capstone.common.websocket.util.WebSocketBlockingReplier
import jnu.ie.capstone.session.dto.internal.ShoppingCartDTO
import jnu.ie.capstone.session.dto.request.WebSocketTextRequest
import jnu.ie.capstone.session.dto.response.WebSocketTextResponse
import jnu.ie.capstone.session.enums.MessageType
import jnu.ie.capstone.session.enums.SessionEvent
import jnu.ie.capstone.session.enums.SessionState
import jnu.ie.capstone.session.registry.WebSocketSessionRegistry
import jnu.ie.capstone.session.service.KioskSessionBlockingService
import mu.KotlinLogging
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.beans.factory.annotation.Value
import org.springframework.statemachine.StateMachine
import org.springframework.statemachine.config.StateMachineFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.BinaryWebSocketHandler
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@Component
class KioskAiSessionBlockingHandler(
    private val service: KioskSessionBlockingService,
    private val stateMachineFactory: StateMachineFactory<SessionState, SessionEvent>,
    private val sessionRegistry: WebSocketSessionRegistry,
    private val mapper: ObjectMapper,
    @param:Value("\${spring.threads.virtual.enabled:false}")
    private val virtualThreadsEnabled: Boolean
) : BinaryWebSocketHandler() {
    private companion object {
        const val CLIENT_VOICE_QUEUE_KEY = "blockingClientVoiceQueue"
        const val VOICE_QUEUE_SIZE_KEY = "blockingVoiceQueueSize"
        const val SHOPPING_CART_KEY = "shoppingCart"
        const val REPLIER_KEY = "blockingReplier"
        const val RUNNING_KEY = "blockingRunning"
        const val PRINCIPAL_KEY = "principal"
        const val STORE_ID_KEY = "storeId"
        val logger = KotlinLogging.logger {}
    }

    private val sessionStateMachines =
        ConcurrentHashMap<String, StateMachine<SessionState, SessionEvent>>()
    private val sessionExecutors = ConcurrentHashMap<String, ExecutorService>()
    private val sharedPlatformExecutor: ExecutorService by lazy {
        Executors.newFixedThreadPool(64)
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        logger.info { "blocking 연결 시작 -> ${session.id}" }

        sessionRegistry.register(session)
        val stateMachine = initializeStateMachine(session)
        val executor = createExecutor()
        sessionExecutors[session.id] = executor

        val replier = WebSocketBlockingReplier(mapper, session)
        session.attributes[REPLIER_KEY] = replier

        val running = AtomicBoolean(true)
        session.attributes[RUNNING_KEY] = running

        val queueSize = AtomicInteger(0)
        session.attributes[VOICE_QUEUE_SIZE_KEY] = queueSize

        val clientVoiceQueue: BlockingQueue<ByteArray> = LinkedBlockingQueue(128)
        session.attributes[CLIENT_VOICE_QUEUE_KEY] = clientVoiceQueue

        val authentication = session.attributes[PRINCIPAL_KEY] as? UsernamePasswordAuthenticationToken
        val userDetails = authentication?.principal as? KioskUserDetails
            ?: run {
                logger.error { "올바르지 않은 authentication -> ${session.attributes[PRINCIPAL_KEY]}" }
                session.close(CloseStatus.POLICY_VIOLATION)
                return
            }

        val storeId = session.attributes[STORE_ID_KEY] as? Long
            ?: run {
                logger.error { "올바르지 않은 storeId -> ${session.attributes[STORE_ID_KEY]}" }
                session.close(CloseStatus.BAD_DATA)
                return
            }

        session.attributes[SHOPPING_CART_KEY] = ShoppingCartDTO(mutableListOf())

        executor.submit { replier.drain() }
        executor.submit {
            try {
                service.processVoiceChunk(
                    voiceQueue = clientVoiceQueue,
                    queueSize = queueSize,
                    running = running,
                    storeId = storeId,
                    ownerInfo = userDetails.memberInfo,
                    stateMachine = stateMachine,
                    session = session,
                    executor = executor,
                    onReady = {
                        replier.send(WebSocketTextResponse.fromServerReady())
                        logger.info { "blocking 클라이언트(${session.id})에게 준비 완료 신호 전송" }
                    },
                    onReply = { message -> replier.send(message) }
                )
            } catch (e: Exception) {
                if (running.get()) logger.error(e) { "blocking voice chunk 처리 중 에러 -> ${session.id}" }
            }
        }
    }

    override fun handleBinaryMessage(session: WebSocketSession, message: BinaryMessage) {
        val clientVoiceQueue = session.attributes[CLIENT_VOICE_QUEUE_KEY] as BlockingQueue<ByteArray>
        val queueSize = session.attributes[VOICE_QUEUE_SIZE_KEY] as AtomicInteger

        val bytes = ByteArray(message.payload.remaining())
        message.payload.get(bytes)

        if (clientVoiceQueue.offer(bytes)) {
            queueSize.incrementAndGet()
        } else {
            clientVoiceQueue.poll()
            queueSize.decrementAndGet()
            if (clientVoiceQueue.offer(bytes)) queueSize.incrementAndGet()
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val request: WebSocketTextRequest = runCatching {
            mapper.readValue<WebSocketTextRequest>(message.payload)
        }.getOrElse {
            logger.error(it) { "blocking TextMessage parsing error: ${message.payload}" }
            return
        }

        if (request.messageType != MessageType.PROCESS_PAYMENT) return

        val stateMachine = sessionStateMachines[session.id] ?: return
        val replier = session.attributes[REPLIER_KEY] as? WebSocketBlockingReplier ?: return

        sessionExecutors[session.id]?.submit {
            service.processPayment(stateMachine) { response -> replier.send(response) }
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.info { "blocking Client disconnected: ${session.id}, code: ${status.code}, reason: ${status.reason}" }

        (session.attributes[RUNNING_KEY] as? AtomicBoolean)?.set(false)
        (session.attributes[REPLIER_KEY] as? WebSocketBlockingReplier)?.close()

        sessionStateMachines.remove(session.id)
        sessionRegistry.unregister(session.id)
        service.cleanupSession(session.id)
        sessionExecutors.remove(session.id)?.let {
            if (virtualThreadsEnabled) it.shutdownNow()
        }
    }

    private fun initializeStateMachine(session: WebSocketSession): StateMachine<SessionState, SessionEvent> {
        val stateMachine = stateMachineFactory.getStateMachine("blocking-${session.id}")
        stateMachine.startReactively().block()
        sessionStateMachines[session.id] = stateMachine
        return stateMachine
    }

    private fun createExecutor(): ExecutorService {
        return if (virtualThreadsEnabled) {
            Executors.newVirtualThreadPerTaskExecutor()
        } else {
            sharedPlatformExecutor
        }
    }
}
