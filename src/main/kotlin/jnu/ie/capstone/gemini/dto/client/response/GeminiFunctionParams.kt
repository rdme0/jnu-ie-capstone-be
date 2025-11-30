package jnu.ie.capstone.gemini.dto.client.response

sealed class GeminiFunctionParams {
    object NoParams: GeminiFunctionParams()

    data class SearchMenuRAG(val searchText: String) : GeminiFunctionParams()

    data class AddItems(
        val orderItems: List<OrderItemDTO>
    ) : GeminiFunctionParams()

    data class RemoveItems(
        val removeMenuIds: List<Long>?,
        val removeOptionIds: List<Long>?
    ) : GeminiFunctionParams()
}

data class OrderItemDTO(
    val menuId: Long?,
    val optionIds: List<Long>?
)