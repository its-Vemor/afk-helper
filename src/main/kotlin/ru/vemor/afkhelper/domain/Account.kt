package ru.vemor.afkhelper.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("accounts")
data class Account(
    @Id
    val id: Long? = null,
    val uid: String,
    val authToken: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
