package jnu.ie.capstone.rtzr.cache.repository

import jnu.ie.capstone.rtzr.cache.entity.RtzrAccessToken
import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface RtzrAccessTokenRepository : CrudRepository<RtzrAccessToken, String> {
    override fun findById(id: String) : Optional<RtzrAccessToken>
}