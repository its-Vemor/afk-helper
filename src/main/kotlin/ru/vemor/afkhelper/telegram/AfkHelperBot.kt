package ru.vemor.afkhelper.telegram

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.generics.TelegramClient

/**
 * Точка входа Telegram-бота (long polling).
 *
 * Бин, реализующий [SpringLongPollingBot], автоматически регистрируется стартером
 * (telegrambots-springboot-longpolling-starter). Обработка текстовых команд
 * делегируется в [TelegramMessageHandler], который вызывает существующие сервисы —
 * REST API и бот используют единую бизнес-логику.
 */
@Component
@ConditionalOnProperty(prefix = "telegram.bot", name = ["enabled"], havingValue = "true")
class AfkHelperBot(
    private val properties: TelegramBotProperties,
    private val telegramClient: TelegramClient,
    private val messageHandler: TelegramMessageHandler,
) : SpringLongPollingBot {
    private val log = LoggerFactory.getLogger(AfkHelperBot::class.java)

    override fun getBotToken(): String = properties.token

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = LongPollingUpdateConsumer(::handleUpdates)

    private fun handleUpdates(updates: List<Update>) {
        updates.forEach(::handleUpdate)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun handleUpdate(update: Update) {
        try {
            if (!update.hasMessage() || !update.message.hasText()) {
                return
            }
            val message = update.message
            val reply = messageHandler.handle(message.text)
            val sendMessage =
                SendMessage
                    .builder()
                    .chatId(message.chatId)
                    .text(reply)
                    .build()
            telegramClient.execute(sendMessage)
        } catch (e: Exception) {
            // Сбой при обработке одного update не должен останавливать long polling.
            log.warn("Failed to process Telegram update", e)
        }
    }
}
