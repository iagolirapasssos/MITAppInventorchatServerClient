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

                    // Descriptografa a mensagem
                    cipher.init(Cipher.DECRYPT_MODE, keySpec);
                    byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedMessage));
                    String message = new String(decryptedBytes);
                    
                    System.out.println("Mensagem recebida: " + message);

                    // Recriptografa e envia para todos os outros clientes
                    cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                    String reEncryptedMessage = Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes()));
                    ChatServer.broadcast(reEncryptedMessage, this);
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
