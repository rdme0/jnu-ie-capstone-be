package jnu.ie.capstone.session.service

import jnu.ie.capstone.gemini.client.GeminiLiveClient
import jnu.ie.capstone.gemini.config.PromptConfig
import jnu.ie.capstone.gemini.constant.enums.GeminiFunctionSignature.*
import jnu.ie.capstone.gemini.dto.client.internal.Context
import jnu.ie.capstone.gemini.dto.client.request.GeminiInput
import jnu.ie.capstone.gemini.dto.client.response.GeminiFunctionParams.AddItems
import jnu.ie.capstone.gemini.dto.client.response.GeminiFunctionParams.RemoveItems
import jnu.ie.capstone.gemini.dto.client.response.GeminiOutput.*
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.menu.service.MenuCoordinateService
import jnu.ie.capstone.rtzr.service.RtzrSttService
import jnu.ie.capstone.session.dto.internal.OutputTextChunkDTO
import jnu.ie.capstone.session.dto.internal.OutputTextResultDTO
import jnu.ie.capstone.session.dto.internal.ShoppingCartDTO
import jnu.ie.capstone.session.enums.SessionEvent
import jnu.ie.capstone.session.enums.SessionState
import jnu.ie.capstone.session.event.EndOfGeminiTurnEvent
import jnu.ie.capstone.session.event.OutputTextEvent
import jnu.ie.capstone.session.event.ShoppingCartUpdatedEvent
import jnu.ie.capstone.session.service.internal.KioskShoppingCartService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.messaging.support.MessageBuilder
import org.springframework.statemachine.StateMachine
import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession
import reactor.core.publisher.Mono

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

    suspend fun processVoiceChunk(
        voiceStream: Flow<ByteArray>,
        storeId: Long,
        ownerInfo: MemberInfo,
        stateMachine: StateMachine<SessionState, SessionEvent>,
        session: WebSocketSession,
        onVoiceChunk: suspend (ByteArray) -> Unit
    ) {
        val sharedVoiceStream = voiceStream.shareIn(scopeForContext, SharingStarted.Eagerly)

        val shoppingCart = session.attributes["shoppingCart"] as ShoppingCartDTO

        val voiceFastInput: Flow<GeminiInput.Audio> = sharedVoiceStream
            .map { GeminiInput.Audio(it) }

        val contextSlowInput: Flow<GeminiInput.Text> = sttService
            .stt(sharedVoiceStream, scopeForContext)
            .filter { it.final }
            .map { it.alternatives.first().text }
            .map { menuService.getMenuRelevant(text = it, storeId, ownerInfo) }
            .map {
                GeminiInput.Text(
                    Context.MenuAndShoppingCart(menus = it, shoppingCart = shoppingCart)
                )
            }
            .onEach { logger.info { "gemini slow input -> $it" } }

        val mergedInput = merge(voiceFastInput, contextSlowInput)
            .onEach { logger.debug { "gemini merged input -> $it" } }

        liveClient.getLiveResponse(mergedInput, promptConfig.kiosk).collect { output ->
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
                    logger.info { "now state -> ${stateMachine.id}" }
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
    }

    private fun handleSessionEvent(
        event: SessionEvent,
        stateMachine: StateMachine<SessionState, SessionEvent>
    ) {
        logger.info { "now state -> ${stateMachine.id}" }
        val message = MessageBuilder.withPayload(event).build()
        stateMachine.sendEvent(Mono.just(message)).subscribe()
        logger.info { "after state -> ${stateMachine.id}" }
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