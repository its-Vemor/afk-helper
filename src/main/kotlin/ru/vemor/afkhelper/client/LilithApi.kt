package ru.vemor.afkhelper.client

interface LilithApi {
    fun verifyCode(
        uid: String,
        code: String,
    ): String

    fun getRoles(
        uid: String,
        token: String,
    ): List<RoleInfo>

    fun consume(
        uid: Long,
        token: String,
        redemptionCode: String,
    ): Boolean
}
