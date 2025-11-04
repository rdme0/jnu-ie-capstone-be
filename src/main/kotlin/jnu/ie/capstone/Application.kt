package jnu.ie.capstone

import jnu.ie.capstone.rtzr.config.RtzrConfig
import jnu.ie.capstone.common.security.config.UriSecurityConfig
import jnu.ie.capstone.gemini.config.GeminiConfig
import jnu.ie.capstone.gemini.config.PromptConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.web.config.EnableSpringDataWebSupport

@SpringBootApplication
@EnableJpaAuditing
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EnableConfigurationProperties(
    UriSecurityConfig::class,
    GeminiConfig::class,
    RtzrConfig::class,
    PromptConfig::class
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}