package ru.vemor.afkhelper.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.vemor.afkhelper.dto.CreateRedemptionCodeRequest
import ru.vemor.afkhelper.dto.RedemptionCodeResponse
import ru.vemor.afkhelper.logging.ApiLogger
import ru.vemor.afkhelper.service.RedemptionCodeService

@RestController
@RequestMapping("/api/codes")
class RedemptionCodeController(
    private val service: RedemptionCodeService,
    private val apiLogger: ApiLogger,
) {
    @PostMapping
    fun addCode(
        @Valid @RequestBody request: CreateRedemptionCodeRequest,
    ): ResponseEntity<RedemptionCodeResponse> {
        apiLogger.request("rest", "POST /api/codes", mapOf("code" to request.code))
        val response = service.addCode(request)
        apiLogger.response("rest", "POST /api/codes", response)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
