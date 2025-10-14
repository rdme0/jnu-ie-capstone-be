package jnu.ie.capstone.gemini.constant.function

import com.google.genai.types.Behavior
import com.google.genai.types.FunctionDeclaration
import com.google.genai.types.Tool
import jnu.ie.capstone.session.enums.SessionState.*
import jnu.ie.capstone.session.enums.SessionEvent.*

object GeminiFunction {
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
                    .build()
            )
        )
        .build()

}