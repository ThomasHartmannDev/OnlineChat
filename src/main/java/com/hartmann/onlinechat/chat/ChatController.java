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

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatController {
    
    private final BotService botService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor){
        log.info("Received message: " + chatMessage.getContent());
        
        // START - Modified bot command handling with session targeting
        // Check if this is a bot command
        if (botService.isBotCommand(chatMessage.getContent())) {
            log.info("Processing bot command from user: {} - Command: {}", 
                    chatMessage.getSender(), chatMessage.getContent());
            
            try {
                // Process the bot command
                String botResponse = botService.processCommand(chatMessage.getContent(), headerAccessor);
                log.info("Bot response: {}", botResponse);
                
                // Get session ID for targeting
                String sessionId = headerAccessor.getSessionId();
                log.info("Targeting bot response to session: {}", sessionId);
                
                // Create bot response message with target session
                ChatMessage botMessage = ChatMessage.builder()
                    .content(botResponse)
                    .sender("Server Bot")
                    .type(MessageType.BOT_MESSAGE)
                    .targetSessionId(sessionId) // Only this session should display the message
                    .build();
                
                // Send to public topic, but with session targeting
                messagingTemplate.convertAndSend("/topic/public", botMessage);
                
            } catch (Exception e) {
                log.error("Error processing bot command", e);
                
                String sessionId = headerAccessor.getSessionId();
                ChatMessage errorMessage = ChatMessage.builder()
                    .content("Error processing command: " + e.getMessage())
                    .sender("Server Bot")
                    .type(MessageType.BOT_MESSAGE)
                    .targetSessionId(sessionId)
                    .build();
                
                messagingTemplate.convertAndSend("/topic/public", errorMessage);
            }
            
            // Return null to prevent broadcasting the original command
            return null;
        }
        // END
        
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor){
       // Add a username in websocket Session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        return chatMessage;
    }
}