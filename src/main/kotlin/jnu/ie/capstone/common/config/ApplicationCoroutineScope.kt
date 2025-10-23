package jnu.ie.capstone.common.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.springframework.stereotype.Component
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineExceptionHandler
import mu.KotlinLogging

@Component
class ApplicationCoroutineScope : CoroutineScope {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logger.error(throwable) { "애플리케이션 스코프에서 처리되지 않은 예외 발생" }
    }

    override val coroutineContext = SupervisorJob() + Dispatchers.IO + exceptionHandler

    @PreDestroy
    fun cancelScope() {
        this.cancel()
    }
}