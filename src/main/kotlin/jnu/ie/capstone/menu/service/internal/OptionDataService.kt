package jnu.ie.capstone.menu.service.internal

import jnu.ie.capstone.menu.model.entity.Option
import jnu.ie.capstone.menu.repository.OptionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OptionDataService(
    private val repository: OptionRepository
) {

    @Transactional
    fun save(option: Option) {
        repository.save(option)
    }

    @Transactional
    fun save(options: List<Option>) {
        repository.saveAll(options)
    }

    @Transactional(readOnly = true)
    fun getBy(id: Long) : Option? {
        return repository.findById(id).orElse(null)
    }

    @Transactional(readOnly = true)
    fun getBy(menuId: Long, id: Long) : Option? {
        return repository.findByMenuIdAndId(menuId, id)
    }

    @Transactional(readOnly = true)
    fun getAllBy(menuId: Long) : List<Option> {
        return repository.findByMenuId(menuId)
    }

    @Transactional(readOnly = true)
    fun getAllBy(menuId: Long, pageable: Pageable) : Page<Option> {
        return repository.findByMenuId(menuId, pageable)
    }

    @Transactional(readOnly = true)
    fun getAllBy(menuIds: List<Long>) : List<Option> {
        return repository.findByMenuIdIn(menuIds)
    }

    @Transactional
    fun deleteBy(id: Long) {
        repository.deleteById(id)
    }

    @Transactional
    fun deleteAllBy(menuId: Long) {
        repository.deleteAllByMenuId(menuId)
    }

    @Transactional
    fun deleteAllBy(menuIds: List<Long>) {
        repository.deleteAllByMenuIdIn(menuIds)
    }

}