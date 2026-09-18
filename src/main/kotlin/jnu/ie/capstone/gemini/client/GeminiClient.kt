package jnu.ie.capstone.gemini.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.genai.Client
import com.google.genai.types.*
import jnu.ie.capstone.common.exception.server.InternalServerException
import jnu.ie.capstone.gemini.config.GeminiConfig
import jnu.ie.capstone.gemini.constant.enums.GeminiModel
import org.springframework.stereotype.Component


@Component
class GeminiClient(
    private val mapper: ObjectMapper,
    config: GeminiConfig
) {
    private val client = Client.builder().apiKey(config.apiKey).build()

    fun getEmbedding(
        text: String,
        model: GeminiModel = GeminiModel.GEMINI_EMBEDDING_001
    ): List<ContentEmbedding> {
        val config = EmbedContentConfig.builder()
            .taskType("SEMANTIC_SIMILARITY")
            .outputDimensionality(768)
            .build()

        val response = client.models.embedContent(model.toString(), text, config)
        val embeddings = response.embeddings()

        return embeddings.orElse(null)
            ?: throw InternalServerException(IllegalStateException("Gemini 응답이 비었습니다."))
    }

}