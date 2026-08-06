package ru.vemor.afkhelper.dto

/**
 * Единая структура ответа об ошибке для всех эндпоинтов REST API.
 */
data class ApiError(
    val error: String,
    val message: String,
)
