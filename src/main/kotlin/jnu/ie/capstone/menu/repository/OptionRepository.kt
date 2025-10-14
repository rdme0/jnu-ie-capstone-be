package jnu.ie.capstone.menu.repository

import jnu.ie.capstone.menu.model.entity.Option
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OptionRepository : JpaRepository<Option, Long> {

    fun findByMenuIdIn(menuId: List<Long>): List<Option>

    fun deleteByMenuIdIn(menuId: List<Long>)

}