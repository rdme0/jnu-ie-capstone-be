package jnu.ie.capstone.rtzr.cache.service

import jnu.ie.capstone.rtzr.cache.entity.RtzrAccessToken
import jnu.ie.capstone.rtzr.cache.repository.RtzrAccessTokenRepository
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class RtzrAccessTokenService(
    private val repository: RtzrAccessTokenRepository
) {

    companion object {
        private const val ID = "rtzr_access_token"
    }

    fun get() = repository.findById(ID).getOrNull()?.value

    fun overwrite(token: String) : String {
        val entity = RtzrAccessToken(ID, token)
        return repository.save(entity).value
    }
}