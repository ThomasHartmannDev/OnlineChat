package com.hartmann.onlinechat.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service responsible for processing bot commands.
 * This service acts as the main entry point for bot functionality,
 * handling command parsing and execution delegation.
 * 
 * @author AI Assistant
 */
// START
@Service
@RequiredArgsConstructor
@Slf4j
public class BotService {
    
    private static final String BOT_PREFIX = "@server";
    private static final String UNKNOWN_COMMAND_RESPONSE = "Unknown command. Available commands: info";
    
    private final BotCommandRegistry commandRegistry;
    
    /**
     * Determines if a message is a bot command by checking the @server prefix.
     * 
     * @param content The message content to check
     * @return true if the message starts with @server, false otherwise
     */
    public boolean isBotCommand(String content) {
        boolean isBot = content != null && content.trim().toLowerCase().startsWith(BOT_PREFIX);
        log.debug("Checking if '{}' is bot command: {}", content, isBot);
        return isBot;
    }
    
    /**
     * Processes a bot command and returns the appropriate response.
     * 
     * @param content The full message content including @server prefix
     * @param headerAccessor WebSocket session header accessor
     * @return The bot's response message
     */
    public String processCommand(String content, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Processing bot command: {}", content);
        
        try {
            String[] parts = parseCommand(content);
            if (parts.length == 0 || parts[0].isEmpty()) {
                return "Invalid command format. Use: @server <command>";
            }
            
            String commandName = parts[0];
            String[] args = extractArgs(parts);
            
            log.info("Command name: '{}', args count: {}", commandName, args.length);
            
            Optional<BotCommand> command = commandRegistry.getCommand(commandName);
            
            if (command.isPresent()) {
                log.info("Executing command: {} with {} args", commandName, args.length);
                String result = command.get().execute(args, headerAccessor);
                log.info("Command execution result: {}", result);
                return result;
            } else {
                log.warn("Unknown bot command: {}", commandName);
                return UNKNOWN_COMMAND_RESPONSE;
            }
            
        } catch (Exception e) {
            log.error("Error processing bot command: {}", content, e);
            return "Error processing command: " + e.getMessage();
        }
    }
    
    /**
     * Parses the command content to extract command name and arguments.
     * 
     * @param content The full message content
     * @return Array of command parts [commandName, arg1, arg2, ...]
     */
    private String[] parseCommand(String content) {
        // Remove @server prefix and split by spaces
        String commandContent = content.trim().substring(BOT_PREFIX.length()).trim();
        log.debug("Command content after removing prefix: '{}'", commandContent);
        
        if (commandContent.isEmpty()) {
            return new String[0];
        }
        
        String[] parts = commandContent.split("\\s+");
        log.debug("Command parts: {}", java.util.Arrays.toString(parts));
        return parts;
    }
    
    /**
     * Extracts arguments from command parts, excluding the command name.
     * 
     * @param parts The full command parts array
     * @return Array containing only the arguments
     */
    private String[] extractArgs(String[] parts) {
        if (parts.length <= 1) {
            return new String[0];
        }
        
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        return args;
    }
}
// END