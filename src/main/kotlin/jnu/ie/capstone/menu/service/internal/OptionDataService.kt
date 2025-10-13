package jnu.ie.capstone.menu.service.internal

import jnu.ie.capstone.menu.model.entity.Menu
import jnu.ie.capstone.menu.model.entity.Option
import jnu.ie.capstone.menu.repository.OptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.awt.SystemColor.menu

@Service
class OptionDataService(
    private val repository: OptionRepository
) {

    @Transactional
    fun save(options: List<Option>) {
        repository.saveAll(options)
    }

    @Transactional(readOnly = true)
    fun getAllBy(menuIds: List<Long>) : List<Option> {
        return repository.findByMenuIdIn(menuIds)
    }

    @Transactional
    fun deleteOptionBy(menuIds: List<Long>) {
        repository.deleteByMenuIdIn(menuIds)
    }

    @Transactional
    fun overwrite(menuIdsOfOptions: List<Long>, options: List<Option>) {
        deleteOptionBy(menuIdsOfOptions)
        save(options)
    }

}