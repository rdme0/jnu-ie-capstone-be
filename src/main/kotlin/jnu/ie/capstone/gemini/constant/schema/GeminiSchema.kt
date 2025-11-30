package jnu.ie.capstone.gemini.constant.schema

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.genai.types.Schema
import com.google.genai.types.Type

object GeminiSchema {
    val SEARCH_MENU_RAG_PARAMS_SCHEMA: Schema = Schema.builder()
        .type(Type.Known.OBJECT)
        .properties(
            ImmutableMap.of(
                "searchText",
                Schema.builder().type(Type.Known.STRING).build()
            )
        )
        .required(ImmutableList.of("searchText"))
        .build()

    val SEARCH_MENU_RAG_RESPONSE_SCHEMA: Schema = Schema.builder()
        .type(Type.Known.OBJECT)
        .properties(
            ImmutableMap.of(
                "result",
                Schema.builder().type(Type.Known.STRING).build()
            )
        )
        .description("MENU를 RAG SEARCH 한 결과물이다. 앞에 있는 메뉴일 수록 유사도가 큰 메뉴이다.")
        .build()

    val STATE_MACHINE_RESPONSE_SCHEMA: Schema = Schema.builder()
        .type(Type.Known.OBJECT)
        .properties(
            ImmutableMap.of(
                "result",
                Schema.builder().type(Type.Known.STRING).build()
            )
        )
        .description("상태 변경이나 장바구니 수정 함수에 대한 응답이다. (value는 ok로 고정이다)")
        .build()

    val ADD_MENUS_OR_OPTIONS_SCHEMA: Schema = Schema.builder()
        .type(Type.Known.OBJECT)
        .properties(
            ImmutableMap.of(
                "orderItems",
                Schema.builder()
                    .description("장바구니에 추가하고자 하는 메뉴들과 옵션들 입니다.")
                    .type(Type.Known.ARRAY)
                    .items(
                        Schema.builder()
                            .type(Type.Known.OBJECT)
                            .properties(
                                ImmutableMap.of(
                                    "menuId",
                                    Schema.builder()
                                        .description("장바구니에 추가하고자 하는 메뉴 id 입니다.")
                                        .type(Type.Known.INTEGER)
                                        .build(),
                                    "optionIds",
                                    Schema.builder()
                                        .description("장바구니에 추가하고자 하는 메뉴 하나에 해당하는 옵션 id들 입니다.")
                                        .type(Type.Known.ARRAY)
                                        .items(
                                            Schema.builder()
                                                .description("장바구니에 추가하고자 하는 옵션 id 입니다.")
                                                .type(Type.Known.INTEGER)
                                        ).build()
                                )
                            )
                            .build()
                    ).build()
            )
        )
        .required(ImmutableList.of("orderItems"))
        .build()


    val REMOVE_MENUS_OR_OPTIONS_SCHEMA: Schema = Schema.builder()
        .type(Type.Known.OBJECT)
        .properties(
            ImmutableMap.of(
                "removeMenuIds",
                Schema.builder()
                    .description("장바구니에서 제거하고 싶은 메뉴 id들 입니다. 장바구니의 메뉴를 제거 할 때 옵션들은 cascade 됩니다.")
                    .type(Type.Known.ARRAY)
                    .items(
                        Schema.builder().type(Type.Known.INTEGER).build()
                    ).build(),

                "removeOptionIds",
                Schema.builder()
                    .description("장바구니에서 특정 옵션만을 제거하고 하고 싶을 때 이 옵션 id들을 지정합니다.")
                    .type(Type.Known.ARRAY)
                    .items(
                        Schema.builder().type(Type.Known.INTEGER).build()
                    ).build()
            )
        )
        .build()
}