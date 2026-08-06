package ru.vemor.afkhelper.client

/**
 * Ответ эндпоинта `/api/users` внешнего API Lilith. Внутренняя модель контракта —
 * используется только в [LilithClient].
 */
internal data class UsersResult(
    val success: Boolean,
    val info: String? = null,
    val message: String? = null,
    val data: UsersData? = null,
)
