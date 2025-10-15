package jnu.ie.capstone.menu.service.internal

import io.lettuce.core.ClientListArgs.Builder.ids
import jnu.ie.capstone.menu.model.entity.Menu
import jnu.ie.capstone.menu.repository.MenuRepository
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
    fun getPageBy(storeId: Long, pageable: Pageable): Page<Menu> {
        return repository.findByStoreId(storeId, pageable)
    }

    @Transactional(readOnly = true)
    fun getListBy(storeId: Long): List<Menu> {
        return repository.findByStoreId(storeId)
    }

    @Transactional(readOnly = true)
    fun getRelevantBy(storeId: Long, embedding: FloatArray, limit: Int): List<Menu> {
        return repository.findRelevantMenus(storeId, embedding, limit)
    }

    @Transactional(readOnly = true)
    fun getBy(storeId: Long, id: Long): Menu? {
        return repository.findByStoreIdAndId(storeId, id)
    }

    @Transactional
    fun deleteBy(id: Long) {
        repository.deleteById(id)
    }

}