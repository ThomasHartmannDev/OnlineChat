package com.hartmann.onlinechat.service;

import com.hartmann.onlinechat.chat.ChatMessage;
import com.hartmann.onlinechat.chat.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Service responsible for handling Direct Messages (Private Messages) between
 * users.
 * 
 * @author Thomas Hartmann
 */
// START
@Component
@RequiredArgsConstructor
@Slf4j
public class DirectMessageService {

    private final SessionManager sessionManager;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Sends a private message from one user to another.
     * 
     * @param senderUsername    The username of the sender
     * @param senderSessionId   The session ID of the sender (to send confirmation
     *                          back)
     * @param recipientUsername The username of the recipient
     * @param content           The message content
     * @return A status message indicating success or failure
     */
    public String sendPrivateMessage(String senderUsername, String senderSessionId, String recipientUsername,
            String content) {
        log.info("Attempting to send DM from {} to {}", senderUsername, recipientUsername);

        String recipientSessionId = sessionManager.getSessionIdByUsername(recipientUsername);

        if (recipientSessionId == null) {
            log.warn("Recipient {} not found.", recipientUsername);
            return "User '" + recipientUsername + "' not found or offline.";
        }

        // 1. Send to Recipient
        ChatMessage recipientMsg = ChatMessage.builder()
                .content(content)
                .sender(senderUsername) // Sender is the actual user
                .type(MessageType.CHAT) // Standard CHAT type but targeted
                .targetSessionId(recipientSessionId) // PRIVATE TARGETING
                .build();

        messagingTemplate.convertAndSend("/topic/public", recipientMsg);

        // 2. Send Confirmation to Sender (Mirror the message so they see it in their UI
        // too)
        // In a real app we might just append locally, but here we confirm via server
        // We use a special sender name so the UI knows it's a "Sent" message if needed,
        // or just same format

        ChatMessage senderMsg = ChatMessage.builder()
                .content(content)
                .sender(senderUsername)
                .type(MessageType.CHAT)
                .targetSessionId(senderSessionId)
                .build();

        // We can't easily distinguish "Sent" vs "Received" in the simplistic UI unless
        // we add metadata.
        // But the requirement says "Direktnachricht an verbundenen User".
        // The Bonus UI Chatbox will likely handle "local echo" or we send it back.
        // Let's send a confirmation BOT message to the sender so they know it worked,
        // OR rely on the frontend to display the chat.
        // Since we are building a Chatbox UI, the Chatbox will probably want to display
        // the message immediately.
        // But to be safe, let's send a copy back to the sender with
        // targetSessionId=senderSessionId.

        messagingTemplate.convertAndSend("/topic/public", senderMsg);

        return "Private message sent to " + recipientUsername;
    }
}
// END
