package com.hartmann.onlinechat.bot.commands;

import com.hartmann.onlinechat.bot.BotCommand;
import com.hartmann.onlinechat.service.SessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Command to display server information.
 * Usage: @server-info
 */
// START
@Component
@RequiredArgsConstructor
public class ServerInfoCommand implements BotCommand {

    private final SessionManager sessionManager;

    @Override
    public String execute(String[] args, SimpMessageHeaderAccessor headerAccessor) {
        StringBuilder sb = new StringBuilder();
        sb.append("Server Information:\n");
        sb.append("-------------------\n");
        sb.append("Connected Clients: ").append(sessionManager.getConnectedClientCount()).append("\n");
        sb.append("Server Uptime:     ").append(sessionManager.getUptime()).append("\n");
        sb.append("Admin User:        ").append(sessionManager.getAdminUsername());

        return sb.toString();
    }

    @Override
    public String getCommandName() {
        return "server-info";
    }
}
// END
