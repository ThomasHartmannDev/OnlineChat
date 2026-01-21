package com.hartmann.onlinechat.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.hartmann.onlinechat.bot.BotService;
import lombok.RequiredArgsConstructor;

/**
 * Controller for handling WebSocket chat messages.
 * 
 * @author Thomas Hartmann
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final BotService botService;
    private final SimpMessagingTemplate messagingTemplate;
    // START
    private final com.hartmann.onlinechat.service.SessionManager sessionManager;
    private final com.hartmann.onlinechat.service.DirectMessageService directMessageService;
    // END

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Received message: " + chatMessage.getContent());

        String content = chatMessage.getContent();

        // 1. Strict Handling for any message starting with "@"
        if (content != null && content.trim().startsWith("@")) {

            // A) Bot Command (@server ...)
            if (botService.isBotCommand(content)) {
                handleBotCommand(content, chatMessage.getSender(), headerAccessor);
                return null; // Suppress broadcast
            }

            // B) Direct Message (@User ...)
            // Logic: It starts with @, and is NOT a bot command. Must be DM.
            handleDirectMessage(content, chatMessage.getSender(), headerAccessor);
            return null; // Suppress broadcast
        }

        // 2. Regular Public Message
        return chatMessage;
    }

    // START - Helper: Handle DM
    private void handleDirectMessage(String content, String sender, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Processing Direct Message from user: {}", sender);

        String[] parts = content.trim().split("\\s+", 2);
        if (parts.length > 0) {
            String recipientUsername = parts[0].substring(1); // Remove @
            String messageContent = parts.length > 1 ? parts[1] : "";

            String senderSessionId = headerAccessor.getSessionId();

            directMessageService.sendPrivateMessage(
                    sender,
                    senderSessionId,
                    recipientUsername,
                    messageContent);
        }
    }
    // END

    // START - Helper: Handle Bot
    private void handleBotCommand(String content, String sender, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Processing bot command from user: {} - Command: {}", sender, content);
        try {
            String botResponse = botService.processCommand(content, headerAccessor);
            String sessionId = headerAccessor.getSessionId();

            ChatMessage botMessage = ChatMessage.builder()
                    .content(botResponse)
                    .sender("Server Bot")
                    .type(MessageType.BOT_MESSAGE)
                    .targetSessionId(sessionId)
                    .build();

            messagingTemplate.convertAndSend("/topic/public", botMessage);
        } catch (Exception e) {
            log.error("Error processing bot command", e);
        }
    }
    // END

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String username = chatMessage.getSender();
        String sessionId = headerAccessor.getSessionId();

        // START - Username Validation
        // 1. Check for Empty/Null
        if (username == null || username.trim().isEmpty()) {
            log.warn("Rejected empty username from session: {}", sessionId);
            // Send rejection to this session only
            sendRejection(sessionId, "Username cannot be empty.");
            return null; // Prevent broadcast
        }

        // 2. Check for Duplicates
        // We need a way to check strictly. sessionManager.getSessionIdByUsername works.
        if (sessionManager.getSessionIdByUsername(username) != null) {
            log.warn("Rejected duplicate username '{}' from session: {}", username, sessionId);
            sendRejection(sessionId, "Username '" + username + "' is already taken.");
            return null; // Prevent broadcast
        }
        // END

        // Add a username in websocket Session
        headerAccessor.getSessionAttributes().put("username", username);

        // START
        sessionManager.addSession(sessionId, username);
        // END

        return chatMessage;
    }

    // START - Helper for sending Rejection
    private void sendRejection(String sessionId, String reason) {
        ChatMessage rejection = ChatMessage.builder()
                .content(reason)
                .sender("System")
                .type(MessageType.JOIN_REJECTED)
                .targetSessionId(sessionId)
                .build();

        messagingTemplate.convertAndSend("/topic/public", rejection);
    }
    // END

    // START
    @org.springframework.context.event.EventListener
    public void handleWebSocketDisconnectListener(
            org.springframework.web.socket.messaging.SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        sessionManager.removeSession(sessionId);
    }
    // END
}