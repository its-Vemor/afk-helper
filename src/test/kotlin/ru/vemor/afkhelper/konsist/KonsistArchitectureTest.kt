package ru.vemor.afkhelper.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.ext.list.withAllAnnotationsOf
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import kotlin.test.Test

/**
 * Konsist rules enforcing the architecture and naming conventions described in AGENTS.md.
 */
class KonsistArchitectureTest {
    @Test
    fun `classes annotated with RestController should have suffix Controller`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withAllAnnotationsOf(RestController::class)
            .assertTrue { it.hasNameEndingWith("Controller") }
    }

    @Test
    fun `classes annotated with RestController should reside in controller package`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withAllAnnotationsOf(RestController::class)
            .assertTrue { it.resideInPackage("..controller..") }
    }

    @Test
    fun `classes annotated with RestControllerAdvice should reside in controller package`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withAllAnnotationsOf(RestControllerAdvice::class)
            .assertTrue { it.resideInPackage("..controller..") }
    }

    @Test
    fun `classes annotated with Service should have suffix Service`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withAllAnnotationsOf(Service::class)
            .assertTrue { it.hasNameEndingWith("Service") }
    }

    @Test
    fun `classes annotated with Service should reside in service package`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withAllAnnotationsOf(Service::class)
            .assertTrue { it.resideInPackage("..service..") }
    }

    @Test
    fun `repositories should reside in repository package`() {
        Konsist
            .scopeFromProduction()
            .interfaces()
            .withNameEndingWith("Repository")
            .assertTrue { it.resideInPackage("..repository..") }
    }

    @Test
    fun `request and response classes should reside in dto package`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith("Request", "Response", "Dto", "DTO")
            .assertTrue { it.resideInPackage("..dto..") }
    }

    @Test
    fun `each file should contain at most one top level class named after the file`() {
        Konsist
            .scopeFromProduction()
            .files
            .assertTrue { file ->
                val topLevelClassNames =
                    file
                        .classes()
                        .filter { it.isTopLevel }
                        .map { it.name }
                val fileName = file.name.removeSuffix(".kt")
                topLevelClassNames.size <= 1 && topLevelClassNames.all { it == fileName }
            }
    }

    @Test
    fun `architecture layers dependencies are respected`() {
        Konsist
            .scopeFromProduction()
            .assertArchitecture {
                val controller = Layer("Controller", "ru.vemor.afkhelper.controller..")
                val service = Layer("Service", "ru.vemor.afkhelper.service..")
                val repository = Layer("Repository", "ru.vemor.afkhelper.repository..")
                val domain = Layer("Domain", "ru.vemor.afkhelper.domain..")
                val dto = Layer("Dto", "ru.vemor.afkhelper.dto..")
                val client = Layer("Client", "ru.vemor.afkhelper.client..")

                controller.dependsOn(service, dto, domain)
                service.dependsOn(repository, dto, domain, client)
                repository.dependsOn(domain)
                domain.dependsOnNothing()
                dto.dependsOnNothing()
                client.dependsOnNothing()
            }
    }

    @Test
    fun `test classes should end with Test, Tests or IT suffix`() {
        Konsist
            .scopeFromTest()
            .classes()
            .assertTrue {
                it.hasNameEndingWith("Test") ||
                    it.hasNameEndingWith("Tests") ||
                    it.hasNameEndingWith("IT")
            }
    }
}
