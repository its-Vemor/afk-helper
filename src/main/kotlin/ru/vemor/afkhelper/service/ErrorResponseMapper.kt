package ru.vemor.afkhelper.service

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import ru.vemor.afkhelper.client.LilithApiException
import ru.vemor.afkhelper.dto.ApiError

/**
 * Единая точка преобразования доменных и инфраструктурных исключений в
 * унифицированный [ApiError]. Используется как REST [ru.vemor.afkhelper.controller.ApiExceptionHandler],
 * так и Telegram-слоем, чтобы маппинг ошибок был в одном месте (DRY).
 */
@Component
class ErrorResponseMapper {
    fun toApiError(e: Throwable): ApiError = ApiError(errorCode(e), message(e))

    fun errorCode(e: Throwable): String =
        when (e) {
            is ApiException -> e.errorCode
            is LilithApiException -> e.errorCode ?: "LILITH_API_ERROR"
            is RestClientException -> "LILITH_API_ERROR"
            else -> "INTERNAL_ERROR"
        }

    fun message(e: Throwable): String =
        when (e) {
            is ApiException -> e.message ?: "Request failed"
            is LilithApiException -> e.message ?: "Lilith API error"
            is RestClientException -> "Upstream redemption service is unavailable"
            else -> "Internal server error"
        }
}
