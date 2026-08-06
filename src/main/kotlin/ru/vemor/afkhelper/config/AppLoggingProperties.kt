package ru.vemor.afkhelper.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Настройки логирования вызовов и ответов.
 *
 * @param enabled при true логируются вызовы и ответы (REST, Telegram, Lilith);
 *                ошибки логируются всегда, независимо от флага
 */
@ConfigurationProperties(prefix = "app.logging")
data class AppLoggingProperties(
    val enabled: Boolean = false,
)
