package ru.vemor.afkhelper.logging

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/** Регистрирует [HttpLoggingInterceptor] для REST-эндпоинтов. */
@Configuration
class WebMvcLoggingConfig(
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(httpLoggingInterceptor).addPathPatterns("/api/**")
    }
}
