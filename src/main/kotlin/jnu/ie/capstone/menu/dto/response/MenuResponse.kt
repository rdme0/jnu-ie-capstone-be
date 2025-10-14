package jnu.ie.capstone.menu.dto.response

data class MenuResponse(
    val id: Long,
    val name: String,
    val price: Long,
    val options: List<OptionResponse>? = null
)