package ru.vemor.afkhelper.repository

import org.springframework.data.repository.CrudRepository
import ru.vemor.afkhelper.domain.GameCharacter

interface GameCharacterRepository : CrudRepository<GameCharacter, Long> {
    fun findByAccountId(accountId: Long): List<GameCharacter>
}
