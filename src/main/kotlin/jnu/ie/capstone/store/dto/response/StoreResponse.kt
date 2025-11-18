package jnu.ie.capstone.store.dto.response

import jnu.ie.capstone.store.model.entity.Store

data class StoreResponse(
    val id: Long,
    val name: String
) {
    companion object {
        fun from(store: Store): StoreResponse {
            return StoreResponse(id = store.id, name = store.name.value)
        }
    }
}