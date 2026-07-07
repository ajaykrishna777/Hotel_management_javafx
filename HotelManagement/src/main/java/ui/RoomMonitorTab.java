package ui;

import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import model.Room;
import model.Room.RoomStatus;
import service.HotelService;

import java.util.Optional;

public class RoomMonitorTab {

    private final HotelService svc = HotelService.getInstance();
    private final Node content;

    public RoomMonitorTab() {
        content = build();
    }

    public Node getContent() {
        return content;
    }

    private Node build() {
        VBox page = new VBox(0);
        page.getStyleClass().add("dash-page");
        page.setFillWidth(true);
        VBox.setVgrow(page, Priority.ALWAYS);

        // Header section
        VBox header = new VBox(8);
        header.setPadding(new Insets(20, 24, 16, 24));

        HBox titleBar = new HBox(16);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Room Monitor");
        title.getStyleClass().add("page-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label hint = new Label("Click a room to view details");
        hint.getStyleClass().add("card-subheading");
        titleBar.getChildren().addAll(title, sp, hint);

        // Legend
        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(
                legendItem("Available", "#22C55E"),
                legendItem("Booked", "#F97316"),
                legendItem("Maintenance", "#EF4444"));

        header.getChildren().addAll(titleBar, legend);

        // Room grid - make it fill available space
        FlowPane grid = new FlowPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPadding(new Insets(16, 24, 24, 24));
        grid.setPrefWrapLength(Region.USE_PREF_SIZE); // Allow wrapping
        grid.setOrientation(Orientation.HORIZONTAL);

        svc.getAllRooms().forEach(r -> grid.getChildren().add(monitorCard(r)));

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("edge-to-edge");
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(true); // Allow panning
        VBox.setVgrow(scroll, Priority.ALWAYS);

        page.getChildren().addAll(header, scroll);
        return page;
    }

    private Node monitorCard(Room r) {
        VBox card = new VBox(8);
        card.setPrefWidth(160);
        card.setMinWidth(160);
        card.setMaxWidth(160);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER);
        
        // Color scheme based on status
        String bgColor, borderColor, statusColor, iconText;
        switch (r.getStatus()) {
            case AVAILABLE:
                bgColor = "#F0FDF4";
                borderColor = "#22C55E";
                statusColor = "#15803D";
                iconText = "AVL";
                break;
            case BOOKED:
                bgColor = "#FFF7ED";
                borderColor = "#F97316";
                statusColor = "#C2410C";
                iconText = "BKD";
                break;
            case MAINTENANCE:
                bgColor = "#FEF2F2";
                borderColor = "#EF4444";
                statusColor = "#B91C1C";
                iconText = "MTN";
                break;
            default:
                bgColor = "#F9FAFB";
                borderColor = "#9CA3AF";
                statusColor = "#6B7280";
                iconText = "UNK";
        }
        
        card.setStyle("-fx-background-color:" + bgColor + "; -fx-border-color:" + borderColor
                + "; -fx-border-radius:12; -fx-background-radius:12; -fx-border-width:2; -fx-cursor:hand; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.08), 6, 0, 0, 2);");

        // Room number with icon
        HBox numberRow = new HBox(8);
        numberRow.setAlignment(Pos.CENTER);
        
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(32, 32);
        iconContainer.setMaxSize(32, 32);
        
        Circle iconBg = new Circle(16, Color.web(borderColor + "20"));
        Label iconLabel = new Label(iconText);
        iconLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + borderColor + ";");
        iconContainer.getChildren().addAll(iconBg, iconLabel);
        
        Label num = new Label("Room " + r.getRoomNumber());
        num.setStyle("-fx-font-weight:800; -fx-font-size:15px; -fx-text-fill:#1C1C2E;");
        
        numberRow.getChildren().addAll(iconContainer, num);

        // Room type
        Label type = new Label(r.getRoomType().name());
        type.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B; -fx-font-weight: 500;");

        // Price
        HBox priceRow = new HBox(4);
        priceRow.setAlignment(Pos.CENTER);
        Label priceIcon = new Label("₹");
        priceIcon.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill:" + borderColor + ";");
        Label price = new Label(String.format("%.0f", r.getPricePerNight()));
        price.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill:" + borderColor + ";");
        priceRow.getChildren().addAll(priceIcon, price);

        // Status indicator
        HBox statusRow = new HBox(6);
        statusRow.setAlignment(Pos.CENTER);
        
        Circle statusDot = new Circle(4, Color.web(borderColor));
        Label status = new Label(r.getStatus().name());
        status.setStyle("-fx-font-size: 11px; -fx-text-fill:" + statusColor + "; -fx-font-weight: 600;");
        
        statusRow.getChildren().addAll(statusDot, status);

        card.getChildren().addAll(numberRow, type, priceRow, new Separator(Orientation.HORIZONTAL), statusRow);

        // Enhanced tooltip
        Tooltip tooltip = new Tooltip();
        tooltip.setStyle("-fx-font-size: 12px;");
        tooltip.setText(
                "Room: " + r.getRoomNumber() + "\n" +
                "Type: " + r.getRoomType() + "\n" +
                "Floor: " + r.getFloor() + "\n" +
                "Capacity: " + r.getCapacity() + " guests\n" +
                "Price: ₹" + String.format("%.0f", r.getPricePerNight()) + "/night\n" +
                "Status: " + r.getStatus() + "\n" +
                "Description: " + (r.getDescription().isEmpty() ? "No description" : r.getDescription())
        );
        Tooltip.install(card, tooltip);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color:" + bgColor + "; -fx-border-color:" + borderColor
                    + "; -fx-border-radius:12; -fx-background-radius:12; -fx-border-width:2; -fx-cursor:hand; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.15), 12, 0, 0, 4); -fx-scale-x: 1.02; -fx-scale-y: 1.02;");
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color:" + bgColor + "; -fx-border-color:" + borderColor
                    + "; -fx-border-radius:12; -fx-background-radius:12; -fx-border-width:2; -fx-cursor:hand; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.08), 6, 0, 0, 2);");
        });

        card.setOnMouseClicked(e -> showRoomStatusDialog(r));

        return card;
    }

    private void showRoomStatusDialog(Room r) {
        Dialog<RoomStatus> dialog = new Dialog<>();
        dialog.setTitle("Update Room Status");
        dialog.setHeaderText("Room " + r.getRoomNumber() + " - " + r.getRoomType());

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        Label current = new Label("Current Status: " + r.getStatus());
        current.getStyleClass().add("guest-stats");

        ComboBox<RoomStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(RoomStatus.values());
        statusCombo.setValue(r.getStatus());

        // Cleaning status section
        Label cleaningLabel = new Label("Cleaning Status:");
        cleaningLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");
        
        ToggleGroup cleaningGroup = new ToggleGroup();
        RadioButton cleanRadio = new RadioButton("Clean & Ready");
        cleanRadio.setToggleGroup(cleaningGroup);
        RadioButton cleaningRadio = new RadioButton("Cleaning in Progress");
        cleaningRadio.setToggleGroup(cleaningGroup);
        RadioButton needsCleaningRadio = new RadioButton("Needs Cleaning");
        needsCleaningRadio.setToggleGroup(cleaningGroup);
        
        // Set default based on room status
        if (r.getStatus() == RoomStatus.AVAILABLE) {
            cleanRadio.setSelected(true);
        } else {
            needsCleaningRadio.setSelected(true);
        }

        // Notes section
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Add notes about room condition or cleaning requirements...");
        notesArea.setPrefHeight(60);
        notesArea.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(current, new Label("New Status:"), statusCombo, 
                                  new Separator(), cleaningLabel, cleanRadio, cleaningRadio, needsCleaningRadio,
                                  new Label("Notes:"), notesArea);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                // Handle cleaning status logic
                RoomStatus newStatus = statusCombo.getValue();
                String selectedCleaning = ((RadioButton) cleaningGroup.getSelectedToggle()).getText();
                
                // Auto-update status based on cleaning selection
                if (selectedCleaning.equals("Clean & Ready") && newStatus == RoomStatus.MAINTENANCE) {
                    return RoomStatus.AVAILABLE;
                } else if (selectedCleaning.equals("Needs Cleaning") && newStatus == RoomStatus.AVAILABLE) {
                    return RoomStatus.MAINTENANCE;
                }
                
                return newStatus;
            }
            return null;
        });

        Optional<RoomStatus> result = dialog.showAndWait();
        result.ifPresent(newStatus -> {
            try {
                if (newStatus == RoomStatus.MAINTENANCE) {
                    svc.setRoomMaintenance(r.getRoomNumber(), true);
                } else if (newStatus == RoomStatus.AVAILABLE) {
                    svc.setRoomMaintenance(r.getRoomNumber(), false);
                } else {
                    // For booked, perhaps not allow manual change, or add logic
                    r.setStatus(newStatus);
                }
                // Refresh somehow, but since it's static, perhaps reload the tab
                // For now, just update
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
            }
        });
    }

    private HBox legendItem(String label, String color) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER_LEFT);
        Rectangle swatch = new Rectangle(12, 12);
        swatch.setFill(Color.web(color));
        swatch.setArcWidth(3);
        swatch.setArcHeight(3);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B;");
        item.getChildren().addAll(swatch, lbl);
        return item;
    }
}
