package jnu.ie.capstone.session.service

import jnu.ie.capstone.gemini.client.GeminiLiveClient
import jnu.ie.capstone.gemini.config.PromptConfig
import jnu.ie.capstone.gemini.constant.enums.GeminiFunctionSignature.*
import jnu.ie.capstone.gemini.dto.client.internal.Context.*
import jnu.ie.capstone.gemini.dto.client.request.GeminiInput
import jnu.ie.capstone.gemini.dto.client.request.GeminiInput.Audio
import jnu.ie.capstone.gemini.dto.client.request.GeminiInput.Text
import jnu.ie.capstone.gemini.dto.client.response.GeminiFunctionParams.*
import jnu.ie.capstone.gemini.dto.client.response.GeminiOutput
import jnu.ie.capstone.gemini.dto.client.response.GeminiOutput.*
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
import jnu.ie.capstone.session.enums.SessionState.*
import jnu.ie.capstone.session.service.internal.KioskShoppingCartService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.springframework.messaging.support.MessageBuilder
import org.springframework.statemachine.StateMachine
import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

@Service
class KioskSessionService(
    private val liveClient: GeminiLiveClient,
    private val menuService: MenuCoordinateService,
    private val promptConfig: PromptConfig,
    private val shoppingCartService: KioskShoppingCartService
) {
    private companion object {
        const val SHOPPING_CART_KEY = "shoppingCart"
        const val OK = "ok"

        val logger = KotlinLogging.logger {}
        val stateMachineLocks = ConcurrentHashMap<String, Mutex>()
    }

    suspend fun processVoiceChunk(
        geminiReadySignal: CompletableDeferred<Unit>,
        voiceStream: Flow<ByteArray>,
        storeId: Long,
        ownerInfo: MemberInfo,
        stateMachine: StateMachine<SessionState, SessionEvent>,
        session: WebSocketSession,
        onReply: suspend (WebSocketResponse) -> Unit
    ) = supervisorScope {
        val currentScope: CoroutineScope = this

        val sharedVoiceStream = voiceStream
            .buffer(capacity = 128, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            .shareIn(scope = currentScope, started = SharingStarted.Lazily)

        val shoppingCart = session.attributes[SHOPPING_CART_KEY] as ShoppingCartDTO

        val nowState = stateMachine.state.id

        val voiceFastInput: Flow<Audio> = sharedVoiceStream.map { Audio(it) }

        val contextSlowInput: Flow<Text> = handleContextByState(nowState, shoppingCart)
            .onEach { logger.info { "gemini slow input -> $it" } }

        val toolResponseChannel = Channel<GeminiInput.ToolResponse>(Channel.BUFFERED)

        val toolResponseInput = toolResponseChannel.receiveAsFlow()

        val mergedInput = merge(voiceFastInput, contextSlowInput, toolResponseInput)
            .onEach { logger.debug { "gemini merged input -> $it" } }

        liveClient
            .getLiveResponse(geminiReadySignal, mergedInput, promptConfig.kiosk)
            .collect { output ->
                handleGeminiOutput(
                    output,
                    stateMachine,
                    storeId,
                    ownerInfo,
                    shoppingCart,
                    toolResponseChannel
                ) { message -> onReply(message) }
            }
    }

    fun cleanupSession(sessionId: String) {
        stateMachineLocks.remove(sessionId)
        logger.info { "Cleaned up lock for session: $sessionId" }
    }

    private suspend fun handleContextByState(
        nowState: SessionState,
        shoppingCart: ShoppingCartDTO
    ): Flow<Text> = when (nowState) {
        MENU_SELECTION -> flowOf(Text(MenuSelectionContext(shoppingCart = shoppingCart)))

        CART_CONFIRMATION -> flowOf(Text(CartConfirmationContext(shoppingCart = shoppingCart)))

        PAYMENT_CONFIRMATION -> flowOf(Text(PaymentConfirmationContext(shoppingCart = shoppingCart)))

        else -> flowOf(Text(NoContext))
    }

    private suspend fun handleGeminiOutput(
        output: GeminiOutput,
        stateMachine: StateMachine<SessionState, SessionEvent>,
        storeId: Long,
        ownerInfo: MemberInfo,
        shoppingCart: ShoppingCartDTO,
        toolResponseChannel: Channel<GeminiInput.ToolResponse>,
        onReply: suspend (WebSocketResponse) -> Unit
    ) {
        when (output) {
            is InputSTT -> {
                logger.info { "gemini input stt -> ${output.text}" }
            }

            is OutputSTT -> {
                logger.info { "gemini output stt -> ${output.text}" }

                val content = OutputTextChunkDTO(output.text)

                onReply(WebSocketTextResponse.fromOutputText(content))
            }

            is FunctionCall -> {
                logger.info { "now state -> ${stateMachine.state}" }
                logger.info { "gemini output function -> ${output.signature}" }
                logger.info { "gemini output params -> ${output.params}" }

                output.signature.toSessionEvent()
                    ?.let { sessionEvent ->
                        handleSessionEvent(
                            functionCall = output,
                            event = sessionEvent,
                            stateMachine = stateMachine,
                            toolResponseChannel = toolResponseChannel,
                        ) { textResponse -> onReply(textResponse) }
                    }
                    ?: run {
                        handleGeneralFunctionCall(
                            output,
                            storeId,
                            ownerInfo,
                            shoppingCart,
                            toolResponseChannel,
                        ) { textResponse -> onReply(textResponse) }
                    }
            }

            is VoiceStream -> {
                onReply(WebSocketBinaryResponse(output.chunk))
            }

            is EndOfGeminiTurn -> {
                val content = OutputTextResultDTO(output.finalOutputSTT)
                onReply(WebSocketTextResponse.fromEndOfGeminiTurn(content))
            }
        }
    }

    private suspend fun handleSessionEvent(
        functionCall: FunctionCall,
        event: SessionEvent,
        stateMachine: StateMachine<SessionState, SessionEvent>,
        toolResponseChannel: Channel<GeminiInput.ToolResponse>,
        onReply: suspend (WebSocketTextResponse) -> Unit
    ) {
        val sessionId = stateMachine.id
        val mutex = stateMachineLocks.computeIfAbsent(sessionId) { Mutex() }

        mutex.withLock {
            // mutex 사용 이유 : oldState와 newState 간의 동시성 문제가 발생 가능
            val oldState = stateMachine.state.id
            logger.info { "old state -> $oldState" }

            val message = MessageBuilder.withPayload(event).build()

            try {
                stateMachine.sendEvent(Mono.just(message)).awaitFirstOrNull()

                val newState = stateMachine.state.id
                logger.info { "new state -> $newState" }

                if (newState == oldState) {
                    logger.error { "상태가 바뀌지 않음 -> oldState: $oldState, event: $event" }
                    return@withLock
                }

                val content = StateChangeDTO(from = oldState, to = newState, because = event)

                onReply(WebSocketTextResponse.fromStateChange(content))
            } catch (e: Exception) {
                logger.error(e) { "State machine error processing event $event: ${e.message}" }
            } finally {
                sendOKToGemini(functionCall, toolResponseChannel)
            }
        }
    }

    private suspend fun handleGeneralFunctionCall(
        output: FunctionCall,
        storeId: Long,
        ownerInfo: MemberInfo,
        shoppingCart: ShoppingCartDTO,
        toolResponseChannel: Channel<GeminiInput.ToolResponse>,
        onReply: suspend (WebSocketTextResponse) -> Unit
    ) {
        logger.info { "쇼핑카트 before -> $shoppingCart" }
        var isCartUpdated = false

        when (output.signature) {
            SEARCH_MENU_RAG -> {
                val params = output.params as SearchMenuRAG

                val relevantMenus: List<MenuInternalDTO> = menuService.getMenuRelevant(
                    text = params.searchText,
                    storeId = storeId,
                    ownerInfo = ownerInfo
                )

                toolResponseChannel.send(
                    GeminiInput.ToolResponse(
                        id = output.id,
                        functionName = output.signature.name,
                        result = relevantMenus.joinToString("\n\n" + "-".repeat(20) + "\n\n") { it.toString() }
                    )
                )
            }

            ADD_MENUS_OR_OPTIONS -> {
                val params = output.params as AddItems

                isCartUpdated = shoppingCartService.addMenu(
                    storeId = storeId,
                    ownerInfo = ownerInfo,
                    shoppingCart = shoppingCart,
                    addItems = params
                )

                sendOKToGemini(output, toolResponseChannel)
            }

            REMOVE_MENUS_OR_OPTIONS -> {
                val params = output.params as RemoveItems

                isCartUpdated = shoppingCartService.removeMenu(
                    shoppingCart = shoppingCart,
                    removeItems = params
                )

                sendOKToGemini(output, toolResponseChannel)
            }

            DO_NOTHING -> {
                sendOKToGemini(output, toolResponseChannel)
            }

            else -> {
                sendOKToGemini(output, toolResponseChannel)
            }
        }

        if (isCartUpdated) {
            onReply(WebSocketTextResponse.fromUpdateShoppingCart(shoppingCart.toResponseDTO()))
        }

        logger.info { "쇼핑카트 after -> $shoppingCart" }
    }

    private suspend fun sendOKToGemini(
        functionCall: FunctionCall,
        toolResponseChannel: Channel<GeminiInput.ToolResponse>
    ) {
        toolResponseChannel.send(
            GeminiInput.ToolResponse(
                id = functionCall.id,
                functionName = functionCall.signature.name,
                result = OK
            )
        )
    }
}