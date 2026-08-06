package ru.vemor.afkhelper.service

import org.springframework.http.HttpStatus

/**
 * Базовое доменное исключение API. Каждый наследник несёт стабильный [errorCode]
 * для клиентов и [httpStatus], по которому [ru.vemor.afkhelper.controller.ApiExceptionHandler]
 * строит ответ без дублирования маппинга.
 */
open class ApiException(
    val errorCode: String,
    val httpStatus: HttpStatus,
    message: String,
) : RuntimeException(message)
