package ru.vemor.afkhelper.telegram

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Конфигурация Telegram-бота.
 *
 * @param token токен бота (@BotFather)
 * @param username имя бота (опционально, для отображения в командах)
 * @param enabled включает/выключает создание бинов бота и клиента
 */
@ConfigurationProperties(prefix = "telegram.bot")
data class TelegramBotProperties(
    val token: String = "",
    val username: String? = null,
    val enabled: Boolean = true,
)
