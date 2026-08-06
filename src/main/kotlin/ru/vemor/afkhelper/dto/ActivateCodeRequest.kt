package ru.vemor.afkhelper.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ActivateCodeRequest(
    @field:NotBlank(message = "uid must not be blank")
    @field:Size(max = 64)
    val uid: String,
    val authCode: String? = null,
    @field:NotBlank(message = "Redemption code must not be blank")
    val redemptionCode: String,
)
