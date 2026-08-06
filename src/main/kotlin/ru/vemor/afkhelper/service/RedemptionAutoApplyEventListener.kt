package ru.vemor.afkhelper.service

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Запускает фоновое автоприменение кода после успешного коммита транзакции
 * добавления кода, чтобы слушатель гарантированно увидел сохранённый код.
 * [fallbackExecution] обеспечивает работу и при отсутствии активной транзакции
 * (например, в unit-тестах).
 */
@Component
class RedemptionAutoApplyEventListener(
    private val autoApplyService: RedemptionAutoApplyService,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Async("codeActivationExecutor")
    fun onCodeAdded(event: RedemptionCodeAddedEvent) {
        autoApplyService.applyToAllKnownCharacters(event.code)
    }
}
