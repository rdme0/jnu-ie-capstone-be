package jnu.ie.capstone.menu.repository

import jnu.ie.capstone.menu.model.entity.Option
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OptionRepository : JpaRepository<Option, Long> {

    fun findByMenuIdAndId(menuId: Long, id: Long): Option?

    fun findByMenuId(menuId: Long, pageable: Pageable): Page<Option>

    fun findByMenuIdIn(menuId: List<Long>): List<Option>

    fun deleteAllByMenuId(menuId: Long)

}