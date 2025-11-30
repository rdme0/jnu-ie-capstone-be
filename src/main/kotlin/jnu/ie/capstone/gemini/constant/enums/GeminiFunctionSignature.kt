package jnu.ie.capstone.gemini.constant.enums

import jnu.ie.capstone.gemini.dto.client.response.GeminiFunctionParams
import jnu.ie.capstone.session.enums.SessionEvent
import kotlin.reflect.KClass

enum class GeminiFunctionSignature(
    val paramsType: KClass<out GeminiFunctionParams>
) {
    SEARCH_MENU_RAG(GeminiFunctionParams.SearchMenuRAG::class),

    ADD_MENUS_OR_OPTIONS(GeminiFunctionParams.AddItems::class),
    REMOVE_MENUS_OR_OPTIONS(GeminiFunctionParams.RemoveItems::class),
    DO_NOTHING(GeminiFunctionParams.NoParams::class),

    CONFIRM_CART(GeminiFunctionParams.NoParams::class),
    CONFIRM_PAYMENT(GeminiFunctionParams.NoParams::class),
    PROCESS_PAYMENT(GeminiFunctionParams.NoParams::class),
    PREVIOUS(GeminiFunctionParams.NoParams::class),
    CANCEL(GeminiFunctionParams.NoParams::class);

    val text: String
        get() = this.name

    companion object {
        fun fromText(text: String): GeminiFunctionSignature? {
            return entries.find { it.text == text }
        }
    }

    fun toSessionEvent(): SessionEvent? {
        return SessionEvent.fromText(this.name)
    }
}