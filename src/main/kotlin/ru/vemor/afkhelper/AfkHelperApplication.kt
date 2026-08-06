package ru.vemor.afkhelper

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
class AfkHelperApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<AfkHelperApplication>(*args)
}
