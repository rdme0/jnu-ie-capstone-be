package jnu.ie.capstone.menu.service.internal

import jnu.ie.capstone.menu.model.entity.Menu
import jnu.ie.capstone.menu.repository.MenuRepository
import jnu.ie.capstone.store.model.entity.Store
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MenuDataService(
    private val repository: MenuRepository
) {

    @Transactional
    fun save(menus: List<Menu>) {
        repository.saveAll(menus)
    }

    @Transactional(readOnly = true)
    fun getPageBy(store: Store, ownerId: Long, pageable: Pageable): Page<Menu> {
        return repository.findByStoreIdAndStoreOwnerId(store.id, ownerId, pageable)
    }

    @Transactional(readOnly = true)
    fun getListBy(store: Store, ownerId: Long): List<Menu> {
        return repository.findByStoreIdAndStoreOwnerId(store.id, ownerId)
    }

    @Transactional(readOnly = true)
    fun getBy(store: Store, ownerId: Long, id: Long): Menu? {
        return repository.findByStoreIdAndStoreOwnerIdAndId(store.id, ownerId, id)
    }

    @Transactional
    fun delete(ids: List<Long>) {
        repository.deleteByIdIn(ids)
    }

    @Transactional
    fun overwrite(ids: List<Long>, menus: List<Menu>) {
        repository.deleteByIdIn(ids)
        repository.saveAll(menus)
    }

}