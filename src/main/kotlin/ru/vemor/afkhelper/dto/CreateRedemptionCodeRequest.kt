package ru.vemor.afkhelper.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateRedemptionCodeRequest(
    @field:NotBlank(message = "Code must not be blank")
    @field:Size(max = 255)
    val code: String,
)
