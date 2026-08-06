package ru.vemor.afkhelper.client

/**
 * Ответ эндпоинта `/api/verify-afk-code` внешнего API Lilith. Внутренняя модель
 * контракта — используется только в [LilithClient].
 */
internal data class VerifyCodeResult(
    val success: Boolean,
    val info: String? = null,
    val message: String? = null,
    val data: VerifyData? = null,
)
