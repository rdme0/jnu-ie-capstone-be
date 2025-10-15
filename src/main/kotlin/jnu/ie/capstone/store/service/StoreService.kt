package jnu.ie.capstone.store.service

import jnu.ie.capstone.store.model.entity.Store
import jnu.ie.capstone.store.repository.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StoreService(
    private val repository: StoreRepository
) {

    @Transactional
    fun getBy(id: Long, ownerId: Long): Store? {
        return repository.findByIdAndOwnerId(id, ownerId)
    }

}