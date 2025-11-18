package jnu.ie.capstone.store.dto.request

import jnu.ie.capstone.store.model.vo.StoreName

data class UpdateStoreRequest(
    val name: StoreName
)