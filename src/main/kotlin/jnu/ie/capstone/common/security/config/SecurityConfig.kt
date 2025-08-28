package jnu.ie.capstone.common.security.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val oAuth2UserService:
) {
}