package ru.vemor.afkhelper.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.dao.DataIntegrityViolationException
import ru.vemor.afkhelper.client.LilithApi
import ru.vemor.afkhelper.domain.Account
import ru.vemor.afkhelper.repository.AccountRepository
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AccountServiceTest {
    private val accountRepository: AccountRepository = Mockito.mock(AccountRepository::class.java)
    private val lilithClient: LilithApi = Mockito.mock(LilithApi::class.java)
    private val service = AccountService(accountRepository, lilithClient)

    @Test
    fun `returns existing account if found`() {
        val account = Account(id = 1L, uid = "123", authToken = "token")
        Mockito.doReturn(account).`when`(accountRepository).findByUid("123")

        val result = service.ensureAccount(" 123 ", "auth")

        assertEquals(account, result)
        Mockito.verify(lilithClient, Mockito.never()).verifyCode(Mockito.anyString(), Mockito.anyString())
        Mockito.verify(accountRepository, Mockito.never()).save(Mockito.any(Account::class.java))
    }

    @Test
    fun `registers a new account successfully`() {
        Mockito.doReturn(null).`when`(accountRepository).findByUid("123")
        Mockito.doReturn("new-token").`when`(lilithClient).verifyCode("123", "auth")
        Mockito
            .doAnswer {
                it.getArgument<Account>(0).copy(id = 2L)
            }.`when`(accountRepository)
            .save(Mockito.any(Account::class.java))

        val result = service.ensureAccount("123", "auth")

        assertEquals("123", result.uid)
        assertEquals("new-token", result.authToken)
        assertEquals(2L, result.id)
    }

    @Test
    fun `blank uid throws InvalidCodeException`() {
        val ex =
            assertFailsWith<InvalidCodeException> {
                service.ensureAccount("   ", "auth")
            }
        assertEquals("uid must not be blank", ex.message)
    }

    @Test
    fun `blank authCode throws InvalidCodeException when account not registered`() {
        Mockito.doReturn(null).`when`(accountRepository).findByUid("123")

        val ex =
            assertFailsWith<InvalidCodeException> {
                service.ensureAccount("123", "   ")
            }
        assertEquals("Auth code is required when the account is not registered yet", ex.message)
    }

    @Test
    fun `null authCode throws InvalidCodeException when account not registered`() {
        Mockito.doReturn(null).`when`(accountRepository).findByUid("123")

        val ex =
            assertFailsWith<InvalidCodeException> {
                service.ensureAccount("123", null)
            }
        assertEquals("Auth code is required when the account is not registered yet", ex.message)
    }

    @Test
    fun `recovers from duplicate key if another thread registers account concurrently`() {
        Mockito.doReturn(null).`when`(accountRepository).findByUid("123")
        Mockito.doReturn("new-token").`when`(lilithClient).verifyCode("123", "auth")
        Mockito
            .doThrow(
                DataIntegrityViolationException("duplicate key"),
            ).`when`(accountRepository)
            .save(Mockito.any(Account::class.java))

        val concurrentAccount = Account(id = 3L, uid = "123", authToken = "concurrent-token")
        // on the second findByUid call, return the concurrently created account
        Mockito.doReturn(null, concurrentAccount).`when`(accountRepository).findByUid("123")

        val result = service.ensureAccount("123", "auth")

        assertEquals(concurrentAccount, result)
        assertEquals("concurrent-token", result.authToken)
    }

    @Test
    fun `rethrows DataIntegrityViolationException if account is still not found after duplicate key error`() {
        Mockito.doReturn(null).`when`(accountRepository).findByUid("123")
        Mockito.doReturn("new-token").`when`(lilithClient).verifyCode("123", "auth")
        Mockito
            .doThrow(
                DataIntegrityViolationException("some other integrity violation"),
            ).`when`(accountRepository)
            .save(Mockito.any(Account::class.java))

        // second call also returns null
        Mockito.doReturn(null, null).`when`(accountRepository).findByUid("123")

        assertFailsWith<DataIntegrityViolationException> {
            service.ensureAccount("123", "auth")
        }
    }
}
