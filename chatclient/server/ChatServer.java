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
    private static final int PORT = 12345;
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static final String KEY = "MySuperSecretKey"; // 16-byte key
    private static final String ALGORITHM = "AES";

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor escutando na porta " + PORT);
        
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Conexão estabelecida com " + clientSocket.getRemoteSocketAddress());
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            clients.add(clientHandler);
            new Thread(clientHandler).start();
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    private static class ClientHandler implements Runnable {
    	private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private Cipher cipher;
        private SecretKeySpec keySpec;
        
        // Move the variables inside the ClientHandler class
        private String clientId;
        private String clientIp;
        private String clientTimestamp;


        public ClientHandler(Socket socket) throws Exception {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
            cipher = Cipher.getInstance(ALGORITHM);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String encryptedMessage = in.readLine();
                    if (encryptedMessage == null) break;

                    // Decrypt the message
                    cipher.init(Cipher.DECRYPT_MODE, keySpec);
                    byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedMessage));
                    String message = new String(decryptedBytes);
                    
                    // Check for the message details
                    String[] parts = message.split(",", 4);
                    if (parts.length == 4) {
                        // Message contains details
                        clientTimestamp = parts[0];
                        clientIp = parts[1];
                        clientId = parts[2];
                        message = parts[3];
                    }
                    
                    System.out.println("Message received: " + message);
                    
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

        public void sendMessage(String message) {
            out.println(message);
        }
    }
}
