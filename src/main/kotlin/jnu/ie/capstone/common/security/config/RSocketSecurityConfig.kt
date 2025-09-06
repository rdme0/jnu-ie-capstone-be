package jnu.ie.capstone.common.security.config

import jnu.ie.capstone.common.security.helper.JwtAuthHelper
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity
import org.springframework.security.config.annotation.rsocket.RSocketSecurity
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Configuration
@EnableRSocketSecurity
@EnableReactiveMethodSecurity
class RSocketSecurityConfig(
    private val helper: JwtAuthHelper
) {

    @Bean
    fun rsocketMessageHandler(): RSocketMessageHandler {
        val handler = RSocketMessageHandler()
        handler.argumentResolverConfigurer.addCustomResolver(AuthenticationPrincipalArgumentResolver())
        return handler
    }

    @Bean
    fun payloadSocketAcceptorInterceptor(security: RSocketSecurity): PayloadSocketAcceptorInterceptor {
        return security
            .authorizePayload { authorize ->
                authorize.setup().authenticated()
                    .anyRequest().authenticated()
            }
            .jwt { jwtSpec ->
                jwtSpec.authenticationManager(createReactiveAuthManager())
            }
            .build()
    }

    private fun createReactiveAuthManager(): ReactiveAuthenticationManager {
        return ReactiveAuthenticationManager { authentication ->
            val bearerToken = authentication.credentials as String
            try {
                Mono.fromCallable { helper.authenticate(bearerToken) }
            } catch (e: Exception) {
                logger.warn(e) {"사용자 인증 중 에러 발생"}
                Mono.error(e)
            }
        }
    }

}