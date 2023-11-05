# MITAppInventorchatServerClient
This repository contains the implementation of a secure chat application with server-client architecture, where messages are encrypted and decrypted using symmetric cryptography. Below is a detailed description of how it works, how to use it, security considerations, and future improvements.

# Secure Chat Application

This repository contains the implementation of a secure chat application with server-client architecture, where messages are encrypted and decrypted using symmetric cryptography. Below is a detailed description of how it works, how to use it, security considerations, and future improvements.

## Overview

The chat application is divided into two main components: the server and the client.

### Server

The server is written in Java and listens for incoming TCP connections from clients. Upon a successful connection, it handles incoming messages, decrypts them, and broadcasts the encrypted messages to all connected clients.

#### How It Works:

- The server initializes a `ServerSocket` on a specified port and waits for client connections.
- When a client connects, the server spawns a new thread to handle the communication with that client, enabling simultaneous handling of multiple clients.
- The server uses symmetric key encryption (AES) to encrypt and decrypt messages. All clients and the server must share the same secret key beforehand.
- Received messages from one client are decrypted, logged to the console, and then re-encrypted and broadcasted to all other connected clients.

### Client

The client, which can also be written in Java or other programming languages, connects to the server via TCP, sends encrypted messages, and receives encrypted messages from the server, decrypting them upon arrival.

#### How It Works:

- The client establishes a connection to the server using `Sockets`.
- The client encrypts messages using the shared secret key and sends them to the server.
- When the client receives messages from the server, it decrypts them for display to the user.

## Usage Instructions

### Server

To compile and run the server:

1. Navigate to the server's source code directory.
2. Compile the server code using `javac`:

   ```sh
   javac ChatServer.java
   ```

3. Run the compiled server code with `java`:

   ```sh
   java ChatServer
   ```

### Client

The client will be an Android application that communicates with the server. It encrypts messages before sending them and decrypts incoming messages using the provided extensions for encryption.

#### How It Works:

- The client app uses the ChatClient extension to establish a connection to the server.
- Users can input text messages that are encrypted with AES (or another method depending on the extension's implementation) before being sent to the server.
- The client app receives encrypted messages from the server, which are then decrypted and displayed to the user.

## Usage Instructions for MIT App Inventor

### Setting Up the Client

1. Open MIT App Inventor and create a new project.
2. Design the interface with at least a TextBox for entering messages, a Button to send messages, and a ListView or Label to display incoming messages.
3. Import the ChatClient extension (as an `.aix` file) into your project.
4. Use the blocks provided by the extension to connect to the server, send messages, and handle incoming messages.

### Running the Client App

1. Connect your Android device via USB or use an emulator to test the app.
2. Click the "Connect" menu in MIT App Inventor and select your device or emulator.
3. The MIT App Inventor Companion app will open on your device, and you can interact with your application.

## Security Considerations

The current implementation uses a static symmetric key, which poses security risks. Here are some recommended steps to enhance security:

- Implement SSL/TLS to secure the communication channel between the client and server.
- Use a key exchange protocol like Diffie-Hellman to safely exchange symmetric keys over a public channel.
- Store the symmetric key securely using the Android Keystore on the client side and a secure secrets management solution on the server side.
- Implement certificate pinning to prevent man-in-the-middle attacks.
- Regularly rotate the symmetric key and implement a secure mechanism to distribute the new key.

## Was the server designed for multiple connections?

Yes, the Java chat server described in the previous messages is designed to handle multiple client connections concurrently. It achieves this by spawning a new thread for each client that connects. This way, the server can manage multiple connections in parallel, allowing multiple clients to communicate with each other simultaneously.

Here's a brief overview of how it works:

1. **Server Accepts Connections:**
   The server listens on a specified port using a `ServerSocket`. When a new client tries to connect, the `ServerSocket` accepts the connection, creating a new `Socket` for the client.

2. **Multithreading for Concurrent Handling:**
   For each new `Socket`, the server starts a new `Thread` running an instance of a `ClientHandler`. This handler takes care of the incoming and outgoing messages for that particular client.

3. **Broadcasting Messages:**
   When a client sends a message to the server, the server's `ClientHandler` decrypts the message, and then the server encrypts the message again and broadcasts it to all other connected clients through their respective `ClientHandler` instances.

4. **Managing Multiple Clients:**
   Since each client is handled by its own thread, the server can manage multiple clients at the same time. It maintains a list of clients, adding clients to the list when they connect and removing them when they disconnect.

Here's a simple schematic of the server's multithreaded operations:

```
[Client1] ---\
              \
[Client2] -----> [Server] ---> [ClientHandler1, ClientHandler2, ...]
              /
[ClientN] ---/
```

Each `ClientHandler` can receive messages from its client, decrypt the messages, and then pass them to the server to be sent out (after re-encryption) to all other clients.

In the context of the chat application, this means that any message sent by one member can be received by all other members in the chat, making it a multi-member chat system. However, ensure that the server is robustly designed to handle the potential complexities and load of multiple client connections, which might include synchronization issues, handling disconnects gracefully, and ensuring thread-safe operations on shared resources (like the list of client handlers).

## Future Steps

- Replace the static symmetric key with a securely exchanged key using a protocol such as Diffie-Hellman.
- Incorporate SSL/TLS to encrypt the communication channel.
- Implement user authentication to ensure that only authorized users can connect to the server.
- Develop a user-friendly client interface for message display and input.
- Add support for features such as private messaging, file transfer, and user presence indication.
- Conduct thorough security audits and penetration testing to identify and fix potential vulnerabilities.

## Contributing

Contributions are welcome! Please read the `CONTRIBUTING.md` file for guidelines on how to submit contributions. Before implementing a new feature or change, it's recommended to discuss it with the maintainers by opening an issue.

## License

This project is licensed under the MIT License - see the `LICENSE` file for details.

