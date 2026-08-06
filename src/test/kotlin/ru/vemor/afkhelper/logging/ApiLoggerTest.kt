package ru.vemor.afkhelper.logging

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import ru.vemor.afkhelper.config.AppLoggingProperties
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiLoggerTest {
    private val logger: Logger = LoggerFactory.getLogger(ApiLogger::class.java) as Logger

    private fun attach(): ListAppender<ILoggingEvent> {
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)
        return appender
    }

    private fun detach(appender: ListAppender<ILoggingEvent>) {
        logger.detachAppender(appender)
    }

    @Test
    fun `skips request and response logs when disabled`() {
        val appender = attach()
        try {
            val apiLogger = ApiLogger(AppLoggingProperties(enabled = false))
            apiLogger.request("rest", "POST /api/codes", mapOf("code" to "AFK"))
            apiLogger.response("rest", "POST /api/codes", "result", 5L)
            assertEquals(0, appender.list.size)
        } finally {
            detach(appender)
        }
    }

    @Test
    fun `logs request and masks sensitive values when enabled`() {
        val appender = attach()
        try {
            val apiLogger = ApiLogger(AppLoggingProperties(enabled = true))
            apiLogger.request("rest", "POST /api/codes", mapOf("code" to "AFK-001", "authCode" to "top-secret"))
            assertEquals(1, appender.list.size)
            val message = appender.list[0].formattedMessage
            assertTrue(message.contains("POST /api/codes"))
            assertTrue(message.contains("AFK-001"))
            assertTrue(message.contains("[MASKED]"))
            assertFalse(message.contains("top-secret"))
        } finally {
            detach(appender)
        }
    }

    @Test
    fun `logs error even when disabled`() {
        val appender = attach()
        try {
            val apiLogger = ApiLogger(AppLoggingProperties(enabled = false))
            apiLogger.error("rest", "POST /api/codes", "INVALID_CODE", "bad code")
            assertEquals(1, appender.list.size)
            val message = appender.list[0].formattedMessage
            assertTrue(message.contains("INVALID_CODE"))
        } finally {
            detach(appender)
        }
    }

    @Test
    fun `scrub masks token fields but keeps safe values`() {
        val apiLogger = ApiLogger(AppLoggingProperties(enabled = false))
        val scrubbed = apiLogger.scrub(mapOf("uid" to "1", "authToken" to "abc", "redemptionCode" to "AFK"))
        assertEquals("1", scrubbed["uid"])
        assertEquals("[MASKED]", scrubbed["authToken"])
        assertEquals("AFK", scrubbed["redemptionCode"])
    }
}
