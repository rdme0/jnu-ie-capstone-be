package jnu.ie.capstone.common.security.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val allowedOriginsProperties: AllowedOriginsProperties
) {
    companion object {
        private const val CORS_MAX_AGE = 3600L
    }

    @Bean
    fun webSecurityCustomizer(): WebSecurityCustomizer = WebSecurityCustomizer {
        it.ignoring().requestMatchers("/error", "favicon.ico")
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.setAllowedOriginPatterns(
            allowedOriginsProperties.allowedFrontEndOrigins
        )
        configuration.setAllowedMethods(
            mutableListOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        )
        configuration.allowedHeaders = mutableListOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = CORS_MAX_AGE

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun csrfChannelInterceptor() = object : ChannelInterceptor {
        override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> = message
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity) {
        http.headers { it.frameOptions { it -> it.sameOrigin() } }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeHttpRequests {
                it.requestMatchers("/h2-console/**", "/auth/success").permitAll()
                    .anyRequest().authenticated()
            }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .build()
    }

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

}