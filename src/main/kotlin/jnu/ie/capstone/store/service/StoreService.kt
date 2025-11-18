package jnu.ie.capstone.store.service

import jnu.ie.capstone.common.exception.server.InternalServerException
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.member.model.entity.Member
import jnu.ie.capstone.member.service.MemberCoordinateService
import jnu.ie.capstone.menu.service.MenuCoordinateService
import jnu.ie.capstone.store.dto.request.CreateStoreRequest
import jnu.ie.capstone.store.dto.request.UpdateStoreRequest
import jnu.ie.capstone.store.dto.response.StoreResponse
import jnu.ie.capstone.store.exception.NoSuchStoreException
import jnu.ie.capstone.store.model.entity.Store
import jnu.ie.capstone.store.repository.StoreRepository
import jnu.ie.capstone.store.service.internal.StoreDataService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StoreService(
    private val dataService: StoreDataService,
    private val memberService: MemberCoordinateService,
    private val menuService: MenuCoordinateService
) {

    @Transactional
    fun create(ownerInfo: MemberInfo, request: CreateStoreRequest): Long {
        val owner: Member = memberService.getEntity(ownerInfo.id)
            ?: throw InternalServerException(
                IllegalStateException("id가 ${ownerInfo.id}인 회원이 존재하지 않음")
            )

        val store = Store.builder()
            .owner(owner)
            .name(request.name)
            .build()

        return dataService.save(store).id
            ?: throw InternalServerException(
                IllegalStateException("Store 저장에 실패했습니다: ID가 null입니다.")
            )
    }

    @Transactional(readOnly = true)
    fun getOwnerStore(ownerInfo: MemberInfo, pageable: Pageable): Page<StoreResponse> {
        return dataService
            .getBy(ownerInfo.id, pageable)
            .map(StoreResponse::from)
    }


    @Transactional(readOnly = true)
    fun getBy(id: Long, ownerId: Long): Store? {
        return dataService.getBy(id, ownerId)
    }

    @Transactional
    fun update(id: Long, ownerInfo: MemberInfo, request: UpdateStoreRequest) {
        val store: Store = getBy(id = id, ownerId = ownerInfo.id) ?: throw NoSuchStoreException()
        store.updateName(request.name)
    }

    @Transactional
    fun delete(id: Long, ownerInfo: MemberInfo) {
        val store: Store = getBy(id = id, ownerId = ownerInfo.id) ?: throw NoSuchStoreException()
        menuService.deleteAllMenuBy(store.id, ownerInfo)
        dataService.delete(store)
    }

}