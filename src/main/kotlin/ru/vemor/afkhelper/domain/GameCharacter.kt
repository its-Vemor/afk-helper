package ru.vemor.afkhelper.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("characters")
data class GameCharacter(
    @Id
    val id: Long? = null,
    val accountId: Long,
    val uid: Long,
    val name: String,
    val svrId: Int,
    val level: Int,
    val isMain: Boolean,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
