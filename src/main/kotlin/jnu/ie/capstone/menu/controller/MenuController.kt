package jnu.ie.capstone.menu.controller

import jnu.ie.capstone.common.annotation.ResolvePageable
import jnu.ie.capstone.common.constant.enums.SortField
import jnu.ie.capstone.common.dto.response.CommonResponse
import jnu.ie.capstone.common.security.dto.KioskUserDetails
import jnu.ie.capstone.menu.dto.request.CreateMenuRequest
import jnu.ie.capstone.menu.dto.request.CreateOptionRequest
import jnu.ie.capstone.menu.dto.request.UpdateMenuRequest
import jnu.ie.capstone.menu.dto.request.UpdateOptionRequest
import jnu.ie.capstone.menu.dto.response.MenuResponse
import jnu.ie.capstone.menu.dto.response.OptionResponse
import jnu.ie.capstone.menu.service.MenuCoordinateService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/stores/{storeId}/menus")
class MenuController(
    private val service: MenuCoordinateService
) {

    @PostMapping
    fun createMenu(
        @PathVariable storeId: Long,
        @AuthenticationPrincipal userDetails: KioskUserDetails,
        @RequestBody request: CreateMenuRequest
    ): ResponseEntity<CommonResponse> {
        service.createMenu(storeId = storeId, ownerInfo = userDetails.memberInfo, request = request)

        return ResponseEntity
            .created(URI.create("/stores/$storeId/menus"))
            .body(CommonResponse.ofSuccess())
    }

    @PostMapping("{menuId}/options")
    fun createOption(
        @PathVariable storeId: Long,
        @PathVariable menuId: Long,
        @AuthenticationPrincipal userDetails: KioskUserDetails,
        @RequestBody request: CreateOptionRequest
    ): ResponseEntity<CommonResponse> {

        service.createOption(
            storeId = storeId,
            menuId = menuId,
            ownerInfo = userDetails.memberInfo,
            request = request
        )

        return ResponseEntity
            .created(URI.create("/stores/$storeId/menus/$menuId/options"))
            .body(CommonResponse.ofSuccess())
    }

    @GetMapping
    fun getMenus(
        @PathVariable storeId: Long,
        @AuthenticationPrincipal userDetails: KioskUserDetails,
        @ResolvePageable(allowed = [SortField.CREATED_AT]) pageable: Pageable
    ): Page<MenuResponse> {
        return service.getMenuResponses(
            storeId = storeId,
            ownerInfo = userDetails.memberInfo,
            pageable = pageable
        )
    }

    @GetMapping("/{menuId}/options")
    fun getOptions(
        @PathVariable storeId: Long,
        @PathVariable menuId: Long,
        @AuthenticationPrincipal userDetails: KioskUserDetails,
        @ResolvePageable(allowed = [SortField.CREATED_AT]) pageable: Pageable
    ): Page<OptionResponse> {
        return service.getOptionResponses(
            storeId = storeId,
            menuId = menuId,
            ownerInfo = userDetails.memberInfo,
            pageable = pageable
        )
    }

    @PutMapping("/{menuId}")
    fun updateMenu(
        @PathVariable storeId: Long,
        @PathVariable menuId: Long,
        @AuthenticationPrincipal userDetails: KioskUserDetails,
        @RequestBody request: UpdateMenuRequest
    ): ResponseEntity<Unit> {
        service.updateMenu(
            storeId = storeId,
            menuId = menuId,
            ownerInfo = userDetails.memberInfo,
            request = request
        )

        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{menuId}/options/{optionId}")
    fun updateOption(
        @PathVariable storeId: Long,
        @PathVariable menuId: Long,
        @PathVariable optionId: Long,
        @AuthenticationPrincipal userDetails: KioskUserDetails,
        @RequestBody request: UpdateOptionRequest
    ): ResponseEntity<Unit> {
        service.updateOption(
            storeId = storeId,
            menuId = menuId,
            optionId = optionId,
            ownerInfo = userDetails.memberInfo,
            request = request
        )

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{menuId}/options/{optionId}")
    fun deleteOption(
        @PathVariable storeId: Long,
        @PathVariable menuId: Long,
        @PathVariable optionId: Long,
        @AuthenticationPrincipal userDetails: KioskUserDetails
    ): ResponseEntity<Unit> {
        service.deleteOption(
            storeId = storeId,
            optionId = optionId,
            menuId = menuId,
            ownerInfo = userDetails.memberInfo,
        )

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{menuId}")
    fun deleteMenu(
        @PathVariable storeId: Long,
        @PathVariable menuId: Long,
        @AuthenticationPrincipal userDetails: KioskUserDetails
    ): ResponseEntity<Unit> {
        service.deleteMenu(
            storeId = storeId,
            menuId = menuId,
            ownerInfo = userDetails.memberInfo
        )

        return ResponseEntity.noContent().build()
    }
}