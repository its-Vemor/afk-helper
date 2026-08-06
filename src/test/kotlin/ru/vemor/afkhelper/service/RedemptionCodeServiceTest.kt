package ru.vemor.afkhelper.service

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import ru.vemor.afkhelper.domain.RedemptionCode
import ru.vemor.afkhelper.dto.CreateRedemptionCodeRequest
import ru.vemor.afkhelper.repository.RedemptionCodeRepository
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RedemptionCodeServiceTest {
    private val repository: RedemptionCodeRepository = Mockito.mock(RedemptionCodeRepository::class.java)
    private val eventPublisher: ApplicationEventPublisher = Mockito.mock(ApplicationEventPublisher::class.java)
    private val service = RedemptionCodeService(repository, eventPublisher)

    @Test
    fun `saves a trimmed code preserving its case`() {
        Mockito.doReturn(false).`when`(repository).existsByCode("Afk-Test-001")
        Mockito
            .doAnswer { it.getArgument<RedemptionCode>(0).copy(id = 1L) }
            .`when`(repository)
            .save(Mockito.any(RedemptionCode::class.java))

        val saved = service.addCode(CreateRedemptionCodeRequest(code = "  Afk-Test-001  "))

        assertEquals("Afk-Test-001", saved.code)
        assertEquals(1L, saved.id)
    }

    @Test
    fun `rejects a blank code`() {
        assertFailsWith<InvalidCodeException> {
            service.addCode(CreateRedemptionCodeRequest(code = "   "))
        }
    }

    @Test
    fun `converts a duplicate key violation into CodeAlreadyExistsException`() {
        Mockito.doReturn(false).`when`(repository).existsByCode("AFK-DUP")
        Mockito
            .doThrow(
                DataIntegrityViolationException(
                    "duplicate key value violates unique constraint \"redemption_codes_code_key\"",
                ),
            ).`when`(repository)
            .save(Mockito.any(RedemptionCode::class.java))

        assertFailsWith<CodeAlreadyExistsException> {
            service.addCode(CreateRedemptionCodeRequest(code = "AFK-DUP"))
        }
    }

    @Test
    fun `rethrows integrity violations that are not duplicate keys`() {
        Mockito.doReturn(false).`when`(repository).existsByCode("AFK-LONG")
        Mockito
            .doThrow(DataIntegrityViolationException("value too long for type character varying(255)"))
            .`when`(repository)
            .save(Mockito.any(RedemptionCode::class.java))

        assertFailsWith<DataIntegrityViolationException> {
            service.addCode(CreateRedemptionCodeRequest(code = "AFK-LONG"))
        }
    }

    @Test
    fun `publishes an event for a newly saved code`() {
        Mockito.doReturn(false).`when`(repository).existsByCode("Afk-Test-001")
        val saved = RedemptionCode(id = 1L, code = "Afk-Test-001")
        Mockito
            .doReturn(saved)
            .`when`(repository)
            .save(Mockito.any(RedemptionCode::class.java))

        service.addCode(CreateRedemptionCodeRequest(code = "  Afk-Test-001  "))

        Mockito.verify(eventPublisher).publishEvent(RedemptionCodeAddedEvent(saved))
    }

    @Test
    fun `does not publish an event for a duplicate code`() {
        Mockito.doReturn(true).`when`(repository).existsByCode("AFK-DUP")

        assertFailsWith<CodeAlreadyExistsException> {
            service.addCode(CreateRedemptionCodeRequest(code = "AFK-DUP"))
        }

        Mockito.verify(eventPublisher, Mockito.never()).publishEvent(any(RedemptionCodeAddedEvent::class.java))
    }
}
