package ru.vemor.afkhelper

import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.vemor.afkhelper.client.LilithApi
import ru.vemor.afkhelper.repository.AccountRepository
import ru.vemor.afkhelper.repository.GameCharacterRepository
import ru.vemor.afkhelper.repository.RedemptionCodeRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@AutoConfigureMockMvc
class RedemptionCodeControllerIT : AbstractPostgresIT() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var repository: RedemptionCodeRepository

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var characterRepository: GameCharacterRepository

    @MockitoBean
    lateinit var lilithApi: LilithApi

    @BeforeEach
    fun clean() {
        // Чистим дочерние сущности раньше родительских (FK).
        characterRepository.deleteAll()
        accountRepository.deleteAll()
        repository.deleteAll()
        Mockito.reset(lilithApi)
    }

    private fun performPost(code: String) =
        mockMvc.perform(
            post("/api/codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"$code"}"""),
        )

    // ---------- EP: valid ----------

    @Test
    fun `adds a redemption code and persists it`() {
        performPost("afk-test-001")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("afk-test-001"))
            .andExpect(jsonPath("$.isActive").value(true))
            .andExpect(jsonPath("$.id").isNumber)

        val saved = repository.findAll().firstOrNull { it.code == "afk-test-001" }
        assertNotNull(saved)
        assertEquals("afk-test-001", saved.code)
        assertTrue(saved.isActive)
    }

    @Test
    fun `lowercase code is stored as-is`() {
        performPost("afk-lowercase")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("afk-lowercase"))
    }

    @Test
    fun `code with surrounding whitespace is trimmed`() {
        performPost("  padded  ")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("padded"))
    }

    @Test
    fun `code with letters digits and underscore is accepted`() {
        performPost("A1-b2_c3")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("A1-b2_c3"))
    }

    // ---------- EP: blank -> 400 ----------

    @Test
    fun `returns bad request for an empty code`() {
        performPost("")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value("Code must not be blank"))
    }

    @Test
    fun `returns bad request for a whitespace-only code`() {
        performPost("   ")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
    }

    @Test
    fun `returns bad request for a tabs and newlines code`() {
        mockMvc
            .perform(
                post("/api/codes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"code":"\t\n"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
    }

    // ---------- EP: unmarshallable -> 400 ----------

    @Test
    fun `returns bad request for a missing code field`() {
        mockMvc
            .perform(post("/api/codes").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `returns bad request for a null code`() {
        mockMvc
            .perform(post("/api/codes").contentType(MediaType.APPLICATION_JSON).content("""{"code":null}"""))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `returns bad request for malformed json`() {
        mockMvc
            .perform(post("/api/codes").contentType(MediaType.APPLICATION_JSON).content("""{"code":}"""))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `returns bad request for an empty body`() {
        mockMvc
            .perform(post("/api/codes").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `returns unsupported media type for non-json content`() {
        mockMvc
            .perform(post("/api/codes").contentType(MediaType.TEXT_PLAIN).content("abc"))
            .andExpect(status().isUnsupportedMediaType)
    }
    // ---------- EP: duplicates -> 409 ----------

    @Test
    fun `returns conflict for a duplicate code`() {
        performPost("AFK-DUP")
            .andExpect(status().isCreated)

        performPost("AFK-DUP")
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("CODE_ALREADY_EXISTS"))
    }

    @Test
    fun `treats codes differing only in case as distinct`() {
        performPost("BOUNDARY-CASE")
            .andExpect(status().isCreated)

        performPost("boundary-case")
            .andExpect(status().isCreated)
    }

    @Test
    fun `returns conflict for a duplicate code differing in whitespace`() {
        performPost("BOUNDARY-CASE")
            .andExpect(status().isCreated)

        performPost("   BOUNDARY-CASE   ")
            .andExpect(status().isConflict)
    }

    // ---------- BVA: length ----------

    @Test
    fun `accepts a single character code`() {
        performPost("A")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("A"))
    }

    @Test
    fun `accepts a code at the 255 character boundary`() {
        performPost("A".repeat(255))
            .andExpect(status().isCreated)
    }

    @Test
    fun `rejects a code over the 255 character limit`() {
        performPost("A".repeat(256))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
    }
}
