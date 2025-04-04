package finalproject.server;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditLogger {
    private static final String LOG_DIR = "logs";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static String currentLogFile;

    static {
        new File(LOG_DIR).mkdirs();
        rotateLogFile();
    }

    private static void rotateLogFile() {
        currentLogFile = LOG_DIR + "/transactions_" + LocalDate.now().format(DATE_FORMAT) + ".log";
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

        if (!currentLogFile.contains(LocalDate.now().format(DATE_FORMAT))) {
            rotateLogFile();
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