package ru.vemor.afkhelper

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Базовая интеграционная проверка: поднимает общий PostgreSQL (Testcontainers)
 * и настраивает источник данных через динамические свойства.
 *
 * Контейнер стартует один раз на JVM и не останавливается JUnit-расширением между
 * тестовыми классами, поэтому все классы, наследующие эту абстракцию, разделяют
 * один источник данных (порт стабилен на время прогона).
 */
@SpringBootTest(
    properties = [
        // В тестах бот не стартует: не создаются его бины и не идёт обращение к Telegram API.
        "telegram.bot.enabled=false",
        "telegrambots.enabled=false",
    ],
)
@Suppress("UtilityClassWithPublicConstructor")
abstract class AbstractPostgresIT {
    companion object {
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine").apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
