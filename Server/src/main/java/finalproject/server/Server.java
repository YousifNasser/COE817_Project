package finalproject.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 17000;
    private static final String PSK = "SECURE_PSK_12345";
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Running on port " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket, PSK));
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Failed: " + e.getMessage());
        }
    }
}