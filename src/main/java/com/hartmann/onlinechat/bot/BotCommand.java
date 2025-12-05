package com.hartmann.onlinechat.bot;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

/**
 * Common interface for all bot command implementations.
 * This interface ensures that future bot commands can be added without modifying
 * the main bot call mechanism, following the Open/Closed Principle.
 * 
 * @author Thomas Hartmann
 */
// START
public interface BotCommand {
    
    /**
     * Executes the bot command with the given context and arguments.
     * 
     * @param args The command arguments (excluding the command name itself)
     * @param headerAccessor WebSocket session header accessor for retrieving session information
     * @return The response message to be sent back to the requesting session
     */
    String execute(String[] args, SimpMessageHeaderAccessor headerAccessor);
    
    /**
     * Returns the unique command name that triggers this bot function.
     * 
     * @return The command name (e.g., "info", "help", etc.)
     */
    String getCommandName();
}
// END