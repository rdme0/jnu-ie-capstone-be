package jnu.ie.capstone.gemini.constant.function

import com.google.genai.types.*
import jnu.ie.capstone.gemini.constant.enums.GeminiFunctionSignature.*
import jnu.ie.capstone.gemini.constant.schema.GeminiSchema
import jnu.ie.capstone.session.enums.SessionState.*

object GeminiFunctionDeclaration {
    val RAG_SEARCH_TOOL: Tool = Tool.builder()
        .functionDeclarations(
            listOf(
                FunctionDeclaration.builder()
                    .description("사용자가 요청한 메뉴 데이터를 얻기 위해 사용하는 RAG search 함수.")
                    .name(SEARCH_MENU_RAG.text)
                    .parameters(GeminiSchema.SEARCH_MENU_RAG_PARAMS_SCHEMA)
                    .response(GeminiSchema.SEARCH_MENU_RAG_RESPONSE_SCHEMA)
                    .behavior(Behavior.Known.BLOCKING)
                    .build()
            )
        ).build()

    val STATEMACHINE_TOOL: Tool = Tool.builder()
        .functionDeclarations(
            listOf(
                FunctionDeclaration.builder()
                    .name(CONFIRM_PAYMENT.text)
                    .description("State $MENU_SELECTION -> $PAYMENT_CONFIRMATION")
                    .response(GeminiSchema.STATE_MACHINE_RESPONSE_SCHEMA)
                    .behavior(Behavior.Known.BLOCKING)
                    .build(),

                FunctionDeclaration.builder()
                    .name(PROCESS_PAYMENT.text)
                    .description("State $PAYMENT_CONFIRMATION -> $COMPLETED")
                    .response(GeminiSchema.STATE_MACHINE_RESPONSE_SCHEMA)
                    .behavior(Behavior.Known.BLOCKING)
                    .build(),

                FunctionDeclaration.builder()
                    .name(CANCEL.text)
                    .description("Any State -> $CANCELLED")
                    .response(GeminiSchema.STATE_MACHINE_RESPONSE_SCHEMA)
                    .behavior(Behavior.Known.BLOCKING)
                    .build(),

                FunctionDeclaration.builder()
                    .name(PREVIOUS.text)
                    .description("State $PAYMENT_CONFIRMATION -> $MENU_SELECTION")
                    .response(GeminiSchema.STATE_MACHINE_RESPONSE_SCHEMA)
                    .behavior(Behavior.Known.BLOCKING)
                    .build(),

                FunctionDeclaration.builder()
                    .name(ADD_MENUS_OR_OPTIONS.text)
                    .description("장바구니에 하나 이상의 메뉴 혹은 메뉴와 옵션 조합을 추가합니다.")
                    .parameters(GeminiSchema.ADD_MENUS_OR_OPTIONS_SCHEMA)
                    .response(GeminiSchema.STATE_MACHINE_RESPONSE_SCHEMA)
                    .behavior(Behavior.Known.BLOCKING)
                    .build(),

                FunctionDeclaration.builder()
                    .name(REMOVE_MENUS_OR_OPTIONS.text)
                    .description("장바구니에서 하나 이상의 메뉴 혹은 하나 이상의 옵션을 제거합니다.")
                    .parameters(GeminiSchema.REMOVE_MENUS_OR_OPTIONS_SCHEMA)
                    .response(GeminiSchema.STATE_MACHINE_RESPONSE_SCHEMA)
                    .behavior(Behavior.Known.BLOCKING)
                    .build()
            )
        )
        .build()
}