package ru.vemor.afkhelper.client

/**
 * Ответ эндпоинта `/api/consume` внешнего API Lilith. Внутренняя модель контракта —
 * используется только в [LilithClient].
 */
internal data class ConsumeResult(
    val success: Boolean,
    val info: String? = null,
    val message: String? = null,
    val data: ConsumeData? = null,
)
