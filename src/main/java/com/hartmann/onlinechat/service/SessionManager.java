package com.hartmann.onlinechat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages active chat sessions and server statistics.
 * Responsible for determining the admin user (first connected) and tracking
 * uptime.
 */
// START
@Service
@Slf4j
public class SessionManager {

    private final CopyOnWriteArrayList<String> sessionOrder = new CopyOnWriteArrayList<>();
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    private final Instant startTime;
    // START
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    // END

    public SessionManager(org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.startTime = Instant.now();
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Registers a new session.
     * The first registered session becomes the admin.
     *
     * @param sessionId The unique session ID
     * @param username  The username associated with the session
     */
    public void addSession(String sessionId, String username) {
        if (!activeSessions.containsKey(sessionId)) {
            activeSessions.put(sessionId, username);
            sessionOrder.add(sessionId);
            log.info("Session added: {}. User: {}. Total sessions: {}", sessionId, username, activeSessions.size());

            if (isAdmin(sessionId)) {
                log.info("Session {} ({}) is now the Admin.", sessionId, username);
                broadcastAdminChange(username);
            }
            // START
            broadcastUserList();
            // END
        }
    }

    /**
     * Removes a session.
     * If the admin disconnects, the next user in line automatically becomes admin.
     *
     * @param sessionId The session ID to remove
     */
    public void removeSession(String sessionId) {
        if (activeSessions.containsKey(sessionId)) {
            boolean wasAdmin = isAdmin(sessionId);
            String previousAdminName = activeSessions.get(sessionId);

            activeSessions.remove(sessionId);
            sessionOrder.remove(sessionId);
            log.info("Session removed: {}. Remaining sessions: {}", sessionId, activeSessions.size());

            if (wasAdmin) {
                if (!sessionOrder.isEmpty()) {
                    String newAdminId = sessionOrder.get(0);
                    String newAdminName = activeSessions.get(newAdminId);
                    log.info("Admin disconnected. New Admin is session {} ({})", newAdminId, newAdminName);
                    broadcastAdminChange(newAdminName);
                } else {
                    log.info("All users disconnected. No Admin.");
                }
            }
            // START
            broadcastUserList();
            // END
        }
    }

    private void broadcastAdminChange(String newAdminName) {
        // Broadcast System Event
        com.hartmann.onlinechat.chat.ChatMessage adminMessage = com.hartmann.onlinechat.chat.ChatMessage.builder()
                .content("System: " + newAdminName + " is now the Server Admin.")
                .sender("System")
                .type(com.hartmann.onlinechat.chat.MessageType.BOT_MESSAGE)
                .build();

        messagingTemplate.convertAndSend("/topic/public", adminMessage);
    }

    // START
    private void broadcastUserList() {
        try {
            java.util.List<String> onlineUsers = new java.util.ArrayList<>(activeSessions.values());
            java.util.Collections.sort(onlineUsers);

            // Create a special message type or just send the list
            // We'll reuse ChatMessage but with a special sender/type if needed,
            // OR we can send a raw map/list if the frontend handles it.
            // Let's stick to ChatMessage for simplicity, or a wrapper.
            // Actually, let's send a standard message but with a special header or JSON
            // body content if we can.
            // Simpler: Map<String, Object> payload

            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", "USER_LIST");
            payload.put("users", onlineUsers);
            // START - Added Admin Info
            payload.put("admin", getAdminUsername());
            // END

            messagingTemplate.convertAndSend("/topic/public", payload);
            log.debug("Broadcasted user list: {}", onlineUsers);
        } catch (Exception e) {
            log.error("Failed to broadcast user list", e);
        }
    }

    /**
     * Retrieves the session ID for a given username.
     * Case-insensitive match.
     * 
     * @param username The username to look up
     * @return The session ID or null if not found
     */
    public String getSessionIdByUsername(String username) {
        if (username == null)
            return null;
        String searchName = username.trim().toLowerCase();

        for (Map.Entry<String, String> entry : activeSessions.entrySet()) {
            if (entry.getValue().toLowerCase().equals(searchName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Returns a list of all online usernames.
     * 
     * @return List of usernames
     */
    public java.util.List<String> getOnlineUsers() {
        return new java.util.ArrayList<>(activeSessions.values());
    }
    // END

    /**
     * Checks if the given session ID belongs to the current Admin.
     *
     * @param sessionId The session ID to check
     * @return true if the session is the Admin, false otherwise
     */
    public boolean isAdmin(String sessionId) {
        return !sessionOrder.isEmpty() && sessionOrder.get(0).equals(sessionId);
    }

    /**
     * Retrieves the session ID of the current Admin.
     *
     * @return The Admin's session ID, or null if no users are connected
     */
    public String getAdminSessionId() {
        return sessionOrder.isEmpty() ? null : sessionOrder.get(0);
    }

    public String getAdminUsername() {
        String adminId = getAdminSessionId();
        return adminId != null ? activeSessions.get(adminId) : "None";
    }

    public int getConnectedClientCount() {
        return activeSessions.size();
    }

    public String getUptime() {
        Duration uptime = Duration.between(startTime, Instant.now());
        long hours = uptime.toHours();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
// END
