package ru.vemor.afkhelper

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClientException
import ru.vemor.afkhelper.client.LilithApiException
import ru.vemor.afkhelper.client.LilithClient
import ru.vemor.afkhelper.client.LilithProperties
import ru.vemor.afkhelper.config.AppLoggingProperties
import ru.vemor.afkhelper.logging.ApiLogger
import java.net.InetSocketAddress
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LilithClientIT {
    companion object {
        private lateinit var server: HttpServer
        private val responses = mutableMapOf<String, String>()
        private var statusCode: Int = 200

        @JvmStatic
        @BeforeAll
        fun startServer() {
            server = HttpServer.create(InetSocketAddress(0), 0)
            server.createContext("/") { exchange ->
                val path = exchange.requestURI.path
                val body = responses[path] ?: "{}"
                val bytes = body.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            server.stop(0)
        }
    }

    private fun newClient(): LilithClient {
        val baseUrl = "http://localhost:${server.address.port}"
        return LilithClient(
            properties = LilithProperties(baseUrl = baseUrl),
            apiLogger = ApiLogger(AppLoggingProperties(enabled = false)),
        )
    }

    private fun respond(
        path: String,
        json: String,
        code: Int = 200,
    ) {
        statusCode = code
        responses[path] = json
    }

    // ---------- verifyCode ----------

    @Test
    fun `verifyCode returns the token on success`() {
        respond("/api/verify-afk-code", """{"success":true,"data":{"token":"t-1"}}""")
        assertEquals("t-1", newClient().verifyCode("1", "c1"))
    }

    @Test
    fun `verifyCode throws missing_token when token is absent`() {
        respond("/api/verify-afk-code", """{"success":true}""")
        val e = assertFailsWith<LilithApiException> { newClient().verifyCode("1", "c1") }
        assertEquals("missing_token", e.errorCode)
    }

    @Test
    fun `verifyCode throws with error info on failure`() {
        respond("/api/verify-afk-code", """{"success":false,"info":"bad_code","message":"nope"}""")
        val e = assertFailsWith<LilithApiException> { newClient().verifyCode("1", "c1") }
        assertEquals("bad_code", e.errorCode)
    }

    @Test
    fun `verifyCode surfaces a server error as a RestClientException`() {
        respond("/api/verify-afk-code", """{"error":"internal"}""", code = 500)
        assertFailsWith<RestClientException> { newClient().verifyCode("1", "c1") }
    }

    // ---------- getRoles ----------

    @Test
    fun `getRoles parses the returned roles`() {
        respond(
            "/api/users",
            """{"success":true,"data":{"roles":[{"is_main":true,"svr_id":340,"level":158,"name":"Vemor","uid":1}]}}""",
        )
        val roles = newClient().getRoles("1", "t")
        assertEquals(1, roles.size)
        with(roles[0]) {
            assertEquals(1L, uid)
            assertTrue(isMain)
            assertEquals(340, svrId)
            assertEquals(158, level)
            assertEquals("Vemor", name)
        }
    }

    @Test
    fun `getRoles treats missing roles data as an empty list`() {
        respond("/api/users", """{"success":true}""")
        assertTrue(newClient().getRoles("1", "t").isEmpty())
    }

    @Test
    fun `getRoles throws when the response is not successful`() {
        respond("/api/users", """{"success":false,"info":"no_roles","message":"none"}""")
        val e = assertFailsWith<LilithApiException> { newClient().getRoles("1", "t") }
        assertEquals("no_roles", e.errorCode)
    }

    // ---------- consume ----------

    @Test
    fun `consume returns false when the code was not exchanged`() {
        respond("/api/consume", """{"success":true,"data":{"exchanged":false}}""")
        assertFalse(newClient().consume(1L, "t", "CODE"))
    }

    @Test
    fun `consume returns true when the code was exchanged`() {
        respond("/api/consume", """{"success":true,"data":{"exchanged":true}}""")
        assertTrue(newClient().consume(1L, "t", "CODE"))
    }

    @Test
    fun `consume throws when the response is not successful`() {
        respond("/api/consume", """{"success":false,"info":"err_consume","message":"fail"}""")
        val e = assertFailsWith<LilithApiException> { newClient().consume(1L, "t", "CODE") }
        assertEquals("err_consume", e.errorCode)
    }
}
