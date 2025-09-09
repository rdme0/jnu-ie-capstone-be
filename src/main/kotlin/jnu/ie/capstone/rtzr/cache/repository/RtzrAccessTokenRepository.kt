package jnu.ie.capstone.rtzr.cache.repository

import org.springframework.data.repository.CrudRepository
import jnu.ie.capstone.rtzr.cache.entity.RtzrAccessToken
import java.util.Optional

interface RtzrAccessTokenRepository : CrudRepository<RtzrAccessToken, String> {
    override fun findById(id: String) : Optional<RtzrAccessToken>
}