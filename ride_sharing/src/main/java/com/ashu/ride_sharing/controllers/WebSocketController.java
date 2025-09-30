package com.ashu.ride_sharing.controllers;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @MessageMapping("/notifications/subscribe")
    @SendTo("/topic/notifications")
    public String handleSubscription(String message) {
        return "Subscribed to notifications";
    }
}
