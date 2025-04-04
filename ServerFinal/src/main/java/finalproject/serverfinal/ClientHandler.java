package finalproject.serverfinal;

import java.io.*;
import java.net.Socket;
import javax.crypto.SecretKey;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final String PSK;

    public ClientHandler(Socket socket, String psk) {
        this.clientSocket = socket;
        this.PSK = psk;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            String request = in.readLine();
            if (request == null) return;

            String[] parts = request.split(":", 2);
            String action = parts[0];
            String username = parts.length > 1 ? parts[1] : "";

            if ("REGISTER".equals(action)) {
                handleRegistration(in, out, username);
            } else if ("LOGIN".equals(action)) {
                handleLogin(in, out, username);
            }
        } catch (Exception e) {
            System.err.println("[HANDLER] Error: " + e.getMessage());
        } finally {
            try { clientSocket.close(); } 
            catch (IOException e) { System.err.println("[SOCKET] Close error"); }
        }
    }

    private void handleRegistration(BufferedReader in, PrintWriter out, String username) throws Exception {
        String encryptedPwd = in.readLine();
        String password = CryptoUtils.decryptData(PSK, encryptedPwd);
        
        // Store plaintext password (INSECURE - for testing only)
        if (AccountManager.register(username, password)) {
            out.println(CryptoUtils.encryptData(PSK, "SUCCESS"));
        } else {
            out.println(CryptoUtils.encryptData(PSK, "FAIL:Username exists"));
        }
    }

    private void handleLogin(BufferedReader in, PrintWriter out, String username) throws Exception {
        Account account = AccountManager.getAccount(username);
        if (account == null) {
            out.println(CryptoUtils.encryptData(PSK, "FAIL:Invalid user"));
            return;
        }

        String encryptedPwd = in.readLine();
        String password = CryptoUtils.decryptData(PSK, encryptedPwd);

        // Compare plaintext passwords (INSECURE)
        if (password.equals(account.getHashedPassword())) {
            String masterSecret = CryptoUtils.generateMasterSecret();
            SecretKey[] keys = CryptoUtils.deriveKeys(masterSecret);
            account.setSessionKeys(keys[0], keys[1]);
            
            out.println(CryptoUtils.encryptData(PSK, "SUCCESS:" + masterSecret));
            handleTransactions(in, out, account);
        } else {
            out.println(CryptoUtils.encryptData(PSK, "FAIL:Invalid password"));
        }
    }
    
    private void handleTransactions(BufferedReader in, PrintWriter out, Account account) {
        try {
            while (true) {
                String request = in.readLine();
                if (request == null) break;

                String[] parts = request.split("\\|");
                if (!CryptoUtils.verifyMAC(parts[0], parts[1], account.getMacKey())) {
                    out.println("ERROR|Invalid MAC");
                    continue;
                }

                String decrypted = CryptoUtils.decryptData(account.getEncryptionKey(), parts[0]);
                String[] transaction = decrypted.split("\\|");
                String type = transaction[0];
                double amount = Double.parseDouble(transaction[1]);

                boolean success = false;
                switch (type) {
                    case "DEPOSIT":
                        success = account.deposit(amount);
                        break;
                    case "WITHDRAW":
                        success = account.withdraw(amount);
                        break;
                    case "BALANCE":
                        success = true;
                        break;
                }

                if (success) {
                    AccountManager.updateAccount(account);

                    String response = "SUCCESS|" + account.getBalance();
                    String encrypted = CryptoUtils.encryptData(account.getEncryptionKey(), response);
                    String mac = CryptoUtils.generateMAC(encrypted, account.getMacKey());
                    out.println(encrypted + "|" + mac);
                    
                    if (type.equals("DEPOSIT") || type.equals("WITHDRAW")) {
                        AuditLogger.logTransaction(account.getUsername(), type, amount);                    
                    } 
                    else if(type.equals("BALANCE")){
                        AuditLogger.logTransaction(account.getUsername(), type, account.getBalance());
                    }
                } else {
                    out.println("ERROR|Transaction failed");
                }
            }
        } catch (Exception e) {
            System.err.println("[TRANSACTION] Error: " + e.getMessage());
        }
    }
}