package io.chatclient;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;

import java.io.*;
import java.net.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

import java.util.Base64;

@DesignerComponent(version = 1,
    description = "This extension creates a chat client",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "images/extension.png")
@SimpleObject(external = true)
public class ChatClient extends AndroidNonvisibleComponent {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isConnected = false;
    
    private static final String ALGORITHM = "AES";
    private static final byte[] KEY = "MySuperSecretKey".getBytes();
    
    private Cipher encryptCipher;
    private Cipher decryptCipher;

    public ChatClient(ComponentContainer container) {
        super(container.$form());
        
        try {
            Key key = new SecretKeySpec(KEY, ALGORITHM);
            encryptCipher = Cipher.getInstance(ALGORITHM);
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);
            decryptCipher = Cipher.getInstance(ALGORITHM);
            decryptCipher.init(Cipher.DECRYPT_MODE, key);
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SimpleFunction(description = "Connects to the chat server")
    public void ConnectToServer(final String host, final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(host, port);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    isConnected = true;

                    // Now start a new thread to listen for incoming messages
                    new Thread(new IncomingMessagesListener()).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
    public void SendMessage(String message) {
        if (isConnected && out != null) {
            String encryptedMessage = EncryptMessage(message);
            if (encryptedMessage != null) {
                out.println(encryptedMessage);
            }
        }
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

    @SimpleEvent(description = "Event triggered when a new message is received from the chat server.")
    public void MessageReceived(String message) {
        EventDispatcher.dispatchEvent(this, "MessageReceived", message);
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
