package jnu.ie.capstone.common.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "auth")
data class AllowedOriginsProperties(val allowedFrontEndOrigins: List<String>)