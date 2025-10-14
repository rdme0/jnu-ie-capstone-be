package jnu.ie.capstone.menu.controller

import jnu.ie.capstone.common.annotation.ResolvePageable
import jnu.ie.capstone.common.constant.enums.SortField
import jnu.ie.capstone.common.security.dto.KioskUserDetails
import jnu.ie.capstone.menu.dto.request.CreateMenuRequest
import jnu.ie.capstone.menu.dto.request.UpdateMenuRequest
import jnu.ie.capstone.menu.service.MenuCoordinateService
import org.springframework.data.domain.Pageable
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/store/{id}/menus")
class MenuController(
    private val service: MenuCoordinateService
) {

    @PostMapping
    suspend fun createMenu(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: KioskUserDetails,
        @RequestBody request: CreateMenuRequest
    ) = service.save(storeId = id, memberInfo = userDetails.memberInfo, request = request)

    @GetMapping
    fun getMenus(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: KioskUserDetails,
        @ResolvePageable(allowed = [SortField.CREATED_AT]) pageable: Pageable
    ) = service.get(storeId = id, memberInfo = userDetails.memberInfo, pageable)

    @PutMapping
    suspend fun updateMenu(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: KioskUserDetails,
        @RequestBody request: UpdateMenuRequest
    ) = service.update(storeId = id, memberInfo = userDetails.memberInfo, request = request)

}