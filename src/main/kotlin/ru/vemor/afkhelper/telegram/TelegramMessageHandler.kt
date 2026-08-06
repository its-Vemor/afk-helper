package ru.vemor.afkhelper.telegram

import org.springframework.stereotype.Component
import ru.vemor.afkhelper.dto.ActivateCodeRequest
import ru.vemor.afkhelper.dto.CodeActivationResponse
import ru.vemor.afkhelper.dto.CreateRedemptionCodeRequest
import ru.vemor.afkhelper.logging.ApiLogger
import ru.vemor.afkhelper.service.ApiException
import ru.vemor.afkhelper.service.CodeActivationService
import ru.vemor.afkhelper.service.ErrorResponseMapper
import ru.vemor.afkhelper.service.RedemptionCodeService

/**
 * Обрабатывает текстовые команды Telegram-бота, вызывая существующие сервисы
 * (те же, что использует REST API), и возвращает текст ответа пользователю.
 * Маппинг ошибок делегируется в [ErrorResponseMapper] — единый с REST.
 *
 * Слой не зависит от Telegram API, поэтому легко покрывается unit-тестами.
 */
@Component
class TelegramMessageHandler(
    private val redemptionCodeService: RedemptionCodeService,
    private val codeActivationService: CodeActivationService,
    private val apiLogger: ApiLogger,
    private val errorResponseMapper: ErrorResponseMapper,
) {
    fun handle(text: String): String {
        val parts = text.trim().split(Regex("\\s+"))
        if (parts.isEmpty() || parts[0].isBlank()) {
            return HELP
        }
        val command = parts[0].substringBefore('@').lowercase()
        apiLogger.request("telegram", command, mapOf("channel" to "telegram"))
        val reply =
            when (command) {
                "/add", "/addcode" -> handleAdd(parts, command)
                "/activate", "/use" -> handleActivate(parts, command)
                "/start", "/help" -> HELP
                else -> HELP
            }
        apiLogger.response("telegram", command, reply)
        return reply
    }

    private fun handleAdd(
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

    @Suppress("TooGenericExceptionCaught")
    private fun handleActivate(
        parts: List<String>,
        command: String,
    ): String {
        val uid = parts.getOrNull(1)
        val code = parts.getOrNull(2)
        if (uid.isNullOrBlank() || code.isNullOrBlank()) {
            return "Использование: /activate <uid> <код> [authCode]\nПример: /activate 12345 afk2024"
        }
        val authCode = parts.getOrNull(3)?.takeIf { it.isNotBlank() }
        return try {
            val response =
                codeActivationService.activate(
                    ActivateCodeRequest(uid = uid, authCode = authCode, redemptionCode = code),
                )
            formatResults(response)
        } catch (e: Exception) {
            apiLogger.error("telegram", command, errorResponseMapper.errorCode(e), errorResponseMapper.message(e), e)
            "Не удалось активировать код: ${errorResponseMapper.message(e)}"
        }
    }

    private fun formatResults(response: CodeActivationResponse): String {
        if (response.results.isEmpty()) {
            return "У аккаунта ${response.uid} нет персонажей."
        }
        val lines =
            response.results.joinToString("\n") { result ->
                val status = if (result.success) "ok" else (result.message ?: "failed")
                "${result.name} (uid = ${result.uid}): $status"
            }
        return "Активация кода на аккаунте ${response.uid}:\n$lines"
    }

    private companion object {
        const val HELP: String =
            "Доступные команды:\n" +
                "/add <код> — добавить новый код возмещения (автоприменение ко всем аккаунтам)\n" +
                "/activate <uid> <код> [authCode] — активировать код на аккаунте\n" +
                "/help — справка"
    }
}
