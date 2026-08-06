package ru.vemor.afkhelper.client

import com.fasterxml.jackson.annotation.JsonProperty

data class RoleInfo(
    @JsonProperty("is_main") val isMain: Boolean,
    @JsonProperty("svr_id") val svrId: Int,
    val level: Int,
    val name: String,
    val uid: Long,
)
