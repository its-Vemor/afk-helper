package ru.vemor.afkhelper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import kotlin.test.assertNotNull

class AfkHelperApplicationTests : AbstractPostgresIT() {
    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Test
    fun `context loads and application context is available`() {
        assertNotNull(applicationContext)
    }
}
