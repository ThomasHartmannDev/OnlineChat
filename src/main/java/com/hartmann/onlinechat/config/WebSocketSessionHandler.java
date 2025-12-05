package com.hartmann.onlinechat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

/**
 * WebSocket session handler to manage user sessions and provide better logging.
 * 
 * @author Thomas Hartmann
 */
// START
@Component
@Slf4j
public class WebSocketSessionHandler {

    /**
     * Handles new WebSocket session connections.
     * 
     * @param event The session connected event
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.info("New WebSocket session connected: {}", sessionId);
        log.debug("Session details - ID: {}, User: {}", 
                sessionId, 
                headerAccessor.getUser());
    }
}
// END