package finalproject.serverfinal;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class App extends Application {
    private TableView<ObservableList<String>> logTableView = new TableView<>();
    private ObservableList<ObservableList<String>> logEntries = FXCollections.observableArrayList();
    private Button startButton;
    private static final String LOG_FILE = "transaction.log";
    
    // Define the required column order
    private static final List<String> REQUIRED_COLUMNS = Arrays.asList(
        "CUSTOMER_ID", "ACTION", "AMOUNT", "TIMESTAMP"
    );

    @Override
    public void start(Stage primaryStage) {
        startButton = new Button("Start Server");
        // Setup table
        logTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Layout
        VBox root = new VBox(10, 
            new Label("Transaction Log"),
            logTableView,            
            new Label("Server Control"),
            startButton
        );
        VBox.setVgrow(logTableView, Priority.ALWAYS);

        // Setup auto-refresh
        setupLogWatcher();

        // Initial load
        refreshLog();
        
        new Thread(() -> {
            Server.setGUI(this);
            Server.startServer();
         }).start();
        
        // Configure window
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Server Dashboard");
        primaryStage.show();
    }

private void setupLogWatcher() {
    Thread logWatcher = new Thread(() -> {
        try {
            Path logPath = Paths.get(LOG_FILE).toAbsolutePath();
            Path parentDir = logPath.getParent();
            
            // Create file if it doesn't exist
            if (!Files.exists(logPath)) {
                Files.createDirectories(parentDir); // Create parent directories if needed
                Files.createFile(logPath);
                // Write header if this is a new file
                Files.write(logPath, "CUSTOMER_ID|ACTION|AMOUNT|TIMESTAMP\n".getBytes());
            }

            // Verify parent directory exists
            if (parentDir == null || !Files.exists(parentDir)) {
                System.err.println("[LOG WATCHER] Parent directory does not exist: " + parentDir);
                return;
            }

            WatchService watchService = FileSystems.getDefault().newWatchService();
            parentDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.context().toString().equals(logPath.getFileName().toString())) {
                        javafx.application.Platform.runLater(this::refreshLog);
                    }
                }
                if (!key.reset()) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[LOG WATCHER] Error: " + e.getMessage());
            // Attempt to restart watcher after delay
            try {
                Thread.sleep(5000);
                setupLogWatcher();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    });
    logWatcher.setDaemon(true);
    logWatcher.start();
}

    private void refreshLog() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(LOG_FILE));
            if (lines.isEmpty()) return;

            // Clear existing data
            logEntries.clear();
            logTableView.getColumns().clear();

            // Process header row
            String[] headers = lines.get(0).split("\\|");
            Map<String, Integer> columnIndexMap = new HashMap<>();
            
            // Create mapping from column name to its index
            for (int i = 0; i < headers.length; i++) {
                columnIndexMap.put(headers[i].trim().toUpperCase(), i);
            }

            // Create columns in the required order
            for (String columnName : REQUIRED_COLUMNS) {
                if (columnIndexMap.containsKey(columnName)) {
                    TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnName);
                    final int columnIndex = columnIndexMap.get(columnName);
                    column.setCellValueFactory(param -> 
                        new javafx.beans.property.SimpleStringProperty(param.getValue().get(columnIndex)));
                    logTableView.getColumns().add(column);
                }
            }

            // Add data rows (skip header row)
            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split("\\|");
                ObservableList<String> row = FXCollections.observableArrayList();
                row.addAll(Arrays.asList(parts));
                logEntries.add(row);
            }

            logTableView.setItems(logEntries);

        } catch (IOException e) {
            ObservableList<String> errorRow = FXCollections.observableArrayList();
            errorRow.add(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
            errorRow.add("SYSTEM");
            errorRow.add("ERROR");
            errorRow.add("Could not read log file: " + e.getMessage());
            logEntries.add(errorRow);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}