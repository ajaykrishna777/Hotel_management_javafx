package ui;

import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import model.User;
import service.HotelService;

import java.util.Optional;

public class SettingsTab {

    private final HotelService svc = HotelService.getInstance();
    private final Node content;

    private TextField hotelNameField;
    private Runnable onDataReset;

    public SettingsTab() {
        this(null);
    }

    public SettingsTab(Runnable onReset) {
        this.onDataReset = onReset;
        content = build();
    }

    public Node getContent() {
        return content;
    }

    private Node build() {
        ScrollPane scroll = new ScrollPane(buildPage());
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }

    private VBox buildPage() {
        VBox page = new VBox(0);
        page.getStyleClass().add("dash-page");

        // Toolbar
        HBox toolbar = new HBox();
        toolbar.getStyleClass().add("room-toolbar");
        toolbar.setPadding(new Insets(14, 24, 14, 24));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Settings");
        title.getStyleClass().add("page-title");
        toolbar.getChildren().add(title);

        VBox body = new VBox(24);
        body.setPadding(new Insets(28, 32, 32, 32));

        body.getChildren().addAll(
                buildPropertyCard(),
                buildUserCard(),
                buildThemeCard(),
                buildDataCard(),
                buildAboutCard());

        page.getChildren().addAll(toolbar, body);
        return page;
    }

    // ── Hotel Property ──────────────────────────────────────────────────────
    private VBox buildPropertyCard() {
        VBox card = card("Hotel Property");
        VBox form = new VBox(14);
        form.setPadding(new Insets(20));

        hotelNameField = input("Grand Vista Hotel");
        TextField addressField = input("123 MG Road, Bengaluru, Karnataka 560001");
        TextField phoneField = input("+91 80 1234 5678");
        TextField emailField = input("info@grandvista.in");
        TextField taxField = input("GST: 29ABCDE1234F1Z5");

        Button saveBtn = new Button("Save Property Details");
        saveBtn.getStyleClass().add("primary-btn");
        saveBtn.setOnAction(e -> {
            new Alert(Alert.AlertType.INFORMATION,
                    "Property details saved (in-memory only in this demo).",
                    ButtonType.OK).showAndWait();
        });

        form.getChildren().addAll(
                fieldGroup("HOTEL NAME", hotelNameField),
                fieldGroup("ADDRESS", addressField),
                fieldGroup("PHONE", phoneField),
                fieldGroup("EMAIL", emailField),
                fieldGroup("TAX / GST ID", taxField),
                saveBtn);
        card.getChildren().add(form);
        return card;
    }

    // ── Theme ────────────────────────────────────────────────────────────────
    private VBox buildThemeCard() {
        VBox card = card("Theme & Appearance");
        VBox form = new VBox(14);
        form.setPadding(new Insets(20));

        Label info = new Label(
                "The application uses an Orange Professional theme by default.\n" +
                        "Edit /src/main/resources/css/style.css to customise colours.");
        info.setWrapText(true);
        info.getStyleClass().add("guest-stats");

        ComboBox<String> fontCombo = new ComboBox<>();
        fontCombo.getItems().addAll("System Default", "Arial", "Roboto", "Helvetica");
        fontCombo.setValue("System Default");
        fontCombo.setMaxWidth(Double.MAX_VALUE);
        fontCombo.getStyleClass().add("hotel-combo");

        ComboBox<String> sizeCombo = new ComboBox<>();
        sizeCombo.getItems().addAll("Small", "Medium", "Large");
        sizeCombo.setValue("Medium");
        sizeCombo.setMaxWidth(Double.MAX_VALUE);
        sizeCombo.getStyleClass().add("hotel-combo");

        form.getChildren().addAll(
                info,
                fieldGroup("UI FONT", fontCombo),
                fieldGroup("FONT SIZE", sizeCombo));
        card.getChildren().add(form);
        return card;
    }

    // ── Data Management ──────────────────────────────────────────────────────
    private VBox buildDataCard() {
        VBox card = card("Data Management");
        VBox form = new VBox(14);
        form.setPadding(new Insets(20));

        Label warn = new Label(
                "Warning: resetting data will clear all bookings, guests, and bills\n" +
                        "and reload the default sample data. This cannot be undone.");
        warn.setWrapText(true);
        warn.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");

        Button resetBtn = new Button("Reset to Sample Data");
        resetBtn.getStyleClass().addAll("secondary-btn");
        resetBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Reset all data to sample defaults?", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirm Reset");
            confirm.setHeaderText(null);
            confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
                svc.resetData();
                if (onDataReset != null)
                    onDataReset.run();
                new Alert(Alert.AlertType.INFORMATION, "Data has been reset to sample defaults.", ButtonType.OK)
                        .showAndWait();
            });
        });

        form.getChildren().addAll(warn, resetBtn);
        card.getChildren().add(form);
        return card;
    }

    // ── User Management ─────────────────────────────────────────────────────
    private VBox buildUserCard() {
        VBox card = card("User Management");
        VBox form = new VBox(14);
        form.setPadding(new Insets(20));

        // User table
        TableView<User> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(svc.getUsers()));
        table.setPrefHeight(200);

        TableColumn<User, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        usernameCol.setPrefWidth(120);

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        roleCol.setPrefWidth(100);

        table.getColumns().addAll(usernameCol, roleCol);

        // Buttons
        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("Add User");
        addBtn.getStyleClass().add("primary-btn");
        addBtn.setOnAction(e -> showAddUserDialog(table));

        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("secondary-btn");
        removeBtn.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Remove user '" + selected.getUsername() + "'?", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
                    svc.removeUser(selected.getUsername());
                    table.setItems(FXCollections.observableArrayList(svc.getUsers()));
                });
            }
        });

        btns.getChildren().addAll(addBtn, removeBtn);

        form.getChildren().addAll(table, btns);
        card.getChildren().add(form);
        return card;
    }

    private void showAddUserDialog(TableView<User> table) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Enter user details");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        TextField role = new TextField();
        role.setPromptText("Role");
        role.setText("Staff");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);
        grid.add(new Label("Role:"), 0, 2);
        grid.add(role, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return new User(username.getText(), password.getText(), role.getText());
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(user -> {
            try {
                svc.addUser(user);
                table.setItems(FXCollections.observableArrayList(svc.getUsers()));
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
            }
        });
    }

    // ── About ────────────────────────────────────────────────────────────────
    private VBox buildAboutCard() {
        VBox card = card("About");
        VBox body = new VBox(8);
        body.setPadding(new Insets(20));

        String[][] rows = {
                { "Application", "Grand Vista Hotel Management System" },
                { "Version", "1.0.0" },
                { "Platform", "JavaFX 21 · Java 17" },
                { "Build Tool", "Apache Maven 3.9+" },
                { "Author", "Grand Vista Engineering" },
                { "License", "MIT License" },
                { "Description",
                        "A professional hotel management system featuring room\nmanagement, bookings, guest tracking, and analytics." }
        };

        for (String[] row : rows) {
            HBox r = new HBox(16);
            r.setAlignment(Pos.TOP_LEFT);
            Label key = new Label(row[0] + ":");
            key.setMinWidth(100);
            key.getStyleClass().add("field-label");
            Label val = new Label(row[1]);
            val.setWrapText(true);
            val.getStyleClass().add("guest-stats");
            r.getChildren().addAll(key, val);
            body.getChildren().add(r);
        }

        card.getChildren().add(body);
        return card;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private VBox card(String heading) {
        VBox card = new VBox(0);
        card.getStyleClass().add("dash-card");

        HBox header = new HBox();
        header.getStyleClass().add("card-header-bar");
        header.setPadding(new Insets(14, 18, 14, 18));
        Label h = new Label(heading);
        h.getStyleClass().add("card-heading");
        header.getChildren().add(h);

        Separator sep = new Separator();
        card.getChildren().addAll(header, sep);
        return card;
    }

    private VBox fieldGroup(String label, Control ctrl) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("field-label");
        ctrl.setMaxWidth(Double.MAX_VALUE);
        return new VBox(5, lbl, ctrl);
    }

    private TextField input(String prompt) {
        TextField tf = new TextField(prompt);
        tf.getStyleClass().add("hotel-input");
        return tf;
    }
}
