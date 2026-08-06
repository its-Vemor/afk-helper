package ru.vemor.afkhelper

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class AfkHelperApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<AfkHelperApplication>(*args)
}
