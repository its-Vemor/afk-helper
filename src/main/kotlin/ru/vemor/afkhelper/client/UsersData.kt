package ru.vemor.afkhelper.client

/**
 * Данные успешного ответа о пользователе в [UsersResult].
 */
internal data class UsersData(
    val roles: List<RoleInfo>? = null,
)
