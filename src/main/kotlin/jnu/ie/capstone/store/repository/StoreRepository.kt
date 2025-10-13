package jnu.ie.capstone.store.repository

import jnu.ie.capstone.store.model.entity.Store
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StoreRepository : JpaRepository<Store, Long> {
    fun findByIdAndOwnerId(id: Long, ownerId: Long): Store?
}