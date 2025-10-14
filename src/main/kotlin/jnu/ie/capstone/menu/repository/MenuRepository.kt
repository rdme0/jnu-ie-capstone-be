package jnu.ie.capstone.menu.repository

import jnu.ie.capstone.menu.model.entity.Menu
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MenuRepository : JpaRepository<Menu, Long> {

    fun findByStoreIdAndStoreOwnerId(storeId: Long, ownerId: Long): List<Menu>
    fun findByStoreIdAndStoreOwnerId(storeId: Long, ownerId: Long, pageable: Pageable): Page<Menu>
    fun findByStoreIdAndStoreOwnerIdAndId(storeId: Long, ownerId: Long, id: Long): Menu?
    fun deleteByIdIn(ids: List<Long>)

    @Query(
        value = """
        SELECT m.* FROM menu m
        JOIN store s ON m.store_id = s.id
        WHERE s.id = :storeId AND s.owner_id = :ownerId
        ORDER BY m.embedding <-> CAST(:embedding AS vector)
        LIMIT :limit
    """,
        nativeQuery = true
    )
    fun findRelevantMenus(
        storeId: Long,
        ownerId: Long,
        embedding: FloatArray,
        limit: Int
    ): List<Menu>

}