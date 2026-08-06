package ru.vemor.afkhelper.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientException
import ru.vemor.afkhelper.client.LilithApiException
import ru.vemor.afkhelper.dto.ApiError
import ru.vemor.afkhelper.logging.ApiLogger
import ru.vemor.afkhelper.service.ApiException
import ru.vemor.afkhelper.service.ErrorResponseMapper

/**
 * Преобразует доменные и инфраструктурные исключения в единый формат [ApiError]
 * и логирует каждую ошибку.
 */
@RestControllerAdvice
class ApiExceptionHandler(
    private val apiLogger: ApiLogger,
    private val errorResponseMapper: ErrorResponseMapper,
) {
    @ExceptionHandler(ApiException::class)
    fun handleDomain(
        e: ApiException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        logError(request, e.errorCode, e.message)
        return ResponseEntity
            .status(e.httpStatus)
            .body(ApiError(error = e.errorCode, message = e.message ?: "Request failed"))
    }

    @ExceptionHandler(LilithApiException::class)
    fun handleLilith(
        e: LilithApiException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        logError(request, e.errorCode ?: "LILITH_API_ERROR", e.message, e)
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(errorResponseMapper.toApiError(e))
    }

    @ExceptionHandler(RestClientException::class)
    fun handleUpstream(
        e: RestClientException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        logError(request, "LILITH_API_ERROR", "Upstream redemption service is unavailable", e)
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(errorResponseMapper.toApiError(e))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        val message = e.bindingResult.fieldError?.defaultMessage ?: "Request validation failed"
        logError(request, "INVALID_REQUEST", message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiError(error = "INVALID_REQUEST", message = message))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(
        e: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        logError(request, "INVALID_REQUEST", "Malformed request body", e)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiError(error = "INVALID_REQUEST", message = "Malformed request body"))
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedMediaType(
        e: HttpMediaTypeNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        logError(request, "UNSUPPORTED_MEDIA_TYPE", "Unsupported content type: ${e.contentType}")
        return ResponseEntity
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(ApiError(error = "UNSUPPORTED_MEDIA_TYPE", message = "Unsupported content type: ${e.contentType}"))
    }

    private fun logError(
        request: HttpServletRequest,
        errorCode: String,
        message: String?,
        cause: Throwable? = null,
    ) {
        apiLogger.error("rest", "${request.method} ${request.requestURI}", errorCode, message, cause)
    }
}
