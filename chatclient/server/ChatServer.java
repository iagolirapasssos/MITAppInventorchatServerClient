/*
 * The MIT License (MIT)
Copyright © 2023 <copyright holders>

Permission is hereby granted, free of charge, to any person obtaining a copy of this 
software and associated documentation files (the “Software”), to deal in the Software 
without restriction, including without limitation the rights to use, copy, modify, merge, 
publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons 
to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies 
or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR 
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
DEALINGS IN THE SOFTWARE.
 */

/*
 * ChatServer
 * This extension creates a chat client
 * Version: 1.0
 * Author: Francisco Iago Lira Passos
 * Date: 2023-10-15
 * Docs: https://docs.google.com/document/d/1xk9dMfczvjbbwD-wMsr-ffqkTlE3ga0ocCE1KOb2wvw/pub#h.4jyv4s6bnjrd
 */

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
	
public class ChatServer {
	// Define the server port and the encryption details
    private static final int PORT = 12345;
    private static final String KEY = "MySuperSecretKey"; // 16-byte key
    private static final String ALGORITHM = "AES";
    
    // Create a thread-safe list to hold all client handlers
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server listening on port " + PORT);
        
        while (true) {
        	// Accept new client connections
            Socket clientSocket = serverSocket.accept();
            System.out.println("Connection established with " + clientSocket.getRemoteSocketAddress());
            
            // Create a new client handler for the connection
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            clients.add(clientHandler);
            
            // Start a new thread to handle the client
            new Thread(clientHandler).start();
        }
    }
    
    // Broadcast a message to all clients except the sender
    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }
    
    // Remove a client from the list
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    // Inner class to handle client connections
    private static class ClientHandler implements Runnable {
    	private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private Cipher cipher;
        private SecretKeySpec keySpec;
        
        // Variables to store client details
        private String clientId;
        private String clientIp;
        private String clientTimestamp;


        public ClientHandler(Socket socket) throws Exception {
            this.socket = socket;
            
            // Set up streams for communication
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            // Set up encryption components
            keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
            cipher = Cipher.getInstance(ALGORITHM);
        }

        @Override
        public void run() {
            try {
                while (true) {
                	// Read the encrypted message from the client
                    String encryptedMessage = in.readLine();
                    if (encryptedMessage == null) break;

                    // Decrypt the message
                    cipher.init(Cipher.DECRYPT_MODE, keySpec);
                    byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedMessage));
                    String message = new String(decryptedBytes);
                    
                    // Check for the message details
                    String[] parts = message.split(",", 4);
                    
                    // Assign the details to variables
                    clientMethod = parts[0];
                    clientTimestamp = parts[1];
                    clientIp = parts[2];
                    clientId = parts[3];
                    clientMessage = parts[4];
                    
                    if (clientMethod == "SendMessageWithDetails") {
                        message = clientMessage + ", " + clientIp + ", " + clientId + ", " + clientTimestamp;
                    } else {
                    	message = clientMessage;
                    }
                    
                    // Log the received message and details
                    System.out.println(
                    		"clientMethod: " + clientMethod
                    		+ ", Message received: " + clientMessage 
                    		+ ", IP: " + clientIp + ", ID: " 
                    		+ clientId + "Timestamp: " 
                    		+ clientTimestamp
                    		);
                    
                    // Encrypt and send the response
                    cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                    String encryptedResponse = Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes()));
                    out.println(encryptedResponse);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ChatServer.removeClient(this);
            }
        }
        
        // Helper method to send a message to the client
        public void sendMessage(String message) {
            out.println(message);
        }
    }
}
