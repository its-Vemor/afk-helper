package ru.vemor.afkhelper.service

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.dao.DataIntegrityViolationException
import ru.vemor.afkhelper.client.LilithApi
import ru.vemor.afkhelper.client.LilithApiException
import ru.vemor.afkhelper.client.RoleInfo
import ru.vemor.afkhelper.domain.Account
import ru.vemor.afkhelper.domain.GameCharacter
import ru.vemor.afkhelper.domain.RedemptionCode
import ru.vemor.afkhelper.dto.ActivateCodeRequest
import ru.vemor.afkhelper.repository.AccountRepository
import ru.vemor.afkhelper.repository.GameCharacterRepository
import ru.vemor.afkhelper.repository.RedemptionCodeRepository
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeActivationServiceTest {
    private val api: LilithApi = Mockito.mock(LilithApi::class.java)
    private val accountRepository: AccountRepository = Mockito.mock(AccountRepository::class.java)
    private val characterRepository: GameCharacterRepository = Mockito.mock(GameCharacterRepository::class.java)
    private val redemptionCodeRepository: RedemptionCodeRepository = Mockito.mock(RedemptionCodeRepository::class.java)
    private val accountService = AccountService(accountRepository, api)
    private val characterSyncService = CharacterSyncService(characterRepository)
    private val service = CodeActivationService(api, accountService, characterSyncService, redemptionCodeRepository)

    private val activeCode = RedemptionCode(id = 1L, code = "redeem", isActive = true)

    private val roleA = RoleInfo(isMain = true, svrId = 340, level = 158, name = "Vemor", uid = 1L)
    private val roleB = RoleInfo(isMain = false, svrId = 143, level = 192, name = "Vemor", uid = 2L)

    private val account = Account(id = 1L, uid = "1", authToken = "stored-token")

    private fun stubActiveCode() {
        Mockito.doReturn(activeCode).`when`(redemptionCodeRepository).findByCode("redeem")
    }

    private fun stubStoredAccount() {
        Mockito.doReturn(account).`when`(accountRepository).findByUid("1")
    }

    private fun stubCharacterLoad(vararg characters: GameCharacter) {
        Mockito
            .doReturn(characters.toList())
            .`when`(characterRepository)
            .findByAccountId(account.id!!)
    }

    private fun stubCharacterSaves() {
        Mockito
            .doAnswer { it.getArgument<GameCharacter>(0) }
            .`when`(characterRepository)
            .save(any(GameCharacter::class.java))
        Mockito
            .doAnswer { inv -> inv.getArgument<Iterable<GameCharacter>>(0).toList() }
            .`when`(characterRepository)
            .saveAll(Mockito.anyIterable())
    }

    @Test
    fun `skips token request and reuses stored account token`() {
        stubActiveCode()
        stubStoredAccount()
        Mockito.doReturn(listOf(roleA, roleB)).`when`(api).getRoles("1", "stored-token")
        Mockito.doReturn(true).`when`(api).consume(1L, "stored-token", "redeem")
        Mockito.doReturn(true).`when`(api).consume(2L, "stored-token", "redeem")
        stubCharacterSaves()

        val response = service.activate(ActivateCodeRequest(uid = "1", authCode = "123", redemptionCode = "redeem"))

        Mockito.verify(api, Mockito.never()).verifyCode(anyString(), anyString())
        assertEquals("1", response.uid)
        assertEquals(2, response.results.size)
        assertTrue(response.results.all { it.success })
    }

    @Test
    fun `obtains token and creates account for unknown uid`() {
        stubActiveCode()
        Mockito.doReturn(null).`when`(accountRepository).findByUid("1")
        Mockito.doReturn("new-token").`when`(api).verifyCode("1", "123")
        Mockito.doReturn(listOf(roleA)).`when`(api).getRoles("1", "new-token")
        Mockito.doReturn(true).`when`(api).consume(1L, "new-token", "redeem")
        Mockito
            .doAnswer { it.getArgument<Account>(0).copy(id = 1L) }
            .`when`(accountRepository)
            .save(any(Account::class.java))
        stubCharacterSaves()

        service.activate(ActivateCodeRequest(uid = "1", authCode = "123", redemptionCode = "redeem"))

        Mockito.verify(api).verifyCode("1", "123")
        Mockito.verify(accountRepository).save(any(Account::class.java))
    }

    @Test
    fun `requires auth code when account is absent`() {
        stubActiveCode()
        Mockito.doReturn(null).`when`(accountRepository).findByUid("1")

        assertFailsWith<InvalidCodeException> {
            service.activate(ActivateCodeRequest(uid = "1", authCode = null, redemptionCode = "redeem"))
        }
    }

    @Test
    fun `collects per character failure and continues with the rest`() {
        stubActiveCode()
        stubStoredAccount()
        Mockito.doReturn(listOf(roleA, roleB)).`when`(api).getRoles("1", "stored-token")
        stubCharacterSaves()
        Mockito
            .doAnswer { inv: org.mockito.invocation.InvocationOnMock ->
                val uid = inv.getArgument<Long>(0)
                if (uid == 2L) {
                    throw LilithApiException("err_cdkey_record_not_found", "err_cdkey_record_not_found")
                }
                true
            }.`when`(api)
            .consume(anyLong(), anyString(), anyString())

        val response = service.activate(ActivateCodeRequest(uid = "1", authCode = null, redemptionCode = "redeem"))

        assertEquals(2, response.results.size)
        assertTrue(response.results[0].success)
        assertFalse(response.results[1].success)
        assertEquals("err_cdkey_record_not_found", response.results[1].message)
    }

    @Test
    fun `reuses an account created by a concurrent request when a duplicate key occurs`() {
        stubActiveCode()
        val existing = Account(id = 5L, uid = "1", authToken = "concurrent-token")
        Mockito
            .doReturn(null, existing)
            .`when`(accountRepository)
            .findByUid("1")
        Mockito.doReturn("fresh-token").`when`(api).verifyCode("1", "auth")
        Mockito
            .doThrow(
                DataIntegrityViolationException("duplicate key value violates unique constraint \"accounts_uid_key\""),
            ).`when`(accountRepository)
            .save(any(Account::class.java))
        Mockito.doReturn(listOf(roleA)).`when`(api).getRoles("1", "concurrent-token")
        Mockito.doReturn(true).`when`(api).consume(1L, "concurrent-token", "redeem")
        stubCharacterSaves()

        val response = service.activate(ActivateCodeRequest(uid = "1", authCode = "auth", redemptionCode = "redeem"))

        assertEquals("1", response.uid)
        Mockito.verify(api).getRoles("1", "concurrent-token")
    }

    @Test
    fun `updates existing and inserts new characters in a single saveAll batch`() {
        stubActiveCode()
        stubStoredAccount()
        Mockito.doReturn(listOf(roleA, roleB)).`when`(api).getRoles("1", "stored-token")
        Mockito.doReturn(true).`when`(api).consume(1L, "stored-token", "redeem")
        Mockito.doReturn(true).`when`(api).consume(2L, "stored-token", "redeem")
        val existing =
            GameCharacter(id = 1L, accountId = 1L, uid = 1L, name = "OldName", svrId = 1, level = 1, isMain = false)
        stubCharacterLoad(existing)
        stubCharacterSaves()

        service.activate(ActivateCodeRequest(uid = "1", authCode = null, redemptionCode = "redeem"))

        Mockito
            .verify(characterRepository)
            .saveAll(
                Mockito.argThat(
                    ArgumentMatcher { chars: Iterable<GameCharacter> ->
                        val list = chars.toList()
                        list.any {
                            it.uid == roleA.uid && it.name == roleA.name && it.level == roleA.level && it.id == 1L
                        } &&
                            list.any { it.uid == roleB.uid && it.id == null }
                    },
                ),
            )
    }

    @Test
    fun `rejects a redemption code that is not stored`() {
        Mockito.doReturn(null).`when`(redemptionCodeRepository).findByCode("redeem")

        assertFailsWith<InvalidCodeException> {
            service.activate(ActivateCodeRequest(uid = "1", authCode = "123", redemptionCode = "redeem"))
        }

        Mockito.verify(api, Mockito.never()).consume(anyLong(), anyString(), anyString())
    }

    @Test
    fun `rejects an inactive redemption code`() {
        Mockito
            .doReturn(RedemptionCode(id = 1L, code = "redeem", isActive = false))
            .`when`(redemptionCodeRepository)
            .findByCode("redeem")

        assertFailsWith<InvalidCodeException> {
            service.activate(ActivateCodeRequest(uid = "1", authCode = "123", redemptionCode = "redeem"))
        }

        Mockito.verify(api, Mockito.never()).consume(anyLong(), anyString(), anyString())
    }

    @Test
    fun `sends the code to lilith preserving its case`() {
        stubActiveCode()
        stubStoredAccount()
        Mockito.doReturn(listOf(roleA)).`when`(api).getRoles("1", "stored-token")
        Mockito.doReturn(true).`when`(api).consume(1L, "stored-token", "redeem")
        stubCharacterSaves()

        service.activate(ActivateCodeRequest(uid = "1", authCode = null, redemptionCode = "  redeem  "))

        Mockito.verify(api).consume(1L, "stored-token", "redeem")
    }
}
