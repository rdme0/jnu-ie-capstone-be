package jnu.ie.capstone.session.controller

import jdk.internal.org.jline.utils.Colors.s
import jnu.ie.capstone.session.service.KioskAiSessionService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestBody

@Controller
@MessageMapping("/order")
class KioskAiSessionController(
    private val service: KioskAiSessionService
) {

    @MessageMapping("/session")
    suspend fun receiveOrderVoice(@RequestBody ) {

    }

}