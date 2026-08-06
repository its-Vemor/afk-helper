package ru.vemor.afkhelper.telegram

interface TelegramCommand {
    val names: Set<String>

    fun execute(
        parts: List<String>,
        command: String,
    ): String
}
