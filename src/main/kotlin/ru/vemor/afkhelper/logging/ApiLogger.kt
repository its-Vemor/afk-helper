package ru.vemor.afkhelper.logging

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.vemor.afkhelper.config.AppLoggingProperties

/**
 * Единая точка логирования вызовов/ответов/ошибок для всех каналов
 * (REST, Telegram, Lilith).
 *
 * Вызовы и ответы логируются только при [AppLoggingProperties.enabled] = true;
 * ошибки записываются всегда. Значения чувствительных полей (token, authCode,
 * authorization, secret, password) маскируются.
 */
@Component
class ApiLogger(
    private val properties: AppLoggingProperties,
) {
    private val log = LoggerFactory.getLogger(ApiLogger::class.java)

    private val sensitiveKey = Regex("(?i).*(token|authcode|authorization|secret|password).*")

    fun request(
        channel: String,
        operation: String,
        params: Map<String, Any?> = emptyMap(),
    ) {
        if (!properties.enabled) {
            return
        }
        log.info("[{}] REQUEST {} params={}", channel, operation, scrub(params))
    }

    fun response(
        channel: String,
        operation: String,
        result: Any?,
        durationMs: Long? = null,
    ) {
        if (!properties.enabled) {
            return
        }
        val duration = durationMs?.let { " durationMs=$it" }.orEmpty()
        log.info("[{}] RESPONSE {} result={}{}", channel, operation, result ?: "-", duration)
    }

    fun error(
        channel: String,
        operation: String,
        errorCode: String?,
        message: String?,
        cause: Throwable? = null,
    ) {
        val text = "[$channel] ERROR $operation errorCode=${errorCode ?: "-"} message=${message ?: "-"}"
        if (cause != null) log.warn(text, cause) else log.warn(text)
    }

    internal fun scrub(params: Map<String, Any?>): Map<String, Any?> =
        params.mapValues { (key, value) -> if (sensitiveKey.matches(key)) MASK else value }

    private companion object {
        const val MASK: String = "[MASKED]"
    }
}
