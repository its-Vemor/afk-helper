package ru.vemor.afkhelper.service

import ru.vemor.afkhelper.domain.RedemptionCode

/**
 * Событие о добавлении нового кода возмещения. Публикуется после коммита
 * транзакции и запускает фоновое автоприменение ко всем аккаунтам.
 */
data class RedemptionCodeAddedEvent(
    val code: RedemptionCode,
)
