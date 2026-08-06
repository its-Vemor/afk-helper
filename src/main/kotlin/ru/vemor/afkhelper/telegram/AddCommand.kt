package ru.vemor.afkhelper.telegram

import org.springframework.stereotype.Component
import ru.vemor.afkhelper.dto.CreateRedemptionCodeRequest
import ru.vemor.afkhelper.logging.ApiLogger
import ru.vemor.afkhelper.service.ApiException
import ru.vemor.afkhelper.service.ErrorResponseMapper
import ru.vemor.afkhelper.service.RedemptionCodeService

@Component
class AddCommand(
    private val redemptionCodeService: RedemptionCodeService,
    private val apiLogger: ApiLogger,
    private val errorResponseMapper: ErrorResponseMapper,
) : TelegramCommand {
    override val names: Set<String> = setOf("/add", "/addcode")

    override fun execute(
        parts: List<String>,
        command: String,
    ): String {
        val code = parts.getOrNull(1)
        if (code.isNullOrBlank()) {
            return "Использование: /add <код>\nПример: /add afk2024"
        }
        return try {
            val saved = redemptionCodeService.addCode(CreateRedemptionCodeRequest(code = code))
            "Код ${saved.code} добавлен (id = ${saved.id}, isActive = ${saved.isActive}). " +
                "Автоматическое применение ко всем аккаунтам запущено в фоне."
        } catch (e: ApiException) {
            apiLogger.error("telegram", command, e.errorCode, e.message)
            "Не удалось добавить код: ${errorResponseMapper.message(e)}"
        }
    }
}
