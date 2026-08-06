package ru.vemor.afkhelper.service

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import ru.vemor.afkhelper.domain.RedemptionCode
import ru.vemor.afkhelper.repository.AccountRepository

/**
 * Применяет новый код возмещения ко всем известным аккаунтам в фоне.
 *
 * Запускается после того, как REST-запрос на добавление кода уже вернул ответ:
 * [RedemptionAutoApplyEventListener] планирует вызов на AFTER_COMMIT и исполняет
 * его на отдельном пуле потоков (codeActivationExecutor).
 *
 * Для каждого сохранённого аккаунта (у которого есть токен авторизации)
 * переиспользуется существующий конвейер [CodeActivationService] — токен берётся
 * из БД, поэтому код авторизации (`authCode`) не требуется. Ошибки по отдельным
 * аккаунтам изолированы и не прерывают обработку остальных.
 */
@Service
class RedemptionAutoApplyService(
    private val accountRepository: AccountRepository,
    private val codeActivationService: CodeActivationService,
) {
    private val log = LoggerFactory.getLogger(RedemptionAutoApplyService::class.java)

    @Suppress("TooGenericExceptionCaught")
    fun applyToAllKnownCharacters(code: RedemptionCode) {
        log.info("Auto-applying redemption code '{}' to all known accounts", code.code)

        var fullyActivated = 0
        var totalAccounts = 0
        var page = 0
        val pageSize = 1000

        while (true) {
            val accountsPage = accountRepository.findAll(PageRequest.of(page, pageSize))
            if (accountsPage.isEmpty) {
                break
            }

            for (account in accountsPage) {
                totalAccounts++
                try {
                    if (account.authToken.isBlank()) {
                        log.warn("Skipping account {}: no stored auth token", account.uid)
                        continue
                    }
                    val response = codeActivationService.activate(account.uid, code.code)
                    val succeeded = response.results.count { it.success }
                    if (succeeded == response.results.size) {
                        fullyActivated++
                    }
                    log.info(
                        "Auto-applied '{}' for account {}: {}/{} characters succeeded",
                        code.code,
                        account.uid,
                        succeeded,
                        response.results.size,
                    )
                } catch (e: Exception) {
                    log.warn("Failed to auto-apply '{}' for account {}", code.code, account.uid, e)
                }
            }
            page++
        }
        log.info(
            "Auto-apply of '{}' finished: {} of {} accounts fully activated",
            code.code,
            fullyActivated,
            totalAccounts,
        )
    }
}
