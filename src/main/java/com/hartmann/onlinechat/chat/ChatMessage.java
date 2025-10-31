package com.hartmann.onlinechat.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessage {
    private String content;
    private String sender;
    private MessageType type;
    private String targetSessionId;
}