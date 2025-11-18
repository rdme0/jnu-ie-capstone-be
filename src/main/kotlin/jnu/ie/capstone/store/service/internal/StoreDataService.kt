package jnu.ie.capstone.store.service.internal

import jnu.ie.capstone.store.model.entity.Store
import jnu.ie.capstone.store.repository.StoreRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StoreDataService(
    private val repository: StoreRepository
) {

    @Transactional
    fun save(store: Store): Store {
        return repository.save(store)
    }

    @Transactional(readOnly = true)
    fun getBy(ownerId: Long, pageable: Pageable): Page<Store> {
        return repository.findByOwnerId(ownerId, pageable)
    }

    @Transactional(readOnly = true)
    fun getBy(storeId: Long, ownerId: Long): Store? {
        return repository.findByIdAndOwnerId(storeId, ownerId)
    }

    @Transactional
    fun delete(store: Store) = repository.delete(store)

}