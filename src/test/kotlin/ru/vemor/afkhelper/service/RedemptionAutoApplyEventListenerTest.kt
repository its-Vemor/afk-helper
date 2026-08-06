package ru.vemor.afkhelper.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.vemor.afkhelper.domain.RedemptionCode

class RedemptionAutoApplyEventListenerTest {
    private val autoApplyService: RedemptionAutoApplyService = Mockito.mock(RedemptionAutoApplyService::class.java)
    private val listener = RedemptionAutoApplyEventListener(autoApplyService)

    @Test
    fun `delegates to the auto apply service on a code added event`() {
        val code = RedemptionCode(id = 1L, code = "REDEEM")

        listener.onCodeAdded(RedemptionCodeAddedEvent(code))

        Mockito.verify(autoApplyService).applyToAllKnownCharacters(code)
    }
}
