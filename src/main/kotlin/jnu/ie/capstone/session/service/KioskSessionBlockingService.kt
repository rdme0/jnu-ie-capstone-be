package jnu.ie.capstone.session.service

import jnu.ie.capstone.gemini.client.GeminiLiveBlockingGateway
import jnu.ie.capstone.gemini.client.GeminiLiveBlockingSessionGateway
import jnu.ie.capstone.gemini.config.PromptConfig
import jnu.ie.capstone.gemini.constant.enums.GeminiFunctionSignature.*
import jnu.ie.capstone.gemini.dto.client.internal.Context.MenuSelectionContext
import jnu.ie.capstone.gemini.dto.client.internal.Context.NoContext
import jnu.ie.capstone.gemini.dto.client.request.GeminiInput
import jnu.ie.capstone.gemini.dto.client.response.GeminiFunctionParams.AddItems
import jnu.ie.capstone.gemini.dto.client.response.GeminiFunctionParams.RemoveItems
import jnu.ie.capstone.gemini.dto.client.response.GeminiOutput
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.menu.dto.internal.MenuInternalDTO
import jnu.ie.capstone.menu.service.MenuCoordinateService
import jnu.ie.capstone.session.dto.internal.OutputTextChunkDTO
import jnu.ie.capstone.session.dto.internal.OutputTextResultDTO
import jnu.ie.capstone.session.dto.internal.ShoppingCartDTO
import jnu.ie.capstone.session.dto.internal.StateChangeDTO
import jnu.ie.capstone.session.dto.response.WebSocketBinaryResponse
import jnu.ie.capstone.session.dto.response.WebSocketResponse
import jnu.ie.capstone.session.dto.response.WebSocketTextResponse
import jnu.ie.capstone.session.enums.SessionEvent
import jnu.ie.capstone.session.enums.SessionState
import jnu.ie.capstone.session.enums.SessionState.MENU_SELECTION
import jnu.ie.capstone.session.service.internal.KioskShoppingCartService
import mu.KotlinLogging
import org.springframework.messaging.support.MessageBuilder
import org.springframework.statemachine.StateMachine
import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class KioskSessionBlockingService(
    private val liveClient: GeminiLiveBlockingGateway,
    private val menuService: MenuCoordinateService,
    private val promptConfig: PromptConfig,
    private val shoppingCartService: KioskShoppingCartService
) {
    private companion object {
        const val SHOPPING_CART_KEY = "shoppingCart"
        const val OK = "ok"
        val logger = KotlinLogging.logger {}
        val stateMachineLocks = ConcurrentHashMap<String, ReentrantLock>()
    }

    fun processVoiceChunk(
        voiceQueue: BlockingQueue<ByteArray>,
        queueSize: AtomicInteger,
        running: AtomicBoolean,
        storeId: Long,
        ownerInfo: MemberInfo,
        stateMachine: StateMachine<SessionState, SessionEvent>,
        session: WebSocketSession,
        executor: ExecutorService,
        onReady: () -> Unit,
        onReply: (WebSocketResponse) -> Unit
    ) {
        liveClient.connect(
            prompt = promptConfig.kiosk,
            model = jnu.ie.capstone.gemini.constant.enums.GeminiModel.GEMINI_2_5_FLASH_NATIVE_AUDIO
        ).use { geminiSession ->
            onReady()

            val shoppingCart = session.attributes[SHOPPING_CART_KEY] as ShoppingCartDTO
            geminiSession.send(contextByState(stateMachine.state.id, shoppingCart))

            val outputTask = executor.submit {
                consumeGeminiOutputs(
                    geminiSession,
                    running,
                    stateMachine,
                    storeId,
                    ownerInfo,
                    shoppingCart,
                    onReply
                )
            }

            while (running.get() && session.isOpen) {
                val chunk = voiceQueue.poll(200, TimeUnit.MILLISECONDS)
                if (chunk != null) {
                    queueSize.decrementAndGet()
                    geminiSession.send(GeminiInput.Audio(chunk))
                }
            }

            outputTask.cancel(true)
        }
    }

    fun processPayment(
        stateMachine: StateMachine<SessionState, SessionEvent>,
        onReply: (WebSocketResponse) -> Unit
    ) {
        val stateChange = executeStateTransition(stateMachine, SessionEvent.PROCESS_PAYMENT)

        if (stateChange != null) {
            onReply(WebSocketTextResponse.fromStateChange(stateChange))
        } else {
            logger.error { "상태가 바뀌지 않음 -> oldState: ${stateMachine.state.id}, event: ${SessionEvent.PROCESS_PAYMENT}" }
        }
    }

    fun cleanupSession(sessionId: String) {
        stateMachineLocks.remove(sessionId)
        logger.info { "Cleaned up blocking lock for session: $sessionId" }
    }

    private fun consumeGeminiOutputs(
        geminiSession: GeminiLiveBlockingSessionGateway,
        running: AtomicBoolean,
        stateMachine: StateMachine<SessionState, SessionEvent>,
        storeId: Long,
        ownerInfo: MemberInfo,
        shoppingCart: ShoppingCartDTO,
        onReply: (WebSocketResponse) -> Unit
    ) {
        while (running.get()) {
            val output = runCatching { geminiSession.takeOutput() }
                .getOrElse {
                    if (running.get()) logger.error(it) { "blocking gemini output receive error" }
                    return
                }

            handleGeminiOutput(
                output,
                geminiSession,
                stateMachine,
                storeId,
                ownerInfo,
                shoppingCart,
                onReply
            )
        }
    }

    private fun contextByState(
        nowState: SessionState,
        shoppingCart: ShoppingCartDTO
    ): GeminiInput.Text = when (nowState) {
        MENU_SELECTION -> GeminiInput.Text(MenuSelectionContext(shoppingCart = shoppingCart))
        else -> GeminiInput.Text(NoContext)
    }

    private fun handleGeminiOutput(
        output: GeminiOutput,
        geminiSession: GeminiLiveBlockingSessionGateway,
        stateMachine: StateMachine<SessionState, SessionEvent>,
        storeId: Long,
        ownerInfo: MemberInfo,
        shoppingCart: ShoppingCartDTO,
        onReply: (WebSocketResponse) -> Unit
    ) {
        when (output) {
            is GeminiOutput.InputSTT -> logger.info { "blocking gemini input stt -> ${output.text}" }

            is GeminiOutput.OutputSTT -> {
                logger.info { "blocking gemini output stt -> ${output.text}" }
                onReply(WebSocketTextResponse.fromOutputText(OutputTextChunkDTO(output.text)))
            }

            is GeminiOutput.FunctionCall -> {
                output.signature.toSessionEvent()
                    ?.let { event ->
                        handleSessionEvent(output, event, stateMachine, geminiSession, onReply)
                    }
                    ?: handleGeneralFunctionCall(
                        output,
                        storeId,
                        ownerInfo,
                        shoppingCart,
                        geminiSession,
                        onReply
                    )
            }

            is GeminiOutput.VoiceStream -> onReply(WebSocketBinaryResponse(output.chunk))

            is GeminiOutput.EndOfGeminiTurn -> {
                onReply(WebSocketTextResponse.fromEndOfGeminiTurn(OutputTextResultDTO(output.finalOutputSTT)))
            }
        }
    }

    private fun handleSessionEvent(
        functionCall: GeminiOutput.FunctionCall,
        event: SessionEvent,
        stateMachine: StateMachine<SessionState, SessionEvent>,
        geminiSession: GeminiLiveBlockingSessionGateway,
        onReply: (WebSocketResponse) -> Unit
    ) {
        withSessionLock(stateMachine.id) {
            val stateChange = executeStateTransition(stateMachine, event)

            if (stateChange != null) {
                onReply(WebSocketTextResponse.fromStateChange(stateChange))
            } else {
                logger.error { "상태가 바뀌지 않음 -> oldState: ${stateMachine.state.id}, event: $event" }
            }

            sendOKToGemini(functionCall, geminiSession)
        }
    }

    private fun executeStateTransition(
        stateMachine: StateMachine<SessionState, SessionEvent>,
        event: SessionEvent
    ): StateChangeDTO? {
        val oldState = stateMachine.state.id
        val message = MessageBuilder.withPayload(event).build()

        stateMachine.sendEvent(Mono.just(message)).blockLast()

        val newState = stateMachine.state.id
        return if (oldState != newState) StateChangeDTO(oldState, newState, event) else null
    }

    private fun <T> withSessionLock(sessionId: String, action: () -> T): T {
        val lock = stateMachineLocks.computeIfAbsent(sessionId) { ReentrantLock() }
        return lock.withLock { action() }
    }

    private fun handleGeneralFunctionCall(
        output: GeminiOutput.FunctionCall,
        storeId: Long,
        ownerInfo: MemberInfo,
        shoppingCart: ShoppingCartDTO,
        geminiSession: GeminiLiveBlockingSessionGateway,
        onReply: (WebSocketResponse) -> Unit
    ) {
        var isCartUpdated = false

        when (output.signature) {
            SEARCH_MENU_RAG -> {
                val params = output.params as jnu.ie.capstone.gemini.dto.client.response.GeminiFunctionParams.SearchMenuRAG
                val relevantMenus: List<MenuInternalDTO> = menuService.getMenuRelevant(
                    text = params.searchText,
                    storeId = storeId,
                    ownerInfo = ownerInfo
                )

                geminiSession.send(
                    GeminiInput.ToolResponse(
                        id = output.id,
                        functionName = output.signature.name,
                        result = relevantMenus.joinToString("\n\n" + "-".repeat(20) + "\n\n") { it.toString() }
                    )
                )
            }

            ADD_MENUS_OR_OPTIONS -> {
                isCartUpdated = shoppingCartService.addMenu(
                    storeId = storeId,
                    ownerInfo = ownerInfo,
                    shoppingCart = shoppingCart,
                    addItems = output.params as AddItems
                )
                sendOKToGemini(output, geminiSession)
            }

            REMOVE_MENUS_OR_OPTIONS -> {
                isCartUpdated = shoppingCartService.removeMenu(
                    shoppingCart = shoppingCart,
                    removeItems = output.params as RemoveItems
                )
                sendOKToGemini(output, geminiSession)
            }

            DO_NOTHING -> sendOKToGemini(output, geminiSession)

            else -> sendOKToGemini(output, geminiSession)
        }

        if (isCartUpdated) {
            onReply(WebSocketTextResponse.fromUpdateShoppingCart(shoppingCart.toResponseDTO()))
            geminiSession.send(GeminiInput.Text(MenuSelectionContext(shoppingCart = shoppingCart)))
        }
    }

    private fun sendOKToGemini(
        functionCall: GeminiOutput.FunctionCall,
        geminiSession: GeminiLiveBlockingSessionGateway
    ) {
        geminiSession.send(
            GeminiInput.ToolResponse(
                id = functionCall.id,
                functionName = functionCall.signature.name,
                result = OK
            )
        )
    }
}
