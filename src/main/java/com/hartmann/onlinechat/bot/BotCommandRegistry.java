package com.hartmann.onlinechat.bot;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry that manages all available bot commands.
 * This component automatically discovers all BotCommand implementations
 * and provides a mechanism to resolve commands by name.
 * 
 * @author Thomas Hartmann
 */
// START
@Component
@RequiredArgsConstructor
@Slf4j
public class BotCommandRegistry {
    
    private final List<BotCommand> botCommands;
    private final Map<String, BotCommand> commandMap = new HashMap<>();
    
    /**
     * Initializes the command registry by mapping command names to their implementations.
     */
    @PostConstruct
    public void initializeCommands() {
        botCommands.forEach(command -> {
            String commandName = command.getCommandName().toLowerCase();
            commandMap.put(commandName, command);
            log.info("Registered bot command: {}", commandName);
        });
        log.info("Bot command registry initialized with {} commands", commandMap.size());
    }
    
    /**
     * Retrieves a bot command by its name.
     * 
     * @param commandName The name of the command to retrieve
     * @return Optional containing the command if found, empty otherwise
     */
    public Optional<BotCommand> getCommand(String commandName) {
        return Optional.ofNullable(commandMap.get(commandName.toLowerCase()));
    }
    
    /**
     * Checks if a command with the given name exists.
     * 
     * @param commandName The name of the command to check
     * @return true if the command exists, false otherwise
     */
    public boolean hasCommand(String commandName) {
        return commandMap.containsKey(commandName.toLowerCase());
    }
}
// END