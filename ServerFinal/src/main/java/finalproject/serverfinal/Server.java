package finalproject.serverfinal;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Server {
    private static final int PORT = 17000;
    private static final String PSK = "SECURE_PSK_12345";
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    
    // For JavaFX GUI updates
    private static App gui;
    private static ObservableList<String> connectedClients = FXCollections.observableArrayList();
    private static ObservableList<String> transactionLog = FXCollections.observableArrayList();

    public static void setGUI(App gui) {
        Server.gui = gui;
    }

    public static ObservableList<String> getConnectedClients() {
        return connectedClients;
    }

    public static ObservableList<String> getTransactionLog() {
        return transactionLog;
    }



    public static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientInfo = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
                Platform.runLater(() -> connectedClients.add(clientInfo));
                
                threadPool.execute(new ClientHandler(clientSocket, PSK));
            }
        } catch (IOException e) {
        }
    }
}