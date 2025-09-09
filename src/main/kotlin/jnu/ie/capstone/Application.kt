package jnu.ie.capstone

import jnu.ie.capstone.rtzr.config.RtzrConfig
import jnu.ie.capstone.common.security.config.AllowedOriginsProperties
import jnu.ie.capstone.gemini.config.GeminiConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties(AllowedOriginsProperties::class, GeminiConfig::class, RtzrConfig::class)
class Application
    fun main(args: Array<String>) {
        runApplication<Application>(*args)
    }