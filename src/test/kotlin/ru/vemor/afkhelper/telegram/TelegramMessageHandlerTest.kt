package ru.vemor.afkhelper.telegram

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.vemor.afkhelper.config.AppLoggingProperties
import ru.vemor.afkhelper.dto.ActivateCodeRequest
import ru.vemor.afkhelper.dto.CodeActivationResponse
import ru.vemor.afkhelper.dto.CreateRedemptionCodeRequest
import ru.vemor.afkhelper.dto.RedemptionCodeResponse
import ru.vemor.afkhelper.logging.ApiLogger
import ru.vemor.afkhelper.service.CodeActivationService
import ru.vemor.afkhelper.service.CodeAlreadyExistsException
import ru.vemor.afkhelper.service.ErrorResponseMapper
import ru.vemor.afkhelper.service.InvalidCodeException
import ru.vemor.afkhelper.service.RedemptionCodeService
import kotlin.test.assertContains
import kotlin.test.assertEquals

class TelegramMessageHandlerTest {
    private val redemptionCodeService: RedemptionCodeService = Mockito.mock(RedemptionCodeService::class.java)
    private val codeActivationService: CodeActivationService = Mockito.mock(CodeActivationService::class.java)
    private val apiLogger = ApiLogger(AppLoggingProperties(enabled = false))
    private val errorResponseMapper = ErrorResponseMapper()
    private val addCommand = AddCommand(redemptionCodeService, apiLogger, errorResponseMapper)
    private val activateCommand = ActivateCommand(codeActivationService, apiLogger, errorResponseMapper)
    private val helpCommand = HelpCommand()
    private val handler =
        TelegramMessageHandler(listOf(addCommand, activateCommand, helpCommand), apiLogger)

    // ---------- /add ----------

    @Test
    fun `add command saves a new code and reports success`() {
        Mockito
            .doReturn(RedemptionCodeResponse(id = 1L, code = "AFK-2024", isActive = true))
            .`when`(redemptionCodeService)
            .addCode(CreateRedemptionCodeRequest(code = "afk-2024"))

        val reply = handler.handle("/add afk-2024")

        assertContains(reply, "AFK-2024")
        assertContains(reply, "добавлен")
        Mockito
            .verify(redemptionCodeService)
            .addCode(CreateRedemptionCodeRequest(code = "afk-2024"))
    }

    @Test
    fun `add command works with bot username mention`() {
        Mockito
            .doReturn(RedemptionCodeResponse(id = 2L, code = "AFK-2024", isActive = true))
            .`when`(redemptionCodeService)
            .addCode(CreateRedemptionCodeRequest(code = "afk-2024"))

        handler.handle("/add@MyBot afk-2024")

        Mockito
            .verify(redemptionCodeService)
            .addCode(CreateRedemptionCodeRequest(code = "afk-2024"))
    }

    @Test
    fun `add command reports a duplicate code`() {
        Mockito
            .doThrow(CodeAlreadyExistsException("Redemption code 'AFK-2024' already exists"))
            .`when`(redemptionCodeService)
            .addCode(CreateRedemptionCodeRequest(code = "AFK-2024"))

        val reply = handler.handle("/add AFK-2024")

        assertContains(reply, "Не удалось добавить")
        assertContains(reply, "already exists")
    }

    @Test
    fun `add command without code returns usage hint`() {
        val reply = handler.handle("/add")

        assertContains(reply, "/add <код>")
    }

    // ---------- /activate ----------

    @Test
    fun `activate command activates code without auth code and formats results`() {
        Mockito
            .doReturn(
                CodeActivationResponse(
                    uid = "1",
                    results =
                        listOf(
                            CodeActivationResponse.Result(uid = 10L, name = "HeroA", success = true, message = "ok"),
                            CodeActivationResponse.Result(uid = 11L, name = "HeroB", success = false, message = "err"),
                        ),
                ),
            ).`when`(codeActivationService)
            .activate(ActivateCodeRequest(uid = "1", authCode = null, redemptionCode = "X"))

        val reply = handler.handle("/activate 1 X")

        assertContains(reply, "HeroA (uid = 10): ok")
        assertContains(reply, "HeroB (uid = 11): err")
        Mockito
            .verify(codeActivationService)
            .activate(ActivateCodeRequest(uid = "1", authCode = null, redemptionCode = "X"))
    }

    @Test
    fun `activate command passes the auth code when provided`() {
        Mockito
            .doReturn(CodeActivationResponse(uid = "1", results = emptyList()))
            .`when`(codeActivationService)
            .activate(ActivateCodeRequest(uid = "1", authCode = "auth", redemptionCode = "X"))

        handler.handle("/activate 1 X auth")

        Mockito
            .verify(codeActivationService)
            .activate(ActivateCodeRequest(uid = "1", authCode = "auth", redemptionCode = "X"))
    }

    @Test
    fun `activate command reports an invalid code error`() {
        Mockito
            .doThrow(InvalidCodeException("Redemption code 'X' does not exist"))
            .`when`(codeActivationService)
            .activate(ActivateCodeRequest(uid = "1", authCode = null, redemptionCode = "X"))

        val reply = handler.handle("/activate 1 X")

        assertContains(reply, "Не удалось активировать")
        assertContains(reply, "does not exist")
    }

    @Test
    fun `activate command without enough arguments returns usage hint`() {
        val reply = handler.handle("/activate 1")

        assertContains(reply, "/activate <uid> <код>")
    }

    // ---------- misc ----------

    @Test
    fun `help command returns usage`() {
        val reply = handler.handle("/help")

        assertContains(reply, "Доступные команды")
        assertContains(reply, "/add")
        assertContains(reply, "/activate")
    }

    @Test
    fun `unknown text returns usage`() {
        val reply = handler.handle("hello")

        assertContains(reply, "Доступные команды")
    }

    @Test
    fun `empty text returns usage`() {
        val reply = handler.handle("   ")

        assertContains(reply, "Доступные команды")
    }

    @Test
    fun `no characters on account is reported`() {
        Mockito
            .doReturn(CodeActivationResponse(uid = "1", results = emptyList()))
            .`when`(codeActivationService)
            .activate(ActivateCodeRequest(uid = "1", authCode = null, redemptionCode = "X"))

        val reply = handler.handle("/activate 1 X")

        assertEquals("У аккаунта 1 нет персонажей.", reply)
    }
}
