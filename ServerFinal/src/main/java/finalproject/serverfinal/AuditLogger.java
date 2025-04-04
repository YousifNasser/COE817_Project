/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package finalproject.serverfinal;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditLogger {

    private static String currentLogFile = "transaction.log";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static void rotateLogFile() {
        currentLogFile = "transaction.log";
    }

    private static boolean needsHeader() {
        File file = new File(currentLogFile);
        return !file.exists() || file.length() == 0;
    }

    public static synchronized void logTransaction(String customerId, String action, double amount) {
        // Only log these specific actions
        if (!action.equals("WITHDRAW") && !action.equals("DEPOSIT") && !action.equals("BALANCE")) {
            return;
        }
        
        try (PrintWriter out = new PrintWriter(new FileWriter(currentLogFile, true))) {
            if (needsHeader()) {
                out.println("CUSTOMER_ID|ACTION|AMOUNT|TIMESTAMP");
            }
            
            out.printf("%s|%s|%.2f|%s%n",
                customerId,
                action,
                amount,
                LocalDateTime.now().format(TIME_FORMAT)
            );
        } catch (Exception e) {
            System.err.println("[AUDIT] Error: " + e.getMessage());
        }
    }
}