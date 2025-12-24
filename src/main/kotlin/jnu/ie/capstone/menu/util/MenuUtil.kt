package jnu.ie.capstone.menu.util

import com.google.genai.errors.ServerException
import jnu.ie.capstone.gemini.client.GeminiClient
import jnu.ie.capstone.gemini.constant.enums.GeminiModel
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import kotlin.math.sqrt

@Component
class MenuUtil(
    private val geminiClient: GeminiClient
) {
    @Retryable(
        value = [ServerException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0, random = true)
    )
    fun embedVector(text: String, model: GeminiModel): FloatArray {
        return geminiClient
            .getEmbedding(text, model)
            .first()
            .values()
            .get()
            .toFloatArray()
            .normalize()
    }

    private fun FloatArray.normalize(): FloatArray {
        val norm = sqrt(this.sumOf { (it * it).toDouble() }).toFloat()

        if (norm == 0f) {
            return this
        }

        return FloatArray(this.size) { i -> this[i] / norm }
    }
}
