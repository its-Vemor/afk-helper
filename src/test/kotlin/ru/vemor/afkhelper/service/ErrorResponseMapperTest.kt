package ru.vemor.afkhelper.service

import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClientException
import ru.vemor.afkhelper.client.LilithApiException
import kotlin.test.assertEquals

class ErrorResponseMapperTest {
    private val mapper = ErrorResponseMapper()

    @Test
    fun `maps domain api exception to its code and message`() {
        val e = InvalidCodeException("Code must not be blank")
        assertEquals("INVALID_CODE", mapper.errorCode(e))
        assertEquals("Code must not be blank", mapper.message(e))
        assertEquals("INVALID_CODE", mapper.toApiError(e).error)
        assertEquals("Code must not be blank", mapper.toApiError(e).message)
    }

    @Test
    fun `maps lilith api exception preserving its code`() {
        val e = LilithApiException("err_x", "boom")
        assertEquals("err_x", mapper.errorCode(e))
        assertEquals("boom", mapper.message(e))
    }

    @Test
    fun `maps lilith api exception without a code to a generic one`() {
        val e = LilithApiException(null, "boom")
        assertEquals("LILITH_API_ERROR", mapper.errorCode(e))
    }

    @Test
    fun `maps a rest client exception to a service unavailable message`() {
        val e = RestClientException("nope")
        assertEquals("LILITH_API_ERROR", mapper.errorCode(e))
        assertEquals("Upstream redemption service is unavailable", mapper.message(e))
    }

    @Test
    fun `maps an unknown exception to an internal error`() {
        val e = IllegalStateException("boom")
        assertEquals("INTERNAL_ERROR", mapper.errorCode(e))
        assertEquals("Internal server error", mapper.message(e))
    }
}
