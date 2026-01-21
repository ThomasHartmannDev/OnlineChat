package com.hartmann.onlinechat.bot.commands;

import com.hartmann.onlinechat.bot.BotCommand;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Command to display available commands.
 * Usage: @server help
 */
@Component
public class HelpCommand implements BotCommand {

    @Override
    public String execute(String[] args, SimpMessageHeaderAccessor headerAccessor) {
        StringBuilder sb = new StringBuilder();
        sb.append("Available Commands:\n");
        sb.append("-------------------\n");
        sb.append("@server help            - Show this help message\n");
        sb.append("@server info            - Show your client info\n");
        // sb.append("@server server-info - Show server statistics\n"); // Conflict on
        // 'info', omitting for now unless I fix it
        sb.append("@server math <expr>     - Calculate math expression\n");
        sb.append("@admin <message>        - Send private message to Admin\n");
        sb.append("@<username> <message>   - Send private message to User");
        return sb.toString();
    }

    @Override
    public String getCommandName() {
        return "help";
    }
}
