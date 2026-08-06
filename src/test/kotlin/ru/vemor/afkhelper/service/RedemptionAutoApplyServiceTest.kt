package ru.vemor.afkhelper.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.vemor.afkhelper.domain.Account
import ru.vemor.afkhelper.domain.RedemptionCode
import ru.vemor.afkhelper.dto.CodeActivationResponse
import ru.vemor.afkhelper.repository.AccountRepository

class RedemptionAutoApplyServiceTest {
    private val accountRepository: AccountRepository = Mockito.mock(AccountRepository::class.java)
    private val codeActivationService: CodeActivationService = Mockito.mock(CodeActivationService::class.java)
    private val service = RedemptionAutoApplyService(accountRepository, codeActivationService)

    private val code = RedemptionCode(id = 1L, code = "REDEEM")

    private fun responseFor(uid: String): CodeActivationResponse =
        CodeActivationResponse(
            uid = uid,
            results = listOf(CodeActivationResponse.Result(uid = 1L, name = "Hero", success = true)),
        )

    @Test
    fun `activates the code for every stored account without an auth code`() {
        val accountA = Account(id = 1L, uid = "1", authToken = "token-a")
        val accountB = Account(id = 2L, uid = "2", authToken = "token-b")
        val page =
            org.springframework.data.domain
                .PageImpl(listOf(accountA, accountB))
        val emptyPage =
            org.springframework.data.domain.Page
                .empty<Account>()
        Mockito
            .doReturn(page, emptyPage)
            .`when`(accountRepository)
            .findAll(Mockito.any(org.springframework.data.domain.Pageable::class.java))
        Mockito.doReturn(responseFor("1")).`when`(codeActivationService).activate("1", "REDEEM")
        Mockito.doReturn(responseFor("2")).`when`(codeActivationService).activate("2", "REDEEM")

        service.applyToAllKnownCharacters(code)

        Mockito.verify(codeActivationService).activate("1", "REDEEM")
        Mockito.verify(codeActivationService).activate("2", "REDEEM")
    }

    @Test
    fun `skips accounts without a stored auth token`() {
        val account = Account(id = 1L, uid = "1", authToken = "")
        val page =
            org.springframework.data.domain
                .PageImpl(listOf(account))
        val emptyPage =
            org.springframework.data.domain.Page
                .empty<Account>()
        Mockito
            .doReturn(page, emptyPage)
            .`when`(accountRepository)
            .findAll(Mockito.any(org.springframework.data.domain.Pageable::class.java))

        service.applyToAllKnownCharacters(code)

        Mockito.verifyNoInteractions(codeActivationService)
    }

    @Test
    fun `continues with the remaining accounts when activation fails for one account`() {
        val accountA = Account(id = 1L, uid = "1", authToken = "token-a")
        val accountB = Account(id = 2L, uid = "2", authToken = "token-b")
        val page =
            org.springframework.data.domain
                .PageImpl(listOf(accountA, accountB))
        val emptyPage =
            org.springframework.data.domain.Page
                .empty<Account>()
        Mockito
            .doReturn(page, emptyPage)
            .`when`(accountRepository)
            .findAll(Mockito.any(org.springframework.data.domain.Pageable::class.java))
        Mockito
            .doThrow(IllegalStateException("upstream down"))
            .`when`(codeActivationService)
            .activate("1", "REDEEM")
        Mockito.doReturn(responseFor("2")).`when`(codeActivationService).activate("2", "REDEEM")

        service.applyToAllKnownCharacters(code)

        Mockito.verify(codeActivationService).activate("1", "REDEEM")
        Mockito.verify(codeActivationService).activate("2", "REDEEM")
    }

    @Test
    fun `is a no-op when there are no stored accounts`() {
        val emptyPage =
            org.springframework.data.domain.Page
                .empty<Account>()
        Mockito
            .doReturn(emptyPage)
            .`when`(accountRepository)
            .findAll(Mockito.any(org.springframework.data.domain.Pageable::class.java))

        service.applyToAllKnownCharacters(code)

        Mockito.verifyNoInteractions(codeActivationService)
    }
}
