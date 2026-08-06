package ru.vemor.afkhelper.repository

import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.CrudRepository
import ru.vemor.afkhelper.domain.RedemptionCode

interface RedemptionCodeRepository : CrudRepository<RedemptionCode, Long> {
    fun existsByCode(code: String): Boolean

    @Cacheable("redemptionCodes")
    fun findByCode(code: String): RedemptionCode?
}
