package jnu.ie.capstone.gemini.constant.function

import com.google.genai.types.*
import jnu.ie.capstone.gemini.constant.enums.GeminiFunctionSignature.*
import jnu.ie.capstone.gemini.constant.schema.GeminiSchema
import jnu.ie.capstone.session.enums.SessionState.*

object GeminiFunctionDeclaration {
    val STATEMACHINE_TOOL: Tool = Tool.builder()
        .functionDeclarations(
            listOf(
                FunctionDeclaration.builder()
                    .name(CONFIRM_CART.text)
                    .description("State $MENU_SELECTION -> $CART_CONFIRMATION")
                    .behavior(Behavior.Known.NON_BLOCKING)
                    .build(),

                FunctionDeclaration.builder()
                    .name(CONFIRM_PAYMENT.text)
                    .description("State $CART_CONFIRMATION -> $PAYMENT_CONFIRMATION")
                    .behavior(Behavior.Known.NON_BLOCKING)
                    .build(),

                FunctionDeclaration.builder()
                    .name(PROCESS_PAYMENT.text)
                    .description("State $PAYMENT_CONFIRMATION -> $COMPLETED")
                    .behavior(Behavior.Known.NON_BLOCKING)
                    .build(),

                FunctionDeclaration.builder()
                    .name(CANCEL.text)
                    .description("Any State -> $CANCELLED")
                    .behavior(Behavior.Known.NON_BLOCKING)
                    .build(),

                FunctionDeclaration.builder()
                    .name(PREVIOUS.text)
                    .description("State (${PAYMENT_CONFIRMATION} OR ${CART_CONFIRMATION}) -> $MENU_SELECTION")
                    .behavior(Behavior.Known.NON_BLOCKING)
                    .build(),

                FunctionDeclaration.builder()
                    .name(ADD_MENUS_OR_OPTIONS.text)
                    .description("장바구니에 하나 이상의 메뉴 혹은 메뉴와 옵션 조합을 추가합니다.")
                    .parameters(GeminiSchema.ADD_MENUS_OR_OPTIONS_SCHEMA)
                    .behavior(Behavior.Known.NON_BLOCKING)
                    .build(),

                FunctionDeclaration.builder()
                    .name(REMOVE_MENUS_OR_OPTIONS.text)
                    .description("장바구니에서 하나 이상의 메뉴 혹은 하나 이상의 옵션을 제거합니다.")
                    .parameters(GeminiSchema.REMOVE_MENUS_OR_OPTIONS_SCHEMA)
                    .behavior(Behavior.Known.NON_BLOCKING)
                    .build()
            )
        )
        .build()
}