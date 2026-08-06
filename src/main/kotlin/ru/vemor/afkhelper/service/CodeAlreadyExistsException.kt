package ru.vemor.afkhelper.service

import org.springframework.http.HttpStatus

/**
 * Сигнализирует, что код возмещения с таким значением уже существует в системе.
 */
class CodeAlreadyExistsException(
    message: String,
) : ApiException("CODE_ALREADY_EXISTS", HttpStatus.CONFLICT, message)
