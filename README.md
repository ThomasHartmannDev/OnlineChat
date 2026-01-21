# OnlineChat Application

A modern, real-time chat application built with Spring Boot (Backend) and Vanilla JS/CSS (Frontend). It features a premium "Dark Mode" aesthetic inspired by Discord and Facebook, supporting public chat, private messaging, and bot interactions.

## Features

### Core Features
*   **Real-time Messaging**: Powered by WebSockets (STOMP over SockJS).
*   **Public Chat**: Global `#general` channel for all connected users.
*   **Private Messaging**: Direct messaging between users via pop-up chatboxes (Facebook-style).
*   **User List**: Live sidebar showing online users.
*   **Admin System**:
    *   The first user to join becomes the **Admin**.
    *   Admins are highlighted with a red `ADMIN` tag in the sidebar.
    *   Dedicated `@admin <message>` command to message the current admin privately.

### User Interface
*   **Premium Design**: Glassmorphism login page and a sleek dark theme (default).
*   **Theme Support**: Toggle between Dark and Light modes.
*   **Responsive**: Mobile-friendly sidebar implementation.
*   **Chatbox Controls**: Minimize (`−`) and Close (`×`) private chat windows.

### Bot & Commands
The server includes a built-in bot that listens to commands:
*   `@server help`: Lists all available commands.
*   `@server info`: Shows your client session info.
*   `@server server-info`: Displays server statistics (uptime, connected clients).
*   `@server math <expr>`: Solves math expressions (e.g., `@server math 2+2`).
*   `@<username> <message>`: Sends a private message to a specific user.

## Technology Stack

*   **Backend**: Java, Spring Boot, Spring WebSocket.
*   **Frontend**: HTML5, CSS3 (Variables), JavaScript (ES6+).
*   **Protocol**: STOMP over WebSocket (SockJS fallback).

## Installation & Running

1.  **Prerequisites**: Java 17+ and Maven.
2.  **Clone the repository**.
3.  **Run the application**:
    ```bash
    mvn spring-boot:run
    ```
4.  **Access the App**: Open your browser to `http://localhost:8080`.

## Usage Guide

1.  **Login**: Enter any username to join.
2.  **Public Chat**: Type in the main input area to chat with everyone.
3.  **Private Chat**:
    *   Click on a user in the sidebar to open a private chatbox.
    *   OR type `@Username Hello!` in the main chat.
4.  **Bot Commands**: Type `@server help` to see what the bot can do.
