package ru.vemor.afkhelper.client

/**
 * Сигнализирует об ошибке при обращении к внешнему API Lilith (cdkey.lilith.com).
 */
class LilithApiException(
    val errorCode: String?,
    message: String?,
) : RuntimeException(message)
