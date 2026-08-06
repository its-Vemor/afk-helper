package ru.vemor.afkhelper.logging

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Логирует каждый REST-вызов: метод, путь, статус ответа и длительность.
 * При возникновении исключения логирует ошибку.
 */
@Component
class HttpLoggingInterceptor(
    private val apiLogger: ApiLogger,
) : HandlerInterceptor {
    private val startAttribute = HttpLoggingInterceptor::class.java.name + ".start"

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        request.setAttribute(startAttribute, System.nanoTime())
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        val start = request.getAttribute(startAttribute) as? Long ?: return
        val durationMs = (System.nanoTime() - start) / 1_000_000
        val operation = "${request.method} ${request.requestURI}"
        val status = response.status
        if (ex == null) {
            apiLogger.response("rest", operation, status, durationMs)
        } else {
            apiLogger.error("rest", operation, "HTTP_$status", ex.message, ex)
        }
    }
}
