package ru.vemor.afkhelper.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("redemption_codes")
data class RedemptionCode(
    @Id
    val id: Long? = null,
    val code: String,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
