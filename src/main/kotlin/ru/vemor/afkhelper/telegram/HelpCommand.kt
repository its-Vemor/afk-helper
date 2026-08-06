package ru.vemor.afkhelper.telegram

import org.springframework.stereotype.Component

@Component
class HelpCommand : TelegramCommand {
    override val names: Set<String> = setOf("/start", "/help")

    override fun execute(
        parts: List<String>,
        command: String,
    ): String = HELP

    companion object {
        const val HELP: String =
            "Доступные команды:\n" +
                "/add <код> — добавить новый код возмещения (автоприменение ко всем аккаунтам)\n" +
                "/activate <uid> <код> [authCode] — активировать код на аккаунте\n" +
                "/help — справка"
    }
}
