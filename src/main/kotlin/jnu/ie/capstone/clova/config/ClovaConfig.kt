package jnu.ie.capstone.clova.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "clova")
data class ClovaConfig(
    val apiKey: String,
    val speechUrl: String
)