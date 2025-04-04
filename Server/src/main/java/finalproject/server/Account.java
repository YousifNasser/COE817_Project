package finalproject.server;

import javax.crypto.SecretKey;

public class Account {
    private final String username;
    private final String hashedPassword;
    private double balance;
    private transient SecretKey encryptionKey;
    private transient SecretKey macKey;

    public Account(String username, String hashedPassword) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.balance = 0.0;
    }

    // Getters and setters
    public String getUsername() { return username; }
    public String getHashedPassword() { return hashedPassword; }
    public double getBalance() { return balance; }
    public SecretKey getEncryptionKey() { return encryptionKey; }
    public SecretKey getMacKey() { return macKey; }

    public void setSessionKeys(SecretKey encryptionKey, SecretKey macKey) {
        this.encryptionKey = encryptionKey;
        this.macKey = macKey;
    }

    public synchronized boolean deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            return true;
        }
        return false;
    }

    public synchronized boolean withdraw(double amount) {
        if (amount > 0 && balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return username + ":" + hashedPassword + ":" + balance;
    }

    public static Account fromString(String line) {
        String[] parts = line.split(":");
        Account account = new Account(parts[0], parts[1]);
        account.balance = Double.parseDouble(parts[2]);
        return account;
    }
}