package ru.vemor.afkhelper.dto

/**
 * Данные сохранённого кода возмещения, возвращаемые клиенту.
 */
data class RedemptionCodeResponse(
    val id: Long,
    val code: String,
    val isActive: Boolean,
)
