package ru.vemor.afkhelper.telegram

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.generics.TelegramClient

/** Настраивает HTTP-клиент Telegram для отправки сообщений. */
@Configuration
class TelegramClientConfig {
    @Bean
    @ConditionalOnProperty(prefix = "telegram.bot", name = ["enabled"], havingValue = "true")
    fun telegramClient(properties: TelegramBotProperties): TelegramClient = OkHttpTelegramClient(properties.token)
}
