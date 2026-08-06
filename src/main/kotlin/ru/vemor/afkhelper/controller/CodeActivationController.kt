package ru.vemor.afkhelper.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.vemor.afkhelper.dto.ActivateCodeRequest
import ru.vemor.afkhelper.dto.CodeActivationResponse
import ru.vemor.afkhelper.logging.ApiLogger
import ru.vemor.afkhelper.service.CodeActivationService

@RestController
@RequestMapping("/api/accounts")
class CodeActivationController(
    private val service: CodeActivationService,
    private val apiLogger: ApiLogger,
) {
    @PostMapping("/activate")
    fun activateCode(
        @Valid @RequestBody request: ActivateCodeRequest,
    ): CodeActivationResponse {
        apiLogger.request(
            "rest",
            "POST /api/accounts/activate",
            mapOf(
                "uid" to request.uid,
                "hasAuthCode" to (request.authCode != null),
                "redemptionCode" to request.redemptionCode,
            ),
        )
        val response = service.activate(request)
        apiLogger.response("rest", "POST /api/accounts/activate", response)
        return response
    }
}
