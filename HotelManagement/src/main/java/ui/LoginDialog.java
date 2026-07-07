package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import service.HotelService;

public class LoginDialog {

        private static String loggedInUser = null;
        private static String loggedInUserRole = null;

        public static boolean showLoginDialog(Stage owner) {
                Stage dialog = new Stage();
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.initOwner(owner);
                dialog.initStyle(StageStyle.UNDECORATED);
                dialog.setTitle("Login - Grand Vista Hotel");

                VBox root = new VBox(0);
                root.setStyle("-fx-background-color: white; -fx-border-radius: 12px; -fx-background-radius: 12px;");

                // Header with orange gradient
                VBox header = new VBox(8);
                header.setStyle("-fx-background-color: linear-gradient(to right, #F97316, #FB923C);");
                header.setPadding(new Insets(40, 40, 40, 40));
                header.setAlignment(Pos.CENTER);

                Label title = new Label("GRAND VISTA");
                title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold; -fx-font-family: 'Inter';");
                Label subtitle = new Label("Hotel Management System");
                subtitle.setStyle(
                                "-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 13px; -fx-font-family: 'Inter';");
                header.getChildren().addAll(title, subtitle);

                // Form
                VBox form = new VBox(20);
                form.setPadding(new Insets(40, 40, 40, 40));
                form.setAlignment(Pos.CENTER);

                Label welcome = new Label("Welcome Back");
                welcome.setStyle(
                                "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1F2937; -fx-font-family: 'Inter';");

                // Username field
                VBox usernameBox = new VBox(8);
                Label userLabel = new Label("USERNAME");
                userLabel.setStyle(
                                "-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #6B7280; -fx-letter-spacing: 0.5px;");
                TextField username = new TextField();
                username.setPromptText("Enter your username");
                username.setStyle(
                                "-fx-padding: 12px 14px; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-border-color: #E5E7EB; -fx-background-color: white; -fx-font-size: 14px;");
                username.getStyleClass().add("login-field");
                usernameBox.getChildren().addAll(userLabel, username);

                // Password field
                VBox passwordBox = new VBox(8);
                Label passLabel = new Label("PASSWORD");
                passLabel.setStyle(
                                "-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #6B7280; -fx-letter-spacing: 0.5px;");
                PasswordField password = new PasswordField();
                password.setPromptText("Enter your password");
                password.setStyle(
                                "-fx-padding: 12px 14px; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-border-color: #E5E7EB; -fx-background-color: white; -fx-font-size: 14px;");
                passwordBox.getChildren().addAll(passLabel, password);

                // Login button
                Button loginBtn = new Button("SIGN IN");
                loginBtn.setStyle(
                                "-fx-background-color: #F97316; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 10px; -fx-cursor: hand;");
                loginBtn.setMaxWidth(Double.MAX_VALUE);

                loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(
                                "-fx-background-color: #EA580C; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 10px; -fx-cursor: hand;"));
                loginBtn.setOnMouseExited(e -> loginBtn.setStyle(
                                "-fx-background-color: #F97316; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 10px; -fx-cursor: hand;"));

                Label errorMsg = new Label();
                errorMsg.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
                errorMsg.setVisible(false);

                loginBtn.setOnAction(e -> {
                        String user = username.getText();
                        String pass = password.getText();

                        if (user.isEmpty() || pass.isEmpty()) {
                                errorMsg.setText("Please enter both username and password");
                                errorMsg.setVisible(true);
                        } else if (HotelService.getInstance().authenticate(user, pass)) {
                                loggedInUser = user;
                                loggedInUserRole = HotelService.getInstance().getUserRole(user);
                                dialog.close();
                        } else {
                                errorMsg.setText("Invalid username or password");
                                errorMsg.setVisible(true);
                                username.clear();
                                password.clear();
                                username.requestFocus();
                        }
                });

                // Enter key listener
                password.setOnAction(e -> loginBtn.fire());
                username.setOnAction(e -> password.requestFocus());

                form.getChildren().addAll(welcome, usernameBox, passwordBox, loginBtn, errorMsg);

                // Close button
                Button closeBtn = new Button("Close");
                closeBtn.setStyle(
                                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-font-weight: bold;");
                closeBtn.setOnAction(e -> System.exit(0));

                StackPane closePane = new StackPane(closeBtn);
                closePane.setAlignment(Pos.TOP_RIGHT);
                closePane.setPadding(new Insets(15, 15, 0, 0));

                StackPane main = new StackPane();
                main.getChildren().addAll(root, closePane);
                root.getChildren().addAll(header, form);

                Scene scene = new Scene(main, 480, 600);
                scene.setFill(Color.TRANSPARENT);
                dialog.setScene(scene);

                // Add focus to username field
                dialog.setOnShown(e -> username.requestFocus());

                dialog.showAndWait();

                return loggedInUser != null;
        }

        public static String getLoggedInUser() {
                return loggedInUser;
        }

        public static String getLoggedInUserRole() {
                return loggedInUserRole;
        }

        public static void logout() {
                loggedInUser = null;
                loggedInUserRole = null;
        }
}