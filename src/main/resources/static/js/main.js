'use strict';

var usernamePage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var connectingElement = document.querySelector('.connecting');

var stompClient = null;
var username = null;
// START - Add session ID tracking
var currentSessionId = null;
// END

var colors = [
    '#5865f2', '#32c787', '#00BCD4', '#ed4245',
    '#faa61a', '#ff85af', '#FF9800', '#39bbb0',
    '#8b5cf6', '#06d6a0', '#f72585', '#4cc9f0'
];

// Theme Toggle Functionality
function initThemeToggle() {
    const themeToggleLogin = document.querySelector('#themeToggle');
    const themeToggleChat = document.querySelector('#themeToggleChat');
    const body = document.body;
    
    // Load saved theme or default to dark mode
    const savedTheme = localStorage.getItem('chatTheme') || 'dark';
    if (savedTheme === 'light') {
        body.classList.remove('dark-mode');
    } else {
        body.classList.add('dark-mode');
    }
    
    function toggleTheme() {
        body.classList.toggle('dark-mode');
        const isDark = body.classList.contains('dark-mode');
        localStorage.setItem('chatTheme', isDark ? 'dark' : 'light');
        
        // Update icons visibility
        updateThemeIcons();
    }
    
    function updateThemeIcons() {
        const sunIcons = document.querySelectorAll('.sun-icon');
        const moonIcons = document.querySelectorAll('.moon-icon');
        const isDark = body.classList.contains('dark-mode');
        
        sunIcons.forEach(icon => {
            icon.classList.toggle('hidden', !isDark);
        });
        
        moonIcons.forEach(icon => {
            icon.classList.toggle('hidden', isDark);
        });
    }
    
    // Initial icon update
    updateThemeIcons();
    
    // Add event listeners
    if (themeToggleLogin) {
        themeToggleLogin.addEventListener('click', toggleTheme);
    }
    
    if (themeToggleChat) {
        themeToggleChat.addEventListener('click', toggleTheme);
    }
}

function connect(event) {
    username = document.querySelector('#name').value.trim();

    if(username) {
        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');

        var socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        stompClient.connect({}, onConnected, onError);
    }
    event.preventDefault();
}

function onConnected() {
    // START - Extract session ID from the connection
    currentSessionId = stompClient.ws._transport.url.split('/')[5]; // Extract from WebSocket URL
    console.log('Connected with session ID:', currentSessionId);
    // END
    
    // Subscribe to the Public Topic
    stompClient.subscribe('/topic/public', onMessageReceived);

    // Tell your username to the server
    stompClient.send("/app/chat.addUser",
        {},
        JSON.stringify({sender: username, type: 'JOIN'})
    );

    connectingElement.classList.add('hidden');
}

function onError(error) {
    connectingElement.innerHTML = '<span style="color: var(--error);">Could not connect to WebSocket server. Please refresh this page to try again!</span>';
}

function sendMessage(event) {
    var messageContent = messageInput.value.trim();
    if(messageContent && stompClient) {
        var chatMessage = {
            sender: username,
            content: messageInput.value,
            type: 'CHAT'
        };
        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
    event.preventDefault();
}

// START - Modified message handling with session filtering
/**
 * Displays a bot message in the chat interface
 * @param {Object} message - The bot message object
 */
function displayBotMessage(message) {
    var messageElement = document.createElement('li');
    messageElement.classList.add('bot-message');

    // Create bot avatar - BLUE BAR WITH "SERVER BOT" TEXT INSIDE
    var avatarElement = document.createElement('div');
    avatarElement.classList.add('avatar', 'bot-avatar');
    avatarElement.textContent = 'Server Bot';            // TEXT INSIDE THE BLUE BAR
    avatarElement.style.backgroundColor = '#5865f2';     // Blue background
    avatarElement.style.color = 'white !important';      // Force white text
    avatarElement.style.fontSize = '12px';               
    avatarElement.style.fontWeight = 'bold';             
    avatarElement.style.textAlign = 'center';            
    avatarElement.style.display = 'flex';                
    avatarElement.style.alignItems = 'center';           
    avatarElement.style.justifyContent = 'center';       
    avatarElement.style.width = '80px';                  // Wider for text to fit
    avatarElement.style.height = '40px';                 // Bar height
    // REMOVED borderRadius to keep it as a rectangular bar

    // Create message content container
    var messageContent = document.createElement('div');
    messageContent.classList.add('message-content');

    // Create message text with monospace font for better readability
    var textElement = document.createElement('pre');
    textElement.classList.add('message-text', 'bot-text');
    textElement.style.fontFamily = 'Monaco, Consolas, "Courier New", monospace';
    textElement.style.backgroundColor = 'rgba(88, 101, 242, 0.1)';
    textElement.style.padding = '10px';
    textElement.style.borderRadius = '6px';
    textElement.style.margin = '8px 0';
    textElement.style.whiteSpace = 'pre-wrap';
    textElement.style.border = '1px solid rgba(88, 101, 242, 0.3)';
    textElement.textContent = message.content;

    // Private message footer - NOTICE + TIMESTAMP (together with -)
    var footerElement = document.createElement('div');
    footerElement.style.fontSize = '11px';
    footerElement.style.color = '#6c757d';
    footerElement.style.fontStyle = 'italic';
    footerElement.style.marginTop = '4px';
    
    // Create timestamp
    var timestamp = new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
    
    // Combine notice and timestamp with - separator
    footerElement.textContent = 'Only you can read this message - ' + timestamp;

    // Assemble the structure
    messageContent.appendChild(textElement);             // Message content first
    messageContent.appendChild(footerElement);           // Footer with notice + timestamp

    messageElement.appendChild(avatarElement);
    messageElement.appendChild(messageContent);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);
    
    // START - Handle bot messages with session filtering
    if (message.type === 'BOT_MESSAGE') {
        console.log('Received bot message with targetSessionId:', message.targetSessionId);
        console.log('Current session ID:', currentSessionId);
        
        // Only display bot messages targeted to this session
        if (message.targetSessionId && message.targetSessionId === currentSessionId) {
            console.log('Bot message is for this session, displaying...');
            displayBotMessage(message);
        } else {
            console.log('Bot message is not for this session, ignoring...');
        }
        return; // Don't process further
    }
    // END
    
    var messageElement = document.createElement('li');

    if(message.type === 'JOIN') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' joined the chat';
        
        var eventContent = document.createElement('p');
        eventContent.textContent = message.content;
        messageElement.appendChild(eventContent);
        
    } else if (message.type === 'LEAVE') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' left the chat';
        
        var eventContent = document.createElement('p');
        eventContent.textContent = message.content;
        messageElement.appendChild(eventContent);
        
    } else {
        messageElement.classList.add('chat-message');

        // Create avatar
        var avatarElement = document.createElement('div');
        avatarElement.classList.add('avatar');
        avatarElement.textContent = message.sender[0].toUpperCase();
        avatarElement.style.backgroundColor = getAvatarColor(message.sender);

        // Create message content container
        var messageContent = document.createElement('div');
        messageContent.classList.add('message-content');

        // Create message header
        var messageHeader = document.createElement('div');
        messageHeader.classList.add('message-header');

        var authorElement = document.createElement('span');
        authorElement.classList.add('message-author');
        authorElement.textContent = message.sender;

        var timestampElement = document.createElement('span');
        timestampElement.classList.add('message-timestamp');
        timestampElement.textContent = new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});

        messageHeader.appendChild(authorElement);
        messageHeader.appendChild(timestampElement);

        // Create message text
        var textElement = document.createElement('div');
        textElement.classList.add('message-text');
        textElement.textContent = message.content;

        messageContent.appendChild(messageHeader);
        messageContent.appendChild(textElement);

        messageElement.appendChild(avatarElement);
        messageElement.appendChild(messageContent);
    }

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

function getAvatarColor(messageSender) {
    var hash = 0;
    for (var i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }
    var index = Math.abs(hash % colors.length);
    return colors[index];
}

// Event Listeners
usernameForm.addEventListener('submit', connect, true);
messageForm.addEventListener('submit', sendMessage, true);

// Add Enter key support for message input
messageInput.addEventListener('keypress', function(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage(event);
    }
});

// Initialize everything when DOM loads
document.addEventListener('DOMContentLoaded', function() {
    // Auto-focus on username input
    document.querySelector('#name').focus();
    
    // Initialize theme toggle
    initThemeToggle();
});