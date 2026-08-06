package ru.vemor.afkhelper.logging

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class HttpLoggingInterceptorTest {
    private val apiLogger: ApiLogger = Mockito.mock(ApiLogger::class.java)
    private val interceptor = HttpLoggingInterceptor(apiLogger)

    @Test
    fun `reports response with status on completion`() {
        val request = MockHttpServletRequest("POST", "/api/codes")
        val response = MockHttpServletResponse()
        response.status = 201

        interceptor.preHandle(request, response, Any())
        interceptor.afterCompletion(request, response, Any(), null)

        Mockito
            .verify(apiLogger, Mockito.times(1))
            .response(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.eq<Any?>(201),
                Mockito.anyLong(),
            )
    }

    @Test
    fun `reports error when exception occurred on completion`() {
        val request = MockHttpServletRequest("GET", "/api/unknown")
        val response = MockHttpServletResponse()
        response.status = 500
        val boom = IllegalStateException("boom")

        interceptor.preHandle(request, response, Any())
        interceptor.afterCompletion(request, response, Any(), boom)

        Mockito
            .verify(apiLogger, Mockito.times(1))
            .error(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.eq("HTTP_500"),
                Mockito.eq("boom"),
                Mockito.eq(boom),
            )
    }
}
