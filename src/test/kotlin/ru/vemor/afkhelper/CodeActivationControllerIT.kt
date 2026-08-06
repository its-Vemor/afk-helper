package ru.vemor.afkhelper

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestClientException
import ru.vemor.afkhelper.client.LilithApi
import ru.vemor.afkhelper.client.LilithApiException
import ru.vemor.afkhelper.client.RoleInfo
import ru.vemor.afkhelper.domain.Account
import ru.vemor.afkhelper.domain.RedemptionCode
import ru.vemor.afkhelper.repository.AccountRepository
import ru.vemor.afkhelper.repository.GameCharacterRepository
import ru.vemor.afkhelper.repository.RedemptionCodeRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
class CodeActivationControllerIT : AbstractPostgresIT() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var characterRepository: GameCharacterRepository

    @Autowired
    lateinit var redemptionCodeRepository: RedemptionCodeRepository

    @MockitoBean
    lateinit var lilithApi: LilithApi

    private val roleA = RoleInfo(isMain = true, svrId = 340, level = 158, name = "Vemor", uid = 1L)
    private val roleB = RoleInfo(isMain = false, svrId = 143, level = 192, name = "Vemor", uid = 2L)
    private val token = "token-x"

    @BeforeEach
    fun clean() {
        characterRepository.deleteAll()
        accountRepository.deleteAll()
        redemptionCodeRepository.deleteAll()
        redemptionCodeRepository.save(RedemptionCode(code = "redeem"))
        Mockito.reset(lilithApi)
    }

    private fun body(
        uid: String?,
        authCode: String?,
        redemptionCode: String?,
    ): String {
        val parts = mutableListOf<String>()
        uid?.let { parts += "\"uid\":\"$it\"" }
        authCode?.let { parts += "\"authCode\":\"$it\"" }
        redemptionCode?.let { parts += "\"redemptionCode\":\"$it\"" }
        return "{${parts.joinToString(",")}}"
    }

    private fun performActivate(requestBody: String) =
        mockMvc.perform(
            post("/api/accounts/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )

    private fun stubVerify() {
        Mockito.doReturn(token).`when`(lilithApi).verifyCode(anyString(), anyString())
    }

    private fun stubRoles(roles: List<RoleInfo>) {
        Mockito.doReturn(roles).`when`(lilithApi).getRoles(anyString(), anyString())
    }

    private fun stubConsumeOk() {
        Mockito.doReturn(true).`when`(lilithApi).consume(anyLong(), anyString(), anyString())
    }

    @Test
    fun `activates code on all characters and persists account and characters`() {
        stubVerify()
        stubRoles(listOf(roleA, roleB))
        stubConsumeOk()

        performActivate("""{"uid":"1","authCode":"123","redemptionCode":"redeem"}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.uid").value("1"))
            .andExpect(jsonPath("$.results.length()").value(2))
            .andExpect(jsonPath("$.results[0].success").value(true))
            .andExpect(jsonPath("$.results[1].success").value(true))

        val account = accountRepository.findByUid("1")
        assertNotNull(account)
        assertEquals(token, account.authToken)

        val characters = characterRepository.findByAccountId(account.id!!)
        assertEquals(2, characters.size)
        assertEquals(listOf(1L, 2L), characters.map { it.uid }.sorted())
    }

    // ---------- EP / BVA: uid ----------

    @Test
    fun `rejects a whitespace-only uid`() {
        stubVerify()
        stubRoles(listOf(roleA))
        stubConsumeOk()
        performActivate(body("   ", "123", "redeem"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value("uid must not be blank"))
    }

    @Test
    fun `rejects an empty uid`() {
        stubVerify()
        stubRoles(listOf(roleA))
        stubConsumeOk()
        performActivate(body("", "123", "redeem"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `rejects a missing uid field`() {
        stubVerify()
        stubRoles(listOf(roleA))
        stubConsumeOk()
        performActivate(body(null, "123", "redeem"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `trims surrounding whitespace from uid`() {
        stubVerify()
        stubRoles(listOf(roleA))
        stubConsumeOk()
        performActivate(body("  1  ", "123", "redeem"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.uid").value("1"))
        assertNotNull(accountRepository.findByUid("1"))
    }

    @Test
    fun `rejects a uid longer than the 64 char limit`() {
        stubVerify()
        stubRoles(emptyList())
        stubConsumeOk()
        performActivate(body("9".repeat(65), "123", "redeem"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
    }

    @Test
    fun `accepts a uid at the 64 char boundary`() {
        stubVerify()
        stubRoles(emptyList())
        stubConsumeOk()
        performActivate(body("9".repeat(64), "123", "redeem"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results.length()").value(0))
    }

    // ---------- EP / BVA: redemptionCode ----------

    @Test
    fun `rejects a blank redemption code`() {
        stubVerify()
        stubRoles(listOf(roleA))
        stubConsumeOk()
        performActivate(body("1", "123", "   "))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value("Redemption code must not be blank"))
    }

    @Test
    fun `rejects a missing redemptionCode field`() {
        stubVerify()
        stubRoles(listOf(roleA))
        stubConsumeOk()
        performActivate(body("1", "123", null))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `trims whitespace from redemption code before calling lilith`() {
        stubVerify()
        stubRoles(listOf(roleA))
        stubConsumeOk()
        performActivate(body("1", "123", "  redeem  "))
            .andExpect(status().isOk)
        Mockito.verify(lilithApi).consume(1L, token, "redeem")
    }

    // ---------- EP: authCode ----------

    @Test
    fun `obtains a token and registers a new account when authCode is provided`() {
        stubVerify()
        stubRoles(listOf(roleA))
        stubConsumeOk()
        performActivate(body("9", "123", "redeem"))
            .andExpect(status().isOk)
        Mockito.verify(lilithApi).verifyCode("9", "123")
        assertNotNull(accountRepository.findByUid("9"))
    }

    @Test
    fun `rejects a new account without an authCode`() {
        stubVerify()
        stubRoles(listOf(roleA))
        stubConsumeOk()
        performActivate(body("9", null, "redeem"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("INVALID_CODE"))
            .andExpect(jsonPath("$.message").value("Auth code is required when the account is not registered yet"))
    }

    @Test
    fun `rejects a new account with a blank authCode`() {
        stubVerify()
        stubRoles(listOf(roleA))
        stubConsumeOk()
        performActivate(body("9", "", "redeem"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `reuses the stored account token and skips verification`() {
        accountRepository.save(Account(uid = "1", authToken = "stored-token"))
        stubRoles(listOf(roleA))
        stubConsumeOk()
        performActivate(body("1", null, "redeem"))
            .andExpect(status().isOk)
        Mockito.verify(lilithApi, Mockito.never()).verifyCode(anyString(), anyString())
        Mockito.verify(lilithApi).getRoles("1", "stored-token")
    }

    // ---------- BVA: number of characters ----------

    @Test
    fun `returns an empty result list when the account has no characters`() {
        stubVerify()
        stubRoles(emptyList())
        stubConsumeOk()
        performActivate(body("1", "123", "redeem"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results.length()").value(0))
    }

    @Test
    fun `activates a single character`() {
        stubVerify()
        stubRoles(listOf(roleA))
        stubConsumeOk()
        performActivate(body("1", "123", "redeem"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results.length()").value(1))
            .andExpect(jsonPath("$.results[0].uid").value(1))
            .andExpect(jsonPath("$.results[0].success").value(true))
            .andExpect(jsonPath("$.results[0].message").value("ok"))
    }

    @Test
    fun `treats a null roles response as an empty list`() {
        stubVerify()
        Mockito.doReturn(null).`when`(lilithApi).getRoles(anyString(), anyString())
        stubConsumeOk()
        performActivate(body("1", "123", "redeem"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results.length()").value(0))
    }

    @Test
    fun `activates many characters and maps each result`() {
        val roles =
            (1L..5L).map { i ->
                RoleInfo(isMain = i == 1L, svrId = 300 + i.toInt(), level = 100 + i.toInt(), name = "Hero$i", uid = i)
            }
        stubVerify()
        stubRoles(roles)
        stubConsumeOk()
        performActivate(body("1", "123", "redeem"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results.length()").value(5))
            .andExpect(jsonPath("$.results[2].name").value("Hero3"))
            .andExpect(jsonPath("$.results[4].uid").value(5))
    }

    // ---------- EP: consume outcomes ----------

    @Test
    fun `collects a per character failure and continues with the rest`() {
        stubVerify()
        stubRoles(listOf(roleA, roleB))
        Mockito
            .doAnswer { inv: org.mockito.invocation.InvocationOnMock ->
                val uid = inv.getArgument<Long>(0)
                if (uid == 2L) {
                    throw LilithApiException("err_cdkey_record_not_found", "err_cdkey_record_not_found")
                }
                true
            }.`when`(lilithApi)
            .consume(anyLong(), anyString(), anyString())

        performActivate(body("1", "123", "redeem"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results.length()").value(2))
            .andExpect(jsonPath("$.results[0].success").value(true))
            .andExpect(jsonPath("$.results[1].success").value(false))
            .andExpect(jsonPath("$.results[1].message").value("err_cdkey_record_not_found"))
    }

    @Test
    fun `marks all characters as failed when every consume throws`() {
        stubVerify()
        stubRoles(listOf(roleA, roleB))
        Mockito
            .doThrow(LilithApiException("err_a", "boom"))
            .`when`(lilithApi)
            .consume(anyLong(), anyString(), anyString())

        performActivate(body("1", "123", "redeem"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results[0].success").value(false))
            .andExpect(jsonPath("$.results[1].success").value(false))
    }

    @Test
    fun `marks a character as failed when consume returns false`() {
        stubVerify()
        stubRoles(listOf(roleA))
        Mockito.doReturn(false).`when`(lilithApi).consume(anyLong(), anyString(), anyString())

        performActivate(body("1", "123", "redeem"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results[0].success").value(false))
            .andExpect(jsonPath("$.results[0].message").value("Redemption code was not exchanged"))
    }

    // ---------- EP: Lilith API errors (502) ----------

    @Test
    fun `returns bad gateway with the lilith error code when verification fails`() {
        Mockito
            .doThrow(LilithApiException("err_bad_code", "verification failed"))
            .`when`(lilithApi)
            .verifyCode(anyString(), anyString())

        performActivate(body("1", "123", "redeem"))
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.error").value("err_bad_code"))
            .andExpect(jsonPath("$.message").value("verification failed"))
    }

    @Test
    fun `returns bad gateway with a generic code when the lilith error code is null`() {
        Mockito
            .doThrow(LilithApiException(null, "boom"))
            .`when`(lilithApi)
            .verifyCode(anyString(), anyString())

        performActivate(body("1", "123", "redeem"))
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.error").value("LILITH_API_ERROR"))
    }

    @Test
    fun `returns bad gateway when fetching roles fails`() {
        stubVerify()
        Mockito
            .doThrow(LilithApiException("err_roles", "roles failed"))
            .`when`(lilithApi)
            .getRoles(anyString(), anyString())

        performActivate(body("1", "123", "redeem"))
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.error").value("err_roles"))
    }

    // ---------- EP: persistence / reactivation ----------

    @Test
    fun `does not duplicate characters or accounts on repeated activation`() {
        stubVerify()
        stubRoles(listOf(roleA))
        stubConsumeOk()

        performActivate(body("1", "123", "redeem")).andExpect(status().isOk)
        performActivate(body("1", "456", "redeem")).andExpect(status().isOk)

        assertEquals(1, accountRepository.findAll().count())
        val account = accountRepository.findByUid("1")!!
        assertEquals(1, characterRepository.findByAccountId(account.id!!).size)
        Mockito.verify(lilithApi, Mockito.times(1)).verifyCode(anyString(), anyString())
    }

    @Test
    fun `updates character attributes when role data changes on reactivation`() {
        stubVerify()
        Mockito
            .doReturn(listOf(roleA), listOf(roleA.copy(level = 999, name = "Neo")))
            .`when`(lilithApi)
            .getRoles(anyString(), anyString())
        stubConsumeOk()

        performActivate(body("1", "123", "redeem")).andExpect(status().isOk)
        performActivate(body("1", "123", "redeem")).andExpect(status().isOk)

        val account = accountRepository.findByUid("1")!!
        val characters = characterRepository.findByAccountId(account.id!!)
        assertEquals(1, characters.size)
        assertEquals(999, characters[0].level)
        assertEquals("Neo", characters[0].name)
    }

    @Test
    fun `returns bad gateway when the lilith client fails with a transport error`() {
        Mockito
            .doThrow(RestClientException("connection refused"))
            .`when`(lilithApi)
            .verifyCode(anyString(), anyString())

        performActivate(body("1", "123", "redeem"))
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.error").value("LILITH_API_ERROR"))
            .andExpect(jsonPath("$.message").value("Upstream redemption service is unavailable"))
    }
}
