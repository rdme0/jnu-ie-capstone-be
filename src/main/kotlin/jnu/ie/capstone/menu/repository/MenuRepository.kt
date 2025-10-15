package jnu.ie.capstone.menu.repository

import jnu.ie.capstone.menu.model.entity.Menu
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MenuRepository : JpaRepository<Menu, Long> {

    fun findByStoreId(storeId: Long): List<Menu>
    fun findByStoreId(storeId: Long, pageable: Pageable): Page<Menu>
    fun findByStoreIdAndId(storeId: Long, id: Long): Menu?
    fun deleteByIdIn(ids: List<Long>)

    @Query(
        value = """
        SELECT m.* FROM menu m
        JOIN store s ON m.store_id = s.id
        WHERE s.id = :storeId
        ORDER BY m.embedding <-> CAST(:embedding AS vector)
        LIMIT :limit
    """,
        nativeQuery = true
    )
    fun findRelevantMenus(
        storeId: Long,
        embedding: FloatArray,
        limit: Int
    ): List<Menu>

}