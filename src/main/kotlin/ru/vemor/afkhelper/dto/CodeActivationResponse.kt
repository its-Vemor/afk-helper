package ru.vemor.afkhelper.dto

data class CodeActivationResponse(
    val uid: String,
    val results: List<Result>,
) {
    data class Result(
        val uid: Long,
        val name: String,
        val success: Boolean,
        val message: String? = null,
    )
}
