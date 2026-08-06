package ru.vemor.afkhelper.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import ru.vemor.afkhelper.domain.Account

interface AccountRepository :
    CrudRepository<Account, Long>,
    PagingAndSortingRepository<Account, Long> {
    fun findByUid(uid: String): Account?
}
