package com.hartmann.onlinechat.bot.commands;

import com.hartmann.onlinechat.bot.BotCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Bot command implementation that provides session and client information.
 * Returns the client ID (session ID) and client name if available.
 * 
 * @author Thomas Hartmann
 */
// START
@Component
@Slf4j
public class InfoCommand implements BotCommand {
    
    private static final String COMMAND_NAME = "info";
    private static final String USERNAME_ATTRIBUTE = "username";
    private static final String ANONYMOUS_NAME = "anonym";
    
    /**
     * Executes the info command to retrieve session and client information.
     * 
     * @param args Command arguments (not used for info command)
     * @param headerAccessor WebSocket session header accessor
     * @return Formatted string containing client ID and name information
     */
    @Override
    public String execute(String[] args, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Executing info command for session");
        
        // Get session ID (Client-ID)
        String sessionId = headerAccessor.getSessionId();
        log.info("Session ID: {}", sessionId);
        
        // Get client name from session attributes
        String clientName = getClientName(headerAccessor);
        log.info("Client name: {}", clientName);
        
        // Format response according to specification
        String response = formatInfoResponse(sessionId, clientName);
        log.info("Info command response: {}", response);
        
        return response;
    }
    
    /**
     * Returns the command name for this bot function.
     * 
     * @return The command name "info"
     */
    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }
    
    /**
     * Retrieves the client name from session attributes.
     * 
     * @param headerAccessor WebSocket session header accessor
     * @return The client name if available, "anonym" otherwise
     */
    private String getClientName(SimpMessageHeaderAccessor headerAccessor) {
        Object username = headerAccessor.getSessionAttributes().get(USERNAME_ATTRIBUTE);
        log.debug("Username from session: {}", username);
        return username != null ? username.toString() : ANONYMOUS_NAME;
    }
    
    /**
     * Formats the info response according to the specification.
     * 
     * @param sessionId The session ID (Client-ID)
     * @param clientName The client name or "anonym"
     * @return Formatted response string
     */
    private String formatInfoResponse(String sessionId, String clientName) {
        return String.format("Client-ID: %s\nClient-Name: %s", 
                sessionId != null ? sessionId : "unknown", 
                clientName);
    }
}
// END