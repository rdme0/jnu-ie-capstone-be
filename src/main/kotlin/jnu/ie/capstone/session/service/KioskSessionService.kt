package jnu.ie.capstone.session.service

import jnu.ie.capstone.gemini.client.GeminiLiveClient
import jnu.ie.capstone.gemini.config.PromptConfig
import jnu.ie.capstone.gemini.constant.enums.GeminiFunctionSignature.*
import jnu.ie.capstone.gemini.dto.client.internal.Context.*
import jnu.ie.capstone.gemini.dto.client.request.GeminiInput.*
import jnu.ie.capstone.gemini.dto.client.response.GeminiFunctionParams.AddItems
import jnu.ie.capstone.gemini.dto.client.response.GeminiFunctionParams.RemoveItems
import jnu.ie.capstone.gemini.dto.client.response.GeminiOutput
import jnu.ie.capstone.gemini.dto.client.response.GeminiOutput.*
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.menu.service.MenuCoordinateService
import jnu.ie.capstone.rtzr.service.RtzrSttService
import jnu.ie.capstone.session.dto.internal.OutputTextChunkDTO
import jnu.ie.capstone.session.dto.internal.OutputTextResultDTO
import jnu.ie.capstone.session.dto.internal.ShoppingCartDTO
import jnu.ie.capstone.session.dto.internal.StateChangeDTO
import jnu.ie.capstone.session.enums.SessionEvent
import jnu.ie.capstone.session.enums.SessionState
import jnu.ie.capstone.session.enums.SessionState.*
import jnu.ie.capstone.session.event.EndOfGeminiTurnEvent
import jnu.ie.capstone.session.event.OutputTextEvent
import jnu.ie.capstone.session.event.ShoppingCartUpdatedEvent
import jnu.ie.capstone.session.event.StateChangeEvent
import jnu.ie.capstone.session.service.internal.KioskShoppingCartService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
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
    private val sttService: RtzrSttService,
    private val promptConfig: PromptConfig,
    private val shoppingCartService: KioskShoppingCartService,
    private val eventPublisher: ApplicationEventPublisher
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val scopeForContext = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateMachineLocks = ConcurrentHashMap<String, Mutex>()

    suspend fun processVoiceChunk(
        rtzrReadySignal: CompletableDeferred<Unit>,
        voiceStream: Flow<ByteArray>,
        storeId: Long,
        ownerInfo: MemberInfo,
        stateMachine: StateMachine<SessionState, SessionEvent>,
        session: WebSocketSession,
        onVoiceChunk: suspend (ByteArray) -> Unit
    ) {
        val sharedVoiceStream = voiceStream
            .buffer(capacity = 128, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            .shareIn(scopeForContext, SharingStarted.Lazily)
        val shoppingCart = session.attributes["shoppingCart"] as ShoppingCartDTO
        val nowState = stateMachine.state.id

        val voiceFastInput: Flow<Audio> = sharedVoiceStream.map { Audio(it) }
        val contextSlowInput: Flow<Text> = handleContextByState(
            rtzrReadySignal,
            nowState,
            sharedVoiceStream,
            storeId,
            ownerInfo,
            shoppingCart
        ).onEach { logger.info { "gemini slow input -> $it" } }

        val mergedInput = merge(voiceFastInput, contextSlowInput)
            .onEach { logger.debug { "gemini merged input -> $it" } }

        liveClient
            .getLiveResponse(mergedInput, promptConfig.kiosk)
            .collect { output ->
                handleGeminiOutput(
                    output,
                    session,
                    stateMachine,
                    storeId,
                    ownerInfo,
                    shoppingCart,
                    onVoiceChunk
                )
            }
    }

    fun cleanupSession(sessionId: String) {
        stateMachineLocks.remove(sessionId)
        logger.info { "Cleaned up lock for session: $sessionId" }
    }

    private suspend fun handleContextByState(
        rtzrReadySignal: CompletableDeferred<Unit>,
        nowState: SessionState,
        sharedVoiceStream: SharedFlow<ByteArray>,
        storeId: Long,
        ownerInfo: MemberInfo,
        shoppingCart: ShoppingCartDTO
    ): Flow<Text> = when (nowState) {
        MENU_SELECTION -> {
            sttService
                .stt(sharedVoiceStream, scopeForContext, rtzrReadySignal)
                .onEach { logger.debug { "rtzr stt -> ${it.alternatives.first().text}" } }
                .filter { it.final }
                .map { it.alternatives.first().text }
                .filter { it.isNotBlank() }
                .map { menuService.getMenuRelevant(text = it, storeId, ownerInfo) }
                .map { Text(MenuSelectionContext(menus = it, shoppingCart = shoppingCart)) }
        }

        CART_CONFIRMATION -> flowOf(Text(CartConfirmationContext(shoppingCart = shoppingCart)))

        PAYMENT_CONFIRMATION -> flowOf(Text(PaymentConfirmationContext(shoppingCart = shoppingCart)))

        else -> flowOf(Text(NoContext))
    }

    private suspend fun handleGeminiOutput(
        output: GeminiOutput,
        session: WebSocketSession,
        stateMachine: StateMachine<SessionState, SessionEvent>,
        storeId: Long,
        ownerInfo: MemberInfo,
        shoppingCart: ShoppingCartDTO,
        onVoiceChunk: suspend (ByteArray) -> Unit
    ) {
        when (output) {
            is InputSTT -> {
                logger.info { "gemini input stt -> ${output.text}" }
            }

            is OutputSTT -> {
                logger.info { "gemini output stt -> ${output.text}" }

                val content = OutputTextChunkDTO(output.text)
                eventPublisher.publishEvent(OutputTextEvent(this, session.id, content))
            }

            is FunctionCall -> {
                logger.info { "now state -> ${stateMachine.state}" }
                logger.info { "gemini output function -> ${output.signature}" }
                logger.info { "gemini output params -> ${output.params}" }

                output.signature.toSessionEvent()
                    ?.let { handleSessionEvent(it, stateMachine) }
                    ?: run {
                        handleFunctionCall(output, storeId, ownerInfo, shoppingCart, session.id)
                    }
            }

            is VoiceStream -> {
                onVoiceChunk(output.chunk)
            }

            is EndOfGeminiTurn -> {
                val content = OutputTextResultDTO(output.finalOutputSTT)
                eventPublisher.publishEvent(EndOfGeminiTurnEvent(this, session.id, content))
            }
        }
    }

    private suspend fun handleSessionEvent(
        event: SessionEvent,
        stateMachine: StateMachine<SessionState, SessionEvent>
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

                val stateChangedEvent = StateChangeEvent(
                    this@KioskSessionService,
                    sessionId,
                    content
                )

                eventPublisher.publishEvent(stateChangedEvent)

            } catch (e: Exception) {
                logger.error(e) { "State machine error processing event $event: ${e.message}" }
            }
        }
    }

    private fun handleFunctionCall(
        output: FunctionCall,
        storeId: Long,
        ownerInfo: MemberInfo,
        shoppingCart: ShoppingCartDTO,
        sessionId: String
    ) {
        logger.info { "쇼핑카트 before -> $shoppingCart" }
        var isCartUpdated = false

        when (output.signature) {
            ADD_MENUS_OR_OPTIONS -> {
                val params = output.params as AddItems

                isCartUpdated = shoppingCartService.addMenu(
                    storeId = storeId,
                    ownerInfo = ownerInfo,
                    shoppingCart = shoppingCart,
                    addItems = params
                )
            }

            REMOVE_MENUS_OR_OPTIONS -> {
                val params = output.params as RemoveItems

                isCartUpdated = shoppingCartService.removeMenu(
                    shoppingCart = shoppingCart,
                    removeItems = params
                )
            }

            DO_NOTHING -> {}

            else -> {}
        }

        if (isCartUpdated) {
            val event = ShoppingCartUpdatedEvent(
                source = this,
                sessionId = sessionId,
                content = shoppingCart.toResponseDTO()
            )
            eventPublisher.publishEvent(event)
        }

        logger.info { "쇼핑카트 after -> $shoppingCart" }
    }

}