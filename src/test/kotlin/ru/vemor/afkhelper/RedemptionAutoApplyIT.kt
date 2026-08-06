package ru.vemor.afkhelper

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.vemor.afkhelper.client.LilithApi
import ru.vemor.afkhelper.client.RoleInfo
import ru.vemor.afkhelper.domain.Account
import ru.vemor.afkhelper.domain.GameCharacter
import ru.vemor.afkhelper.repository.AccountRepository
import ru.vemor.afkhelper.repository.GameCharacterRepository
import ru.vemor.afkhelper.repository.RedemptionCodeRepository

/**
 * Проверяет фоновое автоприменение: после добавления кода REST-ручка отвечает сразу,
 * а фоновая джоба активирует код на всех известных персонажах.
 */
@AutoConfigureMockMvc
class RedemptionAutoApplyIT : AbstractPostgresIT() {
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

    @BeforeEach
    fun clean() {
        characterRepository.deleteAll()
        accountRepository.deleteAll()
        redemptionCodeRepository.deleteAll()
        Mockito.reset(lilithApi)
    }

    @Test
    fun `adding a code returns immediately and activates it on all stored accounts in background`() {
        val account = accountRepository.save(Account(uid = "1", authToken = "stored-token"))
        val accountId = requireNotNull(account.id)
        characterRepository.saveAll(
            listOf(
                GameCharacter(accountId = accountId, uid = 10L, name = "HeroA", svrId = 1, level = 10, isMain = true),
                GameCharacter(accountId = accountId, uid = 11L, name = "HeroB", svrId = 1, level = 11, isMain = false),
            ),
        )
        Mockito
            .doReturn(
                listOf(
                    RoleInfo(isMain = true, svrId = 1, level = 10, name = "HeroA", uid = 10L),
                    RoleInfo(isMain = false, svrId = 1, level = 11, name = "HeroB", uid = 11L),
                ),
            ).`when`(lilithApi)
            .getRoles("1", "stored-token")
        Mockito
            .doReturn(true)
            .`when`(lilithApi)
            .consume(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString())

        // 1) REST-ответ приходит сразу, без ожидания фоновой активации.
        mockMvc
            .perform(
                post("/api/codes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"code":"new-code"}"""),
            ).andExpect(status().isCreated)

        // 2) Фоновая джоба применяет код на обоих персонажах сохранённого аккаунта.
        Mockito.verify(lilithApi, Mockito.timeout(10_000)).consume(10L, "stored-token", "new-code")
        Mockito.verify(lilithApi, Mockito.timeout(10_000)).consume(11L, "stored-token", "new-code")

        // 3) Токен аккаунта переиспользуется, повторная верификация не выполняется.
        Mockito.verify(lilithApi, Mockito.never()).verifyCode(Mockito.anyString(), Mockito.anyString())
    }
}
