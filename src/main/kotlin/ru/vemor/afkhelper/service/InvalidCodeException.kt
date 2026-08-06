package ru.vemor.afkhelper.service

import org.springframework.http.HttpStatus

/**
 * Сигнализирует, что переданный код возмещения невалиден (пустой / из пробелов).
 */
class InvalidCodeException(
    message: String,
) : ApiException("INVALID_CODE", HttpStatus.BAD_REQUEST, message)
