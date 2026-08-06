package ru.vemor.afkhelper.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

private const val EXECUTOR_CORE_POOL_SIZE = 1
private const val EXECUTOR_MAX_POOL_SIZE = 2
private const val EXECUTOR_QUEUE_CAPACITY = 500
private const val EXECUTOR_AWAIT_TERMINATION_SECONDS = 30

/**
 * Настройка асинхронного исполнения для фоновых задач (автоприменение кодов).
 */
@Configuration
@EnableAsync
class AsyncConfig {
    @Bean(name = ["codeActivationExecutor"])
    fun codeActivationExecutor(): TaskExecutor =
        ThreadPoolTaskExecutor().apply {
            setCorePoolSize(EXECUTOR_CORE_POOL_SIZE)
            setMaxPoolSize(EXECUTOR_MAX_POOL_SIZE)
            setQueueCapacity(EXECUTOR_QUEUE_CAPACITY)
            setThreadNamePrefix("code-activation-")
            // Даём фоновым задачам доработать при остановке приложения.
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(EXECUTOR_AWAIT_TERMINATION_SECONDS)
        }
}
