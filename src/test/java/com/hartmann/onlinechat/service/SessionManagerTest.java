package com.hartmann.onlinechat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class SessionManagerTest {

    private SessionManager sessionManager;
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(org.springframework.messaging.simp.SimpMessagingTemplate.class);
        sessionManager = new SessionManager(messagingTemplate);
    }

    @Test
    void testFirstUserIsAdmin() {
        sessionManager.addSession("s1", "User1");
        assertTrue(sessionManager.isAdmin("s1"));
        assertEquals("s1", sessionManager.getAdminSessionId());
    }

    @Test
    void testSecondUserIsNotAdmin() {
        sessionManager.addSession("s1", "User1");
        sessionManager.addSession("s2", "User2");
        
        assertTrue(sessionManager.isAdmin("s1"));
        assertFalse(sessionManager.isAdmin("s2"));
        assertEquals("s1", sessionManager.getAdminSessionId());
    }

    @Test
    void testAdminReplacementOnDisconnect() {
        sessionManager.addSession("s1", "User1"); // Admin
        sessionManager.addSession("s2", "User2");
        sessionManager.addSession("s3", "User3");

        // Admin disconnects
        sessionManager.removeSession("s1");

        // Current state: s2 should be admin
        assertFalse(sessionManager.isAdmin("s1")); // Disconnected
        assertTrue(sessionManager.isAdmin("s2"));  // New Admin
        assertFalse(sessionManager.isAdmin("s3"));
        assertEquals("s2", sessionManager.getAdminSessionId());
    }

    @Test
    void testAdminReplacementChain() {
        sessionManager.addSession("s1", "User1");
        sessionManager.addSession("s2", "User2");

        sessionManager.removeSession("s1");
        assertTrue(sessionManager.isAdmin("s2"));

        sessionManager.removeSession("s2");
        assertNull(sessionManager.getAdminSessionId());
        assertEquals(0, sessionManager.getConnectedClientCount());
    }
    
    @Test
    void testGetConnectedClientCount() {
        assertEquals(0, sessionManager.getConnectedClientCount());
        sessionManager.addSession("s1", "User1");
        assertEquals(1, sessionManager.getConnectedClientCount());
    }
}
