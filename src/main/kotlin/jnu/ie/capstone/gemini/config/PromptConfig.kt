package jnu.ie.capstone.gemini.config

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.ResourceLoader
import java.nio.charset.StandardCharsets

@ConfigurationProperties(prefix = "prompt")
data class PromptConfig(
    var kiosk: String
) {

    @Autowired
    lateinit var resourceLoader: ResourceLoader

    @PostConstruct
    fun init() {
        kiosk = loadContent(kiosk)
    }

    private fun loadContent(path: String): String {
        val resource = resourceLoader.getResource(path)
        return resource.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }
}