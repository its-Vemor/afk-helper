package ru.vemor.afkhelper.client

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lilith")
data class LilithProperties(
    val baseUrl: String = "https://cdkey.lilith.com",
    val appId: String = "6241329",
    val game: String = "afkgroup",
    val gmEnvId: String = "dglobal",
    val pupBody: String = "lilith",
)
