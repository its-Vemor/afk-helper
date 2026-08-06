package ru.vemor.afkhelper.client

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import ru.vemor.afkhelper.logging.ApiLogger

@Component
class LilithClient(
    private val properties: LilithProperties,
    private val apiLogger: ApiLogger,
) : LilithApi {
    private val restClient: RestClient =
        RestClient
            .builder()
            .baseUrl(properties.baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()

    override fun verifyCode(
        uid: String,
        code: String,
    ): String {
        apiLogger.request("lilith", "/api/verify-afk-code", mapOf("uid" to uid, "game" to properties.game))
        val body =
            mapOf(
                "uid" to uid,
                "game" to properties.game,
                "code" to code,
            )
        val result =
            restClient
                .post()
                .uri("/api/verify-afk-code")
                .body(body)
                .retrieve()
                .body(VerifyCodeResult::class.java)
        apiLogger.response(
            "lilith",
            "/api/verify-afk-code",
            mapOf("success" to result?.success, "info" to result?.info, "message" to result?.message),
        )

        if (result?.success == true) {
            return result.data?.token
                ?: throw LilithApiException("missing_token", "No token in response")
        }
        throw LilithApiException(result?.info, result?.message)
    }

    override fun getRoles(
        uid: String,
        token: String,
    ): List<RoleInfo> {
        apiLogger.request("lilith", "/api/users", mapOf("uid" to uid, "game" to properties.game))
        val body =
            mapOf(
                "uid" to uid,
                "game" to properties.game,
            )
        val result =
            restClient
                .post()
                .uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .body(body)
                .retrieve()
                .body(UsersResult::class.java)
                ?: throw LilithApiException("empty_response", "Empty response from Lilith API")
        apiLogger.response(
            "lilith",
            "/api/users",
            mapOf("success" to result.success, "info" to result.info, "message" to result.message),
        )

        if (!result.success) {
            throw LilithApiException(result.info, result.message)
        }
        return result.data?.roles.orEmpty()
    }

    override fun consume(
        uid: Long,
        token: String,
        redemptionCode: String,
    ): Boolean {
        apiLogger.request("lilith", "/api/consume", mapOf("roleId" to uid.toString(), "game" to properties.game))
        val body =
            mapOf(
                "appId" to properties.appId,
                "gmEnvId" to properties.gmEnvId,
                "roleId" to uid.toString(),
                "game" to properties.game,
                "cdkey" to redemptionCode,
                "pupBody" to properties.pupBody,
            )
        val result =
            restClient
                .post()
                .uri("/api/consume")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .body(body)
                .retrieve()
                .body(ConsumeResult::class.java)
                ?: throw LilithApiException("empty_response", "Empty response from Lilith API")
        apiLogger.response(
            "lilith",
            "/api/consume",
            mapOf("success" to result.success, "info" to result.info, "exchanged" to result.data?.exchanged),
        )

        if (!result.success) {
            throw LilithApiException(result.info, result.message)
        }
        return result.data?.exchanged ?: false
    }
}
