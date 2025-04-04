package finalproject.clientfinal;

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {
    private Client client;
    private TextArea outputArea;
    private TextField usernameField, passwordField, amountField;
    private ComboBox<String> transactionType;
    private Button registerBtn, loginBtn, transactionBtn, logoutBtn;
    private Stage primaryStage;
    private ProgressIndicator progressIndicator;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Secure Banking Client");

        try{
            // Initialize client
            client = new Client();

            // Setup the initial login/register view
            createLoginRegisterView();

            primaryStage.setOnCloseRequest(e -> {
                client.disconnect();
                Platform.exit();
            });
            primaryStage.show();
        }catch(Exception e){
            System.err.print(e.toString());
        }
    }

    private void createLoginRegisterView() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20, 20, 20, 20));
        grid.setVgap(15);
        grid.setHgap(15);

        // UI Components
        Label titleLabel = new Label("Secure Banking Client");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        GridPane.setConstraints(titleLabel, 0, 0, 2, 1);

        Label userLabel = new Label("Username:");
        GridPane.setConstraints(userLabel, 0, 1);
        usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        GridPane.setConstraints(usernameField, 1, 1);

        Label passLabel = new Label("Password:");
        GridPane.setConstraints(passLabel, 0, 2);
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        GridPane.setConstraints(passwordField, 1, 2);

        registerBtn = new Button("Register");
        registerBtn.setPrefWidth(100);
        registerBtn.setOnAction(e -> handleRegistration());
        GridPane.setConstraints(registerBtn, 0, 3);

        loginBtn = new Button("Login");
        loginBtn.setPrefWidth(100);
        loginBtn.setOnAction(e -> handleLogin());
        GridPane.setConstraints(loginBtn, 1, 3);

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(150);
        GridPane.setConstraints(outputArea, 0, 4, 2, 1);

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        GridPane.setConstraints(progressIndicator, 0, 5, 2, 1);

        grid.getChildren().addAll(titleLabel, userLabel, usernameField, passLabel, 
                                passwordField, registerBtn, loginBtn, outputArea, progressIndicator);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getChildren().add(grid);

        Scene scene = new Scene(layout, 400, 400);
        primaryStage.setScene(scene);
    }

    private void createTransactionView() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20, 20, 20, 20));
        grid.setVgap(15);
        grid.setHgap(15);

        // UI Components
        Label titleLabel = new Label("Bank Transactions");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        GridPane.setConstraints(titleLabel, 0, 0, 2, 1);

        Label typeLabel = new Label("Transaction Type:");
        GridPane.setConstraints(typeLabel, 0, 1);
        transactionType = new ComboBox<>();
        transactionType.getItems().addAll("DEPOSIT", "WITHDRAW", "BALANCE");
        transactionType.getSelectionModel().selectFirst();
        transactionType.valueProperty().addListener((obs, oldVal, newVal) -> {
            amountField.setDisable("BALANCE".equals(newVal));
        });
        GridPane.setConstraints(transactionType, 1, 1);

        Label amountLabel = new Label("Amount:");
        GridPane.setConstraints(amountLabel, 0, 2);
        amountField = new TextField();
        amountField.setPromptText("Enter amount");
        GridPane.setConstraints(amountField, 1, 2);

        transactionBtn = new Button("Submit");
        transactionBtn.setPrefWidth(100);
        transactionBtn.setOnAction(e -> handleTransaction());
        GridPane.setConstraints(transactionBtn, 0, 3);

        logoutBtn = new Button("Logout");
        logoutBtn.setPrefWidth(100);
        logoutBtn.setOnAction(e -> {
            client.disconnect();
            createLoginRegisterView();
        });
        GridPane.setConstraints(logoutBtn, 1, 3);

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(150);
        GridPane.setConstraints(outputArea, 0, 4, 2, 1);

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        GridPane.setConstraints(progressIndicator, 0, 5, 2, 1);

        grid.getChildren().addAll(titleLabel, typeLabel, transactionType, amountLabel, 
                                amountField, transactionBtn, logoutBtn, outputArea, progressIndicator);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getChildren().add(grid);

        primaryStage.getScene().setRoot(layout);
    }

    private void handleRegistration() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            outputArea.appendText("Error: Username and password cannot be empty\n");
            return;
        }

        RegistrationService service = new RegistrationService(client, username, password);
        bindServiceToUI(service);
        service.start();
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            outputArea.appendText("Error: Username and password cannot be empty\n");
            return;
        }

        LoginService service = new LoginService(client, username, password);
        bindServiceToUI(service);
        
        service.setOnSucceeded(e -> {
            String result = service.getValue();
            if (result.startsWith("SUCCESS")) {
                outputArea.appendText("Login successful!\n");
                createTransactionView();
            } else {
                outputArea.appendText("Login failed: " + result.substring(5) + "\n");
            }
        });
        
        service.start();
    }

    private void handleTransaction() {
        String type = transactionType.getValue();
        double amount = 0;

        if (!"BALANCE".equals(type)) {
            try {
                amount = Double.parseDouble(amountField.getText().trim());
                if (amount <= 0) {
                    outputArea.appendText("Error: Amount must be positive\n");
                    return;
                }
            } catch (NumberFormatException e) {
                outputArea.appendText("Error: Invalid amount format\n");
                return;
            }
        }

        TransactionService service = new TransactionService(client, type, amount);
        bindServiceToUI(service);
        service.start();
    }

    private void bindServiceToUI(Service<String> service) {
        // Disable buttons during operation
        if (service instanceof RegistrationService) {
            registerBtn.setDisable(true);
            loginBtn.setDisable(true);
        } else if (service instanceof LoginService) {
            registerBtn.setDisable(true);
            loginBtn.setDisable(true);
        } else if (service instanceof TransactionService) {
            transactionBtn.setDisable(true);
            logoutBtn.setDisable(true);
        }

        progressIndicator.visibleProperty().bind(service.runningProperty());
        progressIndicator.progressProperty().bind(service.progressProperty());

        service.setOnSucceeded(e -> {
            outputArea.appendText(service.getValue() + "\n");
            resetButtons();
        });

        service.setOnFailed(e -> {
            outputArea.appendText("Error: " + service.getException().getMessage() + "\n");
            resetButtons();
        });
    }

    private void resetButtons() {
        registerBtn.setDisable(false);
        loginBtn.setDisable(false);
        transactionBtn.setDisable(false);
        logoutBtn.setDisable(false);
    }

    // Service classes for background operations
    private static class RegistrationService extends Service<String> {
        private final Client client;
        private final String username;
        private final String password;

        public RegistrationService(Client client, String username, String password) {
            this.client = client;
            this.username = username;
            this.password = password;
        }

        @Override
        protected Task<String> createTask() {
            return new Task<>() {
                @Override
                protected String call() throws Exception {
                    client.connect();
                    return client.register(username, password);
                }
            };
        }
    }

    private static class LoginService extends Service<String> {
        private final Client client;
        private final String username;
        private final String password;

        public LoginService(Client client, String username, String password) {
            this.client = client;
            this.username = username;
            this.password = password;
        }

        @Override
        protected Task<String> createTask() {
            return new Task<>() {
                @Override
                protected String call() throws Exception {
                    client.connect();
                    return client.login(username, password);
                }
            };
        }
    }

    private static class TransactionService extends Service<String> {
        private final Client client;
        private final String type;
        private final double amount;

        public TransactionService(Client client, String type, double amount) {
            this.client = client;
            this.type = type;
            this.amount = amount;
        }

        @Override
        protected Task<String> createTask() {
            return new Task<>() {
                @Override
                protected String call() throws Exception {
                    return client.performTransaction(type, amount);
                }
            };
        }
    }
}