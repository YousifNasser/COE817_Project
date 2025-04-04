package finalproject.clientfinal;

import java.io.*;
import java.net.Socket;
import javax.crypto.SecretKey;

public class Client {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 17000;
    private static final String PSK = "SECURE_PSK_12345";
    private SecretKey encryptionKey, macKey;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public void connect() throws IOException {
        socket = new Socket(SERVER_IP, PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    public String register(String username, String password) throws Exception {
        out.println("REGISTER:" + username);
        out.println(CryptoUtils.encryptData(PSK, password));
        
        String response = CryptoUtils.decryptData(PSK, in.readLine());
        return response;
    }

    public String login(String username, String password) throws Exception {
        out.println("LOGIN:" + username);
        out.println(CryptoUtils.encryptData(PSK, password));
        
        String response = CryptoUtils.decryptData(PSK, in.readLine());
        if (response.startsWith("SUCCESS")) {
            String masterSecret = response.split(":")[1];
            SecretKey[] keys = CryptoUtils.deriveKeys(masterSecret);
            encryptionKey = keys[0];
            macKey = keys[1];
        }
        return response;
    }

    public String performTransaction(String type, double amount) throws Exception {
        String request = type + "|" + amount + "|" + System.currentTimeMillis();
        String encrypted = CryptoUtils.encryptData(encryptionKey, request);
        String mac = CryptoUtils.generateMAC(encrypted, macKey);
        out.println(encrypted + "|" + mac);
        
        String response = in.readLine();
        String[] parts = response.split("\\|");
        if (CryptoUtils.verifyMAC(parts[0], parts[1], macKey)) {
            return CryptoUtils.decryptData(encryptionKey, parts[0]);
        }
        throw new SecurityException("Invalid MAC in server response");
    }
}