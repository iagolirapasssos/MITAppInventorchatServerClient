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
 * ChatClient
 * This extension creates a chat client
 * Version: 1.0
 * Author: Francisco Iago Lira Passos
 * Date: 2023-10-15
 * Docs: https://docs.google.com/document/d/1xk9dMfczvjbbwD-wMsr-ffqkTlE3ga0ocCE1KOb2wvw/pub#h.4jyv4s6bnjrd
 */

// Package and import statements for necessary libraries and dependencies
package io.chatclient;

import android.util.Log; // For logging debug messages
import android.os.Handler; // For posting runnables to execute on the UI thread
import android.os.Looper; // For checking the current thread and looper

// Annotations and components for App Inventor integration
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;

// Java I/O and networking imports
import java.io.*;
import java.net.*;
// Imports for encryption and decryption
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

// Utility imports for generating unique identifiers and formatting dates
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.Date;

// For encoding and decoding messages in Base64 format
import java.util.Base64;

//Annotation to describe the designer properties of the extension
@DesignerComponent(version = 1,
    description = "This extension creates a chat client",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "images/extension.png")
@SimpleObject(external = true)
public class ChatClient extends AndroidNonvisibleComponent {
	// Declare client socket and I/O for network communication
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isConnected = false; // To track the connection status
    
    // Constants for the encryption algorithm and key
    private static final String ALGORITHM = "AES";
    private static final byte[] KEY = "MySuperSecretKey".getBytes();
    
    // Ciphers for encryption and decryption
    private Cipher encryptCipher;
    private Cipher decryptCipher;
    
    // Unique client ID and timestamp formatter
    private final String clientId = UUID.randomUUID().toString();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    // Constructor for the chat client
    public ChatClient(ComponentContainer container) {
        super(container.$form());
        
        // Initialize encryption and decryption ciphers using the secret key
        try {
            Key key = new SecretKeySpec(KEY, ALGORITHM);
            encryptCipher = Cipher.getInstance(ALGORITHM);
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);
            decryptCipher = Cipher.getInstance(ALGORITHM);
            decryptCipher.init(Cipher.DECRYPT_MODE, key);
        } 
        catch (Exception e) {
            e.printStackTrace(); // Print stack trace on error
        }
    }
    
    // Method to dispatch events on the UI thread
    private void dispatchEventOnUiThread(final Runnable eventRunner) {
        // Check if we are already on the UI thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
        	// If already on UI thread, run the event
            eventRunner.run();
        } else {
            // Use the Handler associated with the main looper to post the Runnable
        	// Otherwise, post the event to the handler of the main looper
            new Handler(Looper.getMainLooper()).post(eventRunner);
        }
    }
    
    // Method to connect to the chat server
    @SimpleFunction(description = "Connects to the chat server")
    public void ConnectToServer(final String host, final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(host, port); // Establish a new socket connection
                    out = new PrintWriter(socket.getOutputStream(), true); // Initialize the print writer for output
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Initialize the reader for input
                    isConnected = true; // Update connection status

                    // Trigger Connected event on UI thread
                    dispatchEventOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Connected();
                        }
                    });

                    // Now start a new thread to listen for incoming messages
                    // Start a new thread to listen for incoming messages
                    new Thread(new IncomingMessagesListener()).start();
                } catch (IOException e) {
                	// Handle exceptions and trigger the ErrorOccurred event on the UI thread
                	final String errorMessage = e.getMessage();
                    // Trigger ErrorOccurred event on UI thread
                    dispatchEventOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ErrorOccurred(errorMessage);
                        }
                    });
                }
            }
        }).start(); // Start the connection thread
    }
    
    @SimpleFunction(description = "Decrypts a message")
    public String DecryptMessage(String encryptedMessage) {
        try {
            byte[] decryptedBytes = decryptCipher.doFinal(Base64.getDecoder().decode(encryptedMessage));
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @SimpleFunction(description = "Encrypts a message")
    public String EncryptMessage(String message) {
        try {
            byte[] encryptedBytes = encryptCipher.doFinal(message.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SimpleFunction(description = "Sends a message to the chat server")
    public void SendMessage(final String message) {
        if (!isConnected || out == null) {
            String errorMsg = "Client is not connected to the server.";
            Log.e("ChatClient", errorMsg);
            ErrorOccurred(errorMsg);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                	String timestamp = dateFormat.format(new Date());
                    String clientIp = socket.getLocalAddress().getHostAddress();
                    String messageWithDetails = "SendMessage," + timestamp + "," + clientIp + "," + clientId + "," + message;
                    
                    final String encryptedMessage = EncryptMessage(messageWithDetails);
                    if (encryptedMessage == null) {
                        Log.e("ChatClient", "Encryption returned null");
                        ErrorOccurred("Encryption error");
                        return;
                    }

                    Log.i("ChatClient", "Sending message: " + encryptedMessage);
                    out.println(encryptedMessage);
                    out.flush();
                } catch (Exception e) {
                    Log.e("ChatClient", "Send Message failed: " + e.getMessage(), e);
                    ErrorOccurred(e.getMessage());
                }
            }
        }).start();
    }

    
    @SimpleFunction(description = "Sends a message along with date, IP, and unique ID to the chat server")
    public void SendMessageWithDetails(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isConnected && out != null) {
                    try {
                    	String timestamp = dateFormat.format(new Date());
                        String clientIp = socket.getLocalAddress().getHostAddress();
                        String messageWithDetails = "SendMessageWithDetails," + timestamp + "," + clientIp + "," + clientId + "," + message;
                        
                        final String encryptedMessage = EncryptMessage(messageWithDetails);
                        if (encryptedMessage != null) {
                            Log.i("ChatClient", "Sending message: " + encryptedMessage);
                            out.println(encryptedMessage);
                            out.flush(); // Garante que a mensagem seja enviada imediatamente
                        } else {
                            Log.e("ChatClient", "Encryption returned null");
                            ErrorOccurred("Encryption error");
                        }
                    } catch (Exception e) {
                        Log.e("ChatClient", "Send Message failed: " + e.getMessage(), e);
                        ErrorOccurred(e.getMessage());
                    }
                } else {
                    String errorMsg = "Client is not connected to server.";
                    Log.e("ChatClient", errorMsg);
                    ErrorOccurred(errorMsg);
                }
            }
        }).start();
    }
    
 // Modifique o método IncomingMessagesListener para descriptografar as mensagens recebidas
    private class IncomingMessagesListener implements Runnable {
        @Override
        public void run() {
            try {
                String encryptedLine;
                while ((encryptedLine = in.readLine()) != null) {
                    String decryptedLine = DecryptMessage(encryptedLine);
                    if (decryptedLine != null) {
                        // Trigger an event that a new message has been received
                        MessageReceived(decryptedLine);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @SimpleFunction(description = "Disconnects from the chat server")
    public void DisconnectFromServer() {
        if (isConnected) {
            try {
                isConnected = false;
                if (socket != null) {
                    socket.close();
                }
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                // Trigger Disconnected event
                Disconnected();
            } catch (IOException e) {
                // Trigger ErrorOccurred event
                ErrorOccurred(e.getMessage());
            }
        }
    }

    @SimpleEvent(description = "Event triggered when a new message is received from the chat server.")
    public void MessageReceived(final String message) {
    	dispatchEventOnUiThread(new Runnable() {
            @Override
            public void run() {
            	EventDispatcher.dispatchEvent(ChatClient.this, "MessageReceived", message);
            }
    	});
    }
    
    @SimpleEvent(description = "Event triggered when an error occurs.")
    public void ErrorOccurred(final String errorMessage) {
        dispatchEventOnUiThread(new Runnable() {
            @Override
            public void run() {
                EventDispatcher.dispatchEvent(ChatClient.this, "ErrorOccurred", errorMessage);
            }
        });
    }

    @SimpleEvent(description = "Event triggered when the client successfully connects to the server.")
    public void Connected() {
    	dispatchEventOnUiThread(new Runnable() {
            @Override
            public void run() {
            	EventDispatcher.dispatchEvent(ChatClient.this, "Connected");
            }
    	});
    }

    @SimpleEvent(description = "Event triggered when the client is disconnected from the server.")
    public void Disconnected() {
    	dispatchEventOnUiThread(new Runnable() {
            @Override
            public void run() {
            	EventDispatcher.dispatchEvent(ChatClient.this, "Disconnected");
            }
    	});
    }
}
