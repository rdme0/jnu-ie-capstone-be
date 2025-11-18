package jnu.ie.capstone.store.controller

import jnu.ie.capstone.common.annotation.ResolvePageable
import jnu.ie.capstone.common.constant.enums.SortField
import jnu.ie.capstone.common.dto.response.CommonResponse
import jnu.ie.capstone.common.security.dto.KioskUserDetails
import jnu.ie.capstone.store.dto.request.CreateStoreRequest
import jnu.ie.capstone.store.dto.request.UpdateStoreRequest
import jnu.ie.capstone.store.dto.response.StoreResponse
import jnu.ie.capstone.store.service.StoreService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/stores")
class StoreController(
    private val service: StoreService
) {

    @PostMapping
    fun createStore(
        @AuthenticationPrincipal userDetails: KioskUserDetails,
        @RequestBody request: CreateStoreRequest
    ): ResponseEntity<CommonResponse> {
        val id = service.create(ownerInfo = userDetails.memberInfo, request = request)

        return ResponseEntity
            .created(URI.create("/stores/$id"))
            .body(CommonResponse.ofSuccess())
    }

    @GetMapping
    fun getStores(
        @AuthenticationPrincipal userDetails: KioskUserDetails,
        @ResolvePageable(allowed = [SortField.CREATED_AT]) pageable: Pageable
    ): Page<StoreResponse> {
        return service.getOwnerStore(ownerInfo = userDetails.memberInfo, pageable = pageable)
    }

    @PatchMapping("/{storeId}")
    fun updateStore(
        @PathVariable storeId: Long,
        @AuthenticationPrincipal userDetails: KioskUserDetails,
        @RequestBody updateStoreRequest: UpdateStoreRequest
    ): ResponseEntity<Unit> {
        service.update(
            id = storeId,
            ownerInfo = userDetails.memberInfo,
            request = updateStoreRequest
        )

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{storeId}")
    fun deleteStore(
        @PathVariable storeId: Long,
        @AuthenticationPrincipal userDetails: KioskUserDetails
    ): ResponseEntity<Unit> {
        service.delete(id = storeId, ownerInfo = userDetails.memberInfo)

        return ResponseEntity.noContent().build()
    }

}