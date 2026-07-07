package main;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import ui.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainApp extends Application {

    private BorderPane root;
    private StackPane contentHost;
    private Label clockLabel;
    private Label userLabel;
    private Button logoutBtn;
    private Stage primaryStage;

    private DashboardTab dashboardTab;
    private RoomTab roomTab;
    private BookingTab bookingTab;
    private CheckoutTab checkoutTab;
    private GuestsTab guestsTab;
    private ReportsTab reportsTab;
    private SettingsTab settingsTab;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // Show login dialog first
        if (!LoginDialog.showLoginDialog(stage)) {
            stage.close();
            return;
        }

        initializeTabs();
        buildUI();

        stage.setTitle("Grand Vista Hotel — Management System");
        stage.setScene(new Scene(root, 1280, 800));
        stage.setMinWidth(1100);
        stage.setMinHeight(680);

        // Load CSS after scene is set
        String css = getClass().getResource("/css/style.css").toExternalForm();
        stage.getScene().getStylesheets().add(css);

        stage.show();

        // Live clock
        javafx.animation.Timeline clock = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1),
                        e -> updateClock()));
        clock.setCycleCount(javafx.animation.Animation.INDEFINITE);
        clock.play();
    }

    private void initializeTabs() {
        dashboardTab = new DashboardTab();
        roomTab = new RoomTab(this::onDataChanged);
        bookingTab = new BookingTab(this::onDataChanged, dashboardTab);
        checkoutTab = new CheckoutTab(this::onDataChanged, dashboardTab);
        guestsTab = new GuestsTab();
        reportsTab = new ReportsTab();
        settingsTab = new SettingsTab(this::onDataChanged);
    }

    private void buildUI() {
        root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());

        contentHost = new StackPane();
        contentHost.getStyleClass().add("content-host");
        root.setCenter(contentHost);

        showTab("dashboard");
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(0);
        bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        VBox logo = new VBox(1);
        logo.getStyleClass().add("logo-area");
        logo.setPadding(new Insets(0, 24, 0, 24));
        logo.setAlignment(Pos.CENTER_LEFT);
        Label logoMain = new Label("GRAND VISTA");
        logoMain.getStyleClass().add("logo-main");
        Label logoSub = new Label("HOTEL MANAGEMENT");
        logoSub.getStyleClass().add("logo-sub");
        logo.getChildren().addAll(logoMain, logoSub);

        Rectangle divider = new Rectangle(1, 36);
        divider.setStyle("-fx-fill: #E5E7EB;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox rightInfo = new HBox(20);
        rightInfo.setPadding(new Insets(0, 24, 0, 16));
        rightInfo.setAlignment(Pos.CENTER);

        clockLabel = new Label();
        clockLabel.getStyleClass().add("top-clock");
        updateClock();

        VBox userInfo = new VBox(1);
        userInfo.setAlignment(Pos.CENTER_RIGHT);
        userLabel = new Label(LoginDialog.getLoggedInUser());
        userLabel.getStyleClass().add("manager-name");
        Label userRole = new Label(LoginDialog.getLoggedInUserRole());
        userRole.getStyleClass().add("manager-role");
        userInfo.getChildren().addAll(userLabel, userRole);

        logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().add("logout-btn");
        logoutBtn.setOnAction(e -> handleLogout());

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("avatar");
        Label initials = new Label("GV");
        initials.getStyleClass().add("avatar-text");
        avatar.getChildren().add(initials);
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);

        rightInfo.getChildren().addAll(clockLabel, userInfo, logoutBtn, avatar);
        bar.getChildren().addAll(logo, divider, spacer, rightInfo);
        return bar;
    }

    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Sign Out");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to sign out?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            LoginDialog.logout();
            primaryStage.close();
            start(new Stage());
        }
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(240);
        sidebar.setMinWidth(240);

        ToggleGroup nav = new ToggleGroup();

        Label sectionLabel1 = sidebarSection("OVERVIEW");
        ToggleButton btnDash = navBtn("Dashboard", "dashboard", nav);
        ToggleButton btnRooms = navBtn("Rooms", "rooms", nav);

        Label sectionLabel2 = sidebarSection("OPERATIONS");
        ToggleButton btnBooking = navBtn("New Booking", "booking", nav);
        ToggleButton btnCheckout = navBtn("Check Out", "checkout", nav);
        ToggleButton btnGuests = navBtn("Guests", "guests", nav);
        ToggleButton btnMonitor = navBtn("Room Monitor", "monitor", nav);

        Label sectionLabel3 = sidebarSection("ANALYTICS");
        ToggleButton btnReports = navBtn("Reports", "reports", nav);
        ToggleButton btnSettings = navBtn("Settings", "settings", nav);

        btnDash.setSelected(true);

        Region flex = new Region();
        VBox.setVgrow(flex, Priority.ALWAYS);

        VBox versionBox = new VBox(2);
        versionBox.getStyleClass().add("sidebar-version");
        versionBox.setPadding(new Insets(16, 20, 16, 20));
        versionBox.getChildren().add(new Label()); // Empty version box

        sidebar.getChildren().addAll(
                sectionLabel1, btnDash, btnRooms,
                sectionLabel2, btnBooking, btnCheckout, btnGuests, btnMonitor,
                sectionLabel3, btnReports, btnSettings,
                flex, versionBox);
        return sidebar;
    }

    private Label sidebarSection(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("sidebar-section");
        lbl.setPadding(new Insets(24, 20, 8, 20));
        return lbl;
    }

    private ToggleButton navBtn(String label, String tabId, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(label);
        btn.setToggleGroup(group);
        btn.getStyleClass().add("nav-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(12, 20, 12, 20));
        btn.setOnAction(e -> {
            if (!btn.isSelected())
                btn.setSelected(true);
            showTab(tabId);
        });
        return btn;
    }

    private void showTab(String id) {
        contentHost.getChildren().clear();
        switch (id) {
            case "dashboard" -> {
                dashboardTab.refresh();
                contentHost.getChildren().add(dashboardTab.getContent());
            }
            case "rooms" -> {
                roomTab.refresh();
                contentHost.getChildren().add(roomTab.getContent());
            }
            case "booking" -> {
                bookingTab.refreshAll();
                contentHost.getChildren().add(bookingTab.getContent());
            }
            case "checkout" -> {
                checkoutTab.refreshAll();
                contentHost.getChildren().add(checkoutTab.getContent());
            }
            case "guests" -> {
                guestsTab.refresh();
                contentHost.getChildren().add(guestsTab.getContent());
            }
            case "monitor" -> {
                contentHost.getChildren().add(new RoomMonitorTab().getContent());
            }
            case "reports" -> {
                reportsTab.refresh();
                contentHost.getChildren().add(reportsTab.getContent());
            }
            case "settings" -> contentHost.getChildren().add(settingsTab.getContent());
        }
    }

    private void onDataChanged() {
        dashboardTab.refresh();
        roomTab.refresh();
        bookingTab.refreshAll();
        checkoutTab.refreshAll();
        guestsTab.refresh();
        reportsTab.refresh();
    }

    private void updateClock() {
        if (clockLabel != null)
            clockLabel.setText(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  HH:mm:ss")));
    }

    public static void main(String[] args) {
        launch(args);
    }
}