package jnu.ie.capstone

import jnu.ie.capstone.common.security.config.AllowedOriginsProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AllowedOriginsProperties::class)
class Application
    fun main(args: Array<String>) {
        runApplication<Application>(*args)
    }