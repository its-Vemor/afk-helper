package ru.vemor.afkhelper.telegram

import org.springframework.stereotype.Component
import ru.vemor.afkhelper.dto.ActivateCodeRequest
import ru.vemor.afkhelper.dto.CodeActivationResponse
import ru.vemor.afkhelper.logging.ApiLogger
import ru.vemor.afkhelper.service.CodeActivationService
import ru.vemor.afkhelper.service.ErrorResponseMapper

@Component
class ActivateCommand(
    private val codeActivationService: CodeActivationService,
    private val apiLogger: ApiLogger,
    private val errorResponseMapper: ErrorResponseMapper,
) : TelegramCommand {
    override val names: Set<String> = setOf("/activate", "/use")

    @Suppress("TooGenericExceptionCaught")
    override fun execute(
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
}
