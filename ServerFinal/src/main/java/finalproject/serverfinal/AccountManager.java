/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package finalproject.serverfinal;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AccountManager {
    private static final String DB_FILE = "accounts.txt";
    private static final Map<String, Account> accounts = new HashMap<>();

    static {
        loadAccounts();
    }

    private static void loadAccounts() {
        try (BufferedReader reader = new BufferedReader(new FileReader(DB_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Account account = Account.fromString(line);
                accounts.put(account.getUsername(), account);
            }
        } catch (IOException e) {
            System.out.println("[ACCOUNT] Initialized new account database");
        }
    }

    // In AccountManager.java
    public static boolean register(String username, String plaintextPassword) {
        // Store plaintext password directly
        accounts.put(username, new Account(username, plaintextPassword));
        return true;
    }

    public static synchronized Account getAccount(String username) {
        return accounts.get(username);
    }

    private static synchronized void saveAccounts() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DB_FILE))) {
            for (Account account : accounts.values()) {
                writer.println(account.toString());
            }
        } catch (IOException e) {
            System.err.println("[ACCOUNT] Save failed: " + e.getMessage());
        }
    }
    
    public static synchronized void updateAccount(Account account) {
        accounts.put(account.getUsername(), account);
        saveAccounts(); // This will overwrite the file with current balances
    }
}