package ru.vemor.afkhelper.controller

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import ru.vemor.afkhelper.logging.ApiLogger
import ru.vemor.afkhelper.service.ErrorResponseMapper
import kotlin.test.assertEquals

class ApiExceptionHandlerTest {

    private val apiLogger = Mockito.mock(ApiLogger::class.java)
    private val errorResponseMapper = Mockito.mock(ErrorResponseMapper::class.java)
    private val handler = ApiExceptionHandler(apiLogger, errorResponseMapper)

    @Test
    fun `handleValidation returns error message from field error`() {
        val request = Mockito.mock(HttpServletRequest::class.java)
        val exception = Mockito.mock(MethodArgumentNotValidException::class.java)
        val bindingResult = Mockito.mock(BindingResult::class.java)
        val fieldError = FieldError("objectName", "field", "Custom validation error")

        Mockito.`when`(request.method).thenReturn("POST")
        Mockito.`when`(request.requestURI).thenReturn("/api/test")
        Mockito.`when`(exception.bindingResult).thenReturn(bindingResult)
        Mockito.`when`(bindingResult.fieldError).thenReturn(fieldError)

        val response = handler.handleValidation(exception, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("INVALID_REQUEST", response.body?.error)
        assertEquals("Custom validation error", response.body?.message)

        Mockito.verify(apiLogger).error("rest", "POST /api/test", "INVALID_REQUEST", "Custom validation error", null)
    }

    @Test
    fun `handleValidation returns default error message when field error has no message`() {
        val request = Mockito.mock(HttpServletRequest::class.java)
        val exception = Mockito.mock(MethodArgumentNotValidException::class.java)
        val bindingResult = Mockito.mock(BindingResult::class.java)

        Mockito.`when`(request.method).thenReturn("POST")
        Mockito.`when`(request.requestURI).thenReturn("/api/test")
        Mockito.`when`(exception.bindingResult).thenReturn(bindingResult)
        Mockito.`when`(bindingResult.fieldError).thenReturn(null)

        val response = handler.handleValidation(exception, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("INVALID_REQUEST", response.body?.error)
        assertEquals("Request validation failed", response.body?.message)

        Mockito.verify(apiLogger).error("rest", "POST /api/test", "INVALID_REQUEST", "Request validation failed", null)
    }
}
