package ru.vemor.afkhelper.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.vemor.afkhelper.client.RoleInfo
import ru.vemor.afkhelper.domain.GameCharacter
import ru.vemor.afkhelper.repository.GameCharacterRepository

/**
 * Синхронизирует персонажей аккаунта с данными из Lilith (upsert по uid).
 */
@Service
class CharacterSyncService(
    private val characterRepository: GameCharacterRepository,
) {
    @Transactional
    fun syncCharacters(
        accountId: Long,
        roles: List<RoleInfo>,
    ): List<GameCharacter> {
        val existingByUid = characterRepository.findByAccountId(accountId).associateBy { it.uid }

        val toSave =
            roles.map { role ->
                existingByUid[role.uid]?.copy(
                    accountId = accountId,
                    name = role.name,
                    svrId = role.svrId,
                    level = role.level,
                    isMain = role.isMain,
                ) ?: GameCharacter(
                    accountId = accountId,
                    uid = role.uid,
                    name = role.name,
                    svrId = role.svrId,
                    level = role.level,
                    isMain = role.isMain,
                )
            }

        return characterRepository.saveAll(toSave).toList()
    }
}
