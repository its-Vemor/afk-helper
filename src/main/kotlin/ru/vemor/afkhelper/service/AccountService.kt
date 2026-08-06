package ru.vemor.afkhelper.service

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import ru.vemor.afkhelper.client.LilithApi
import ru.vemor.afkhelper.domain.Account
import ru.vemor.afkhelper.repository.AccountRepository

/**
 * Управление аккаунтами: поиск, регистрация и нормализация/валидация uid.
 */
@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val lilithClient: LilithApi,
) {
    /**
     * Возвращает существующий аккаунт по uid или регистрирует новый, запрашивая
     * токен у Lilith. При гонке параллельных запросов переиспользует запись
     * другого потока вместо падения на duplicate key.
     */
    fun ensureAccount(
        uidRaw: String,
        authCode: String?,
    ): Account {
        val uid = requireUid(uidRaw)
        accountRepository.findByUid(uid)?.let { return it }

        val code = requireAuthCode(authCode)
        val token = lilithClient.verifyCode(uid, code)
        return try {
            accountRepository.save(Account(uid = uid, authToken = token))
        } catch (e: DataIntegrityViolationException) {
            // Параллельный запрос успел зарегистрировать тот же uid — переиспользуем запись.
            accountRepository.findByUid(uid) ?: throw e
        }
    }

    private fun requireUid(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            throw InvalidCodeException("uid must not be blank")
        }
        return trimmed
    }

    private fun requireAuthCode(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            throw InvalidCodeException("Auth code is required when the account is not registered yet")
        }
        return trimmed
    }
}
