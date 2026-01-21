'use strict';

// --- State Variables ---
var usernamePage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var connectingElement = document.querySelector('.connecting');
var usersListElement = document.querySelector('#usersList');
var onlineCountElement = document.querySelector('#online-count');
var chatboxesContainer = document.querySelector('#chatboxes-container');

var stompClient = null;
var username = null;
var currentSessionId = null;
var currentAdminUsername = null;

var colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

var openPrivateChats = new Set(); // Stores usernames of open chatboxes

// --- Initialization ---

function init() {
    // Theme logic
    const themeBtn = document.getElementById('theme-toggle-btn');
    const savedTheme = localStorage.getItem('theme') || 'dark';
    document.documentElement.setAttribute('data-theme', savedTheme);

    themeBtn.addEventListener('click', () => {
        const current = document.documentElement.getAttribute('data-theme');
        const next = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
        localStorage.setItem('theme', next);
    });

    // Mobile Sidebar
    document.getElementById('sidebar-toggle').addEventListener('click', () => {
        document.getElementById('sidebar').classList.toggle('active');
    });

    // Forms
    usernameForm.addEventListener('submit', connect, true);
    messageForm.addEventListener('submit', sendMessage, true);
}

// --- WebSocket Connection ---

function connect(event) {
    username = document.querySelector('#name').value.trim();

    if (username) {
        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');

        var socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        // stompClient.debug = null; // Disable debug logs in console

        stompClient.connect({}, onConnected, onError);
    }
    event.preventDefault();
}

function onConnected() {
    // Determine Session ID from transport URL
    // Format: .../ws/<server>/<session>/websocket
    var url = stompClient.ws._transport.url;
    // This is a common hack for SockJS to get the session ID client-side if the server doesn't send it explicitly
    // standard sockjs url structure: base_url + /server_id/session_id/transport
    var parts = url.split('/');
    // usually parts[parts.length - 2] is session id
    currentSessionId = parts[parts.length - 2];
    console.log("My Session ID: " + currentSessionId);

    // Subscribe to Public Topic
    stompClient.subscribe('/topic/public', onMessageReceived);

    // Tell your username to the server
    stompClient.send("/app/chat.addUser",
        {},
        JSON.stringify({ sender: username, type: 'JOIN' })
    );

    // connectingElement.classList.add('hidden'); // Not used in new UI logic explicitly but good to have
}

function onError(error) {
    connectingElement.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
    connectingElement.style.color = 'red';
    connectingElement.classList.remove('hidden');
    usernamePage.classList.remove('hidden'); // Show login again
    chatPage.classList.add('hidden');
}

// --- Message Sending ---

function sendMessage(event) {
    event.preventDefault();
    var messageContent = messageInput.value.trim();
    if (messageContent && stompClient) {
        var chatMessage = {
            sender: username,
            content: messageInput.value,
            type: 'CHAT'
        };

        // Optimistic UI for DM could go here, but strict server echo is safer to ensure it arrived.
        // HOWEVER, the Guide says: for DMs, we assume server does NOT broadcast to public.
        // So we MUST handle optimistic UI for DMs if we want to see what we sent.

        // Check if DM
        if (messageContent.startsWith('@') && !messageContent.startsWith('@server')) {
            // It's a DM or invalid
            // Parse recipient
            let parts = messageContent.split(/\s+/, 2);
            let recipient = parts[0].substring(1);

            // Handle @admin alias
            if (recipient.toLowerCase() === 'admin') {
                if (currentAdminUsername) {
                    recipient = currentAdminUsername;
                    // Reconstruct message content for protocol (standard @User format)
                    let msgBody = parts.length > 1 ? messageContent.substring(parts[0].length) : "";
                    chatMessage.content = "@" + recipient + msgBody;
                } else {
                    var systemMessage = {
                        sender: 'System',
                        content: "No admin is currently online.",
                        type: 'BOT_MESSAGE'
                    };
                    displayMainChatMessage(systemMessage);
                    messageInput.value = '';
                    return;
                }
            }

            // Check if user exists
            if (!document.getElementById('user-item-' + recipient)) {
                var systemMessage = {
                    sender: 'System',
                    content: "User '" + recipient + "' not found.",
                    type: 'BOT_MESSAGE'
                };
                displayMainChatMessage(systemMessage);

                messageInput.value = '';
                return;
            }

            // Open Chatbox locally
            openChatbox(recipient);
            // Add message to chatbox
            addMessageToChatbox(recipient, parts.length > 1 ? messageContent.substring(parts[0].length).trim() : "", 'sent');

            // Send to server
            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        } else {
            // Public message or Bot command
            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        }

        messageInput.value = '';
    }
}

/**
 * Handle Private Message Sending from Chatbox Input
 */
function sendPrivateMessage(recipient, inputElement) {
    var content = inputElement.value.trim();
    if (content && stompClient) {
        // Construct the @User msg format
        var fullContent = "@" + recipient + " " + content;

        var chatMessage = {
            sender: username,
            content: fullContent,
            type: 'CHAT'
        };

        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));

        // Optimistic append
        addMessageToChatbox(recipient, content, 'sent');
        inputElement.value = '';
    }
}

// --- Message Receiving ---

function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);

    // 1. Logic: Target Session Filter
    if (message.targetSessionId) {
        // If message has a target, check if it is for ME.
        // Note: The session ID extraction hack (currentSessionId) must match what server thinks.
        // If this fails, DMs might be ignored. 
        // fallback: if we can't verify session ID, we might have issues.

        if (message.targetSessionId !== currentSessionId) {
            return; // Ignore message for others
        }

        // It IS for me.

        // Case A: Bot Message (Private reply)
        if (message.type === 'BOT_MESSAGE') {
            // Show in Main Chat (as per guide requirement "Display in Main Chat")
            // or check where user expected it. 
            // Guide says: "Bot/System: Display in Main Chat."
            displayMainChatMessage(message);
            return;
        }

        // Case B: Join Rejection
        if (message.type === 'JOIN_REJECTED') {
            alert(message.content);
            window.location.reload();
            return;
        }

        // Case C: Private Message from another User
        if (message.type === 'CHAT' && message.sender !== username) {
            openChatbox(message.sender);
            addMessageToChatbox(message.sender, message.content, 'received');
            return;
        }
        // Case D: Echo from Self (Confirmation)
        if (message.sender === username) {
            return; // Ignore echo
        }

        return; // Prevent fall-through for any other targeted messages
    }

    // 2. Public Messages (No targetSessionId)
    // AND Bot messages (even if targeted, if logic above didn't catch specific types)

    if (message.type === 'JOIN') {
        message.content = message.sender + ' joined!';
        displayEventMessage(message);
        // In a real app, we'd fetch the user list from an API here or via WebSocket.
        // For this demo, let's just add them to the list locally if possible?
        // Wait, backend doesn't send USER_LIST automatically on join in standard spring chat demo.
        // We might need to implement that if we want a real sidebar. 
        // For now, we will just add this user to sidebar.
        updateUserList(message.sender, 'add');
    }
    else if (message.type === 'LEAVE') {
        message.content = message.sender + ' left!';
        displayEventMessage(message);
        updateUserList(message.sender, 'remove');
    }
    else if (message.type === 'USER_LIST') {
        refreshUserList(message.users, message.admin);
    }
    else {
        // Normal Public Chat
        displayMainChatMessage(message);
    }
}

// --- UI Rendering Helpers ---

function displayMainChatMessage(message) {
    var messageElement = document.createElement('li');
    messageElement.classList.add('chat-message');

    if (message.type === 'BOT_MESSAGE') {
        messageElement.classList.add('bot');
    }

    // Avatar
    var avatarElement = document.createElement('div');
    avatarElement.classList.add('message-avatar');
    avatarElement.style.backgroundColor = getAvatarColor(message.sender);

    var avatarText = document.createTextNode(message.sender[0].toUpperCase());
    avatarElement.appendChild(avatarText);

    // Content Wrapper
    var contentElement = document.createElement('div');
    contentElement.classList.add('message-content');

    // Header (Sender + Time)
    var headerElement = document.createElement('div');
    headerElement.classList.add('message-header');

    var senderElement = document.createElement('span');
    senderElement.classList.add('message-sender');
    senderElement.innerText = message.sender;

    if (message.type === 'BOT_MESSAGE') {
        var botTag = document.createElement('span');
        botTag.classList.add('bot-tag');
        botTag.innerText = (message.sender === 'System') ? 'SYSTEM' : 'BOT';
        senderElement.appendChild(botTag);
    }

    var timeElement = document.createElement('span');
    timeElement.classList.add('message-time');
    var date = new Date();
    timeElement.innerText = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    headerElement.appendChild(senderElement);
    headerElement.appendChild(timeElement);

    // Text
    var textElement = document.createElement('p');
    textElement.classList.add('message-text');
    var messageText = document.createTextNode(message.content);
    textElement.appendChild(messageText);

    contentElement.appendChild(headerElement);
    contentElement.appendChild(textElement);

    messageElement.appendChild(avatarElement);
    messageElement.appendChild(contentElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

function displayEventMessage(message) {
    var li = document.createElement('li');
    li.classList.add('event-message');
    li.innerText = message.content;
    messageArea.appendChild(li);
    messageArea.scrollTop = messageArea.scrollHeight;
}

// --- Sidebar Logic ---

// Note: A true robust user list requires server-side state sync (sending list on connect).
// This is a simplified version that adds/removes based on events initiated AFTER I join.
function updateUserList(user, action) {
    // Check if exists
    let existing = document.getElementById('user-item-' + user);

    if (action === 'add') {
        if (!existing) {
            var li = document.createElement('li');
            li.id = 'user-item-' + user;
            li.classList.add('user-item');
            li.onclick = function () { openChatbox(user); };

            var avatar = document.createElement('div');
            avatar.classList.add('user-avatar');
            avatar.style.backgroundColor = getAvatarColor(user);
            avatar.innerText = user[0].toUpperCase();

            var dot = document.createElement('div');
            dot.classList.add('status-dot');
            avatar.appendChild(dot);

            var nameSpan = document.createElement('span');
            nameSpan.classList.add('user-name');
            if (user === username) nameSpan.classList.add('self');
            nameSpan.innerText = user + (user === username ? ' (You)' : '');

            li.appendChild(avatar);
            li.appendChild(nameSpan);

            usersListElement.appendChild(li);
        }
    } else if (action === 'remove') {
        if (existing) {
            existing.remove();
        }
    }

    // Update count
    onlineCountElement.innerText = usersListElement.children.length;
}

function refreshUserList(users, adminName) {
    usersListElement.innerHTML = '';
    currentAdminUsername = adminName;

    // Ensure "Self" is in the list or handled?
    // The list from server includes self.

    users.forEach(user => {
        const isAdmin = (user === adminName);
        updateUserList(user, 'add', isAdmin);
    });
}


// --- Private Chatbox Logic ---

// ... existing code ...

// Note: We need to modify updateUserList to accept isAdmin
function updateUserList(user, action, isAdmin = false) {
    // Check if exists
    let existing = document.getElementById('user-item-' + user);

    if (action === 'add') {
        if (!existing) {
            var li = document.createElement('li');
            li.id = 'user-item-' + user;
            li.classList.add('user-item');
            li.onclick = function () { openChatbox(user); };

            var avatar = document.createElement('div');
            avatar.classList.add('user-avatar');
            avatar.style.backgroundColor = getAvatarColor(user);
            avatar.innerText = user[0].toUpperCase();

            var dot = document.createElement('div');
            dot.classList.add('status-dot');
            avatar.appendChild(dot);

            var nameSpan = document.createElement('span');
            nameSpan.classList.add('user-name');
            if (user === username) nameSpan.classList.add('self');
            nameSpan.innerText = user + (user === username ? ' (You)' : '');

            if (isAdmin) {
                var adminTag = document.createElement('span');
                adminTag.classList.add('admin-tag');
                adminTag.innerText = 'ADMIN';
                nameSpan.appendChild(adminTag);
            }

            li.appendChild(avatar);
            li.appendChild(nameSpan);

            usersListElement.appendChild(li);
        }
    } else if (action === 'remove') {
        if (existing) {
            existing.remove();
        }
    }

    // Update count
    onlineCountElement.innerText = usersListElement.children.length;
}


// --- Private Chatbox Logic ---

function openChatbox(otherUser) {
    if (otherUser === username) return; // Can't chat with self
    if (openPrivateChats.has(otherUser)) return; // Already open

    openPrivateChats.add(otherUser);

    var chatbox = document.createElement('div');
    chatbox.classList.add('chatbox');
    chatbox.id = 'chatbox-' + otherUser;

    // Header
    var header = document.createElement('div');
    header.classList.add('chatbox-header');

    var title = document.createElement('span');
    title.innerText = otherUser;

    var controls = document.createElement('div');
    controls.classList.add('chatbox-controls');

    // Minimize Button
    var minBtn = document.createElement('span');
    minBtn.classList.add('minimize-btn');
    minBtn.innerText = '−'; // Minus sign
    minBtn.onclick = function (e) {
        e.stopPropagation();
        chatbox.classList.toggle('minimized');
    };

    // Close Button
    var closeBtn = document.createElement('span');
    closeBtn.classList.add('close-btn');
    closeBtn.innerText = '×'; // Multiplication sign
    closeBtn.onclick = function (e) {
        e.stopPropagation();
        chatbox.remove();
        openPrivateChats.delete(otherUser);
    };

    controls.appendChild(minBtn);
    controls.appendChild(closeBtn);

    header.appendChild(title);
    header.appendChild(controls);

    // Allow clicking the header bar generally to minimize too (optional, but good UX)
    header.onclick = function () {
        chatbox.classList.toggle('minimized');
    };

    // Body
    var body = document.createElement('div');
    body.classList.add('chatbox-body');
    body.id = 'chatbox-body-' + otherUser;

    // Footer
    var footer = document.createElement('div');
    footer.classList.add('chatbox-footer');

    var input = document.createElement('input');
    input.type = 'text';
    input.placeholder = 'Message...';
    input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            sendPrivateMessage(otherUser, input);
        }
    });

    footer.appendChild(input);
    chatbox.appendChild(header);
    chatbox.appendChild(body);
    chatbox.appendChild(footer);

    chatboxesContainer.appendChild(chatbox);
}

function addMessageToChatbox(otherUser, content, type) {
    // type is 'sent' or 'received'
    var body = document.getElementById('chatbox-body-' + otherUser);
    if (!body) return; // Should not happen if openChatbox called first

    var msgDiv = document.createElement('div');
    msgDiv.classList.add('cb-msg');
    msgDiv.classList.add(type);
    msgDiv.innerText = content;

    body.appendChild(msgDiv);
    body.scrollTop = body.scrollHeight;
}


// --- Utilities ---

function getAvatarColor(messageSender) {
    var hash = 0;
    for (var i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }
    var index = Math.abs(hash % colors.length);
    return colors[index];
}

// Start
init();
