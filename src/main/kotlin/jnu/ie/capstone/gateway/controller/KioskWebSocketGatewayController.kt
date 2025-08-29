package jnu.ie.capstone.gateway.controller

import jnu.ie.capstone.gateway.service.KioskWebSocketGatewayService
import org.springframework.stereotype.Controller

@Controller
class KioskWebSocketGatewayController(
    private val service: KioskWebSocketGatewayService
) {

}