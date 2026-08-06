package ru.vemor.afkhelper.repository

import org.springframework.data.repository.CrudRepository
import ru.vemor.afkhelper.domain.Account

interface AccountRepository : CrudRepository<Account, Long> {
    fun findByUid(uid: String): Account?
}
