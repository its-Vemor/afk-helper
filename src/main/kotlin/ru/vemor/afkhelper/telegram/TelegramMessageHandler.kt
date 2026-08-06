package ru.vemor.afkhelper.telegram

import org.springframework.stereotype.Component
import ru.vemor.afkhelper.logging.ApiLogger

/**
 * Обрабатывает текстовые команды Telegram-бота, вызывая зарегистрированные [TelegramCommand].
 * Слой не зависит от Telegram API, поэтому легко покрывается unit-тестами.
 */
@Component
class TelegramMessageHandler(
    commands: List<TelegramCommand>,
    private val apiLogger: ApiLogger,
) {
    private val commandRegistry = commands.flatMap { cmd -> cmd.names.map { it to cmd } }.toMap()

    fun handle(text: String): String {
        val parts = text.trim().split(Regex("\\s+"))
        if (parts.isEmpty() || parts[0].isBlank()) {
            return HelpCommand.HELP
        }
        val command = parts[0].substringBefore('@').lowercase()
        apiLogger.request("telegram", command, mapOf("channel" to "telegram"))

        val handler = commandRegistry[command]
        val reply = handler?.execute(parts, command) ?: HelpCommand.HELP

        apiLogger.response("telegram", command, reply)
        return reply
    }
}
