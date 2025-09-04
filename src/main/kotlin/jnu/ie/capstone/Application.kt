package jnu.ie.capstone

import jnu.ie.capstone.clova.config.ClovaConfig
import jnu.ie.capstone.common.security.config.AllowedOriginsProperties
import jnu.ie.capstone.gemini.config.GeminiConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AllowedOriginsProperties::class, GeminiConfig::class, ClovaConfig::class)
class Application
    fun main(args: Array<String>) {
        runApplication<Application>(*args)
    }