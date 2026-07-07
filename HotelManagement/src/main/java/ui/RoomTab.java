package ui;

import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import model.Room;
import model.Room.RoomStatus;
import model.Room.RoomType;
import service.HotelService;

import java.util.List;

public class RoomTab {

    private final HotelService svc = HotelService.getInstance();
    private Runnable onDataChanged;

    private TableView<Room> roomTable;
    private TextField searchField;
    private ToggleButton filterAll, filterAvail, filterBooked, filterMaint;
    private VBox addPanel;
    private TextField roomNumField, priceField, descField, floorField, capacityField;
    private ComboBox<RoomType> typeCombo;
    private ComboBox<RoomStatus> statusCombo;
    private RoomStatus activeFilter = null;

    private final BorderPane rootPane;

    public RoomTab(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        rootPane = buildRoot();
        refresh();
    }

    public Node getContent() {
        return rootPane;
    }

    private BorderPane buildRoot() {
        BorderPane bp = new BorderPane();
        bp.getStyleClass().add("room-root");
        bp.setTop(buildToolbar());

        roomTable = new TableView<>();
        roomTable.getStyleClass().add("hotel-table");
        roomTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Room, String> numCol = new TableColumn<>("Room");
        numCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getRoomNumber()));
        numCol.setPrefWidth(80);

        TableColumn<Room, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getRoomType().name()));

        TableColumn<Room, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus().name()));
        statusCol.setCellFactory(col -> new TableCell<Room, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: " + getStatusColor(item)
                            + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 12px; -fx-alignment: center;");
                }
            }
        });

        TableColumn<Room, String> priceCol = new TableColumn<>("Price/Night");
        priceCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                "₹" + String.format("%.0f", data.getValue().getPricePerNight())));

        TableColumn<Room, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));

        TableColumn<Room, String> floorCol = new TableColumn<>("Floor");
        floorCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getFloor())));

        TableColumn<Room, String> capCol = new TableColumn<>("Capacity");
        capCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getCapacity())));

        TableColumn<Room, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<Room, Void>() {
            private final Button editBtn = new Button("Edit");
            private final Button maintBtn = new Button();
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(5, editBtn, maintBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("table-action-btn");
                maintBtn.getStyleClass().add("table-action-btn");
                deleteBtn.getStyleClass().addAll("table-action-btn", "danger-btn-small");

                editBtn.setOnAction(e -> showEditDialog(getTableView().getItems().get(getIndex())));
                maintBtn.setOnAction(e -> {
                    Room r = getTableView().getItems().get(getIndex());
                    svc.setRoomMaintenance(r.getRoomNumber(), r.getStatus() != RoomStatus.MAINTENANCE);
                    if (onDataChanged != null)
                        onDataChanged.run();
                    refresh();
                });
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Room r = getTableView().getItems().get(getIndex());
                    maintBtn.setText(r.getStatus() == RoomStatus.MAINTENANCE ? "Available" : "Maintenance");
                    setGraphic(pane);
                }
            }
        });

        roomTable.getColumns().addAll(numCol, typeCol, statusCol, priceCol, descCol, floorCol, capCol, actionsCol);

        ScrollPane scroll = new ScrollPane(roomTable);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        bp.setCenter(scroll);

        addPanel = buildAddPanel();
        addPanel.setVisible(false);
        addPanel.setManaged(false);
        addPanel.setPrefWidth(320);
        bp.setRight(addPanel);
        return bp;
    }

    private VBox buildToolbar() {
        VBox toolbarContainer = new VBox(12);
        toolbarContainer.setPadding(new Insets(16, 24, 16, 24));
        toolbarContainer.setStyle("-fx-background-color: white; -fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0;");

        // Top row: Title and stats
        HBox topRow = new HBox(20);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleSection = new VBox(4);
        Label title = new Label("Room Management");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage hotel rooms and availability");
        subtitle.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        titleSection.getChildren().addAll(title, subtitle);

        // Quick stats
        HBox quickStats = new HBox(12);
        quickStats.setAlignment(Pos.CENTER);
        quickStats.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8px; -fx-padding: 6 12 6 12;");

        Label totalRooms = new Label("Total: " + svc.getAllRooms().size());
        totalRooms.setStyle("-fx-text-fill: #374151; -fx-font-weight: 600; -fx-font-size: 12px;");

        Label availableRooms = new Label("Available: " + svc.getAvailableRooms().size());
        availableRooms.setStyle("-fx-text-fill: #059669; -fx-font-weight: 600; -fx-font-size: 12px;");

        Label bookedRooms = new Label("Booked: " + svc.getBookedRooms());
        bookedRooms.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: 600; -fx-font-size: 12px;");

        quickStats.getChildren().addAll(totalRooms, new Separator(Orientation.VERTICAL), availableRooms,
                new Separator(Orientation.VERTICAL), bookedRooms);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(titleSection, spacer, quickStats);

        // Bottom row: Controls
        HBox controlsRow = new HBox(12);
        controlsRow.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Search rooms by number, type, or status...");
        searchField.getStyleClass().add("hotel-input");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((o, v, n) -> applyFilter());

        ToggleGroup group = new ToggleGroup();
        filterAll = filterPill("All Rooms", group);
        filterAll.setSelected(true);
        filterAvail = filterPill("Available", group);
        filterBooked = filterPill("Booked", group);
        filterMaint = filterPill("Maintenance", group);

        filterAll.setOnAction(e -> {
            activeFilter = null;
            applyFilter();
        });
        filterAvail.setOnAction(e -> {
            activeFilter = RoomStatus.AVAILABLE;
            applyFilter();
        });
        filterBooked.setOnAction(e -> {
            activeFilter = RoomStatus.BOOKED;
            applyFilter();
        });
        filterMaint.setOnAction(e -> {
            activeFilter = RoomStatus.MAINTENANCE;
            applyFilter();
        });

        Button addBtn = new Button("Add Room");
        addBtn.getStyleClass().add("primary-btn");
        addBtn.setOnAction(e -> toggleAddPanel());

        Button calendarBtn = new Button("Calendar View");
        calendarBtn.getStyleClass().add("secondary-btn");
        calendarBtn.setOnAction(e -> showCalendarView());

        controlsRow.getChildren().addAll(searchField, filterAll, filterAvail, filterBooked, filterMaint, addBtn,
                calendarBtn);

        toolbarContainer.getChildren().addAll(topRow, controlsRow);
        return toolbarContainer;
    }

    private ToggleButton filterPill(String label, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(label);
        btn.setToggleGroup(group);
        btn.getStyleClass().add("filter-pill");
        return btn;
    }

    private VBox buildAddPanel() {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("add-panel");

        HBox header = new HBox();
        header.getStyleClass().add("form-header-band");
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        Label h = new Label("ADD NEW ROOM");
        h.getStyleClass().add("panel-heading-light");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("close-btn");
        closeBtn.setOnAction(e -> toggleAddPanel());
        header.getChildren().addAll(h, sp, closeBtn);

        VBox form = new VBox(14);
        form.setPadding(new Insets(20, 20, 20, 20));

        roomNumField = input("e.g. 201");
        priceField = input("Price per night in ₹");
        descField = input("Short description");
        floorField = input("Floor number");
        capacityField = input("Max guests");

        typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(RoomType.values());
        typeCombo.setValue(RoomType.STANDARD);
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.getStyleClass().add("hotel-combo");

        statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(RoomStatus.values());
        statusCombo.setValue(RoomStatus.AVAILABLE);
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        statusCombo.getStyleClass().add("hotel-combo");

        Button saveBtn = new Button("Save Room");
        saveBtn.getStyleClass().add("primary-btn");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> handleAddRoom());

        form.getChildren().addAll(
                field("ROOM NUMBER", roomNumField),
                field("ROOM TYPE", typeCombo),
                field("PRICE / NIGHT", priceField),
                field("STATUS", statusCombo),
                field("FLOOR", floorField),
                field("CAPACITY", capacityField),
                field("DESCRIPTION", descField),
                saveBtn);

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().addAll(header, scroll);
        return panel;
    }

    private VBox field(String label, Control ctrl) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("field-label");
        ctrl.setMaxWidth(Double.MAX_VALUE);
        return new VBox(5, lbl, ctrl);
    }

    private TextField input(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("hotel-input");
        return tf;
    }

    private void toggleAddPanel() {
        boolean show = !addPanel.isVisible();
        addPanel.setVisible(show);
        addPanel.setManaged(show);
    }

    private void handleAddRoom() {
        String num = roomNumField.getText().trim();
        String price = priceField.getText().trim();
        String desc = descField.getText().trim();
        String floor = floorField.getText().trim();
        String cap = capacityField.getText().trim();

        if (num.isEmpty() || price.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Missing Fields", "Room number and price are required.");
            return;
        }
        double p;
        int f, c;
        try {
            p = Double.parseDouble(price);
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Invalid Price", "Enter a valid number.");
            return;
        }
        try {
            f = floor.isEmpty() ? 1 : Integer.parseInt(floor);
        } catch (Exception e) {
            f = 1;
        }
        try {
            c = cap.isEmpty() ? 2 : Integer.parseInt(cap);
        } catch (Exception e) {
            c = 2;
        }

        try {
            Room room = new Room(num, typeCombo.getValue(), p, statusCombo.getValue(), desc, f, c);
            svc.addRoom(room);
            if (onDataChanged != null)
                onDataChanged.run();
            refresh();
            clearAddForm();
            toggleAddPanel();
            alert(Alert.AlertType.INFORMATION, "Room Added", "Room " + num + " added successfully.");
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Error", ex.getMessage());
        }
    }

    private void clearAddForm() {
        roomNumField.clear();
        priceField.clear();
        descField.clear();
        floorField.clear();
        capacityField.clear();
        typeCombo.setValue(RoomType.STANDARD);
        statusCombo.setValue(RoomStatus.AVAILABLE);
    }

    public void refresh() {
        applyFilter();
    }

    private void applyFilter() {
        String query = searchField != null ? searchField.getText().toLowerCase() : "";

        List<Room> filtered = svc.getAllRooms().stream()
                .filter(r -> activeFilter == null || r.getStatus() == activeFilter)
                .filter(r -> query.isEmpty()
                        || r.getRoomNumber().toLowerCase().contains(query)
                        || r.getRoomType().name().toLowerCase().contains(query)
                        || r.getDescription().toLowerCase().contains(query))
                .toList();

        roomTable.getItems().setAll(filtered);
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "AVAILABLE" -> "#22C55E";
            case "BOOKED" -> "#F97316";
            case "MAINTENANCE" -> "#EF4444";
            default -> "#6B7280";
        };
    }

    private void handleDelete(Room r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete Room " + r.getRoomNumber() + "? This cannot be undone.", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                svc.deleteRoom(r.getRoomNumber());
                if (onDataChanged != null)
                    onDataChanged.run();
                refresh();
            } catch (Exception e) {
                alert(Alert.AlertType.ERROR, "Cannot Delete", e.getMessage());
            }
        });
    }

    private void alert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showEditDialog(Room r) {
        Dialog<Room> dlg = new Dialog<>();
        dlg.setTitle("Edit Room " + r.getRoomNumber());
        dlg.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField priceF = new TextField(String.valueOf(r.getPricePerNight()));
        TextField descF = new TextField(r.getDescription());
        TextField floorF = new TextField(String.valueOf(r.getFloor()));
        TextField capF = new TextField(String.valueOf(r.getCapacity()));

        ComboBox<RoomType> typeC = new ComboBox<>();
        typeC.getItems().addAll(RoomType.values());
        typeC.setValue(r.getRoomType());

        ComboBox<RoomStatus> statC = new ComboBox<>();
        statC.getItems().addAll(RoomStatus.values());
        statC.setValue(r.getStatus());

        grid.add(new Label("Type:"), 0, 0);
        grid.add(typeC, 1, 0);
        grid.add(new Label("Price:"), 0, 1);
        grid.add(priceF, 1, 1);
        grid.add(new Label("Status:"), 0, 2);
        grid.add(statC, 1, 2);
        grid.add(new Label("Floor:"), 0, 3);
        grid.add(floorF, 1, 3);
        grid.add(new Label("Capacity:"), 0, 4);
        grid.add(capF, 1, 4);
        grid.add(new Label("Desc:"), 0, 5);
        grid.add(descF, 1, 5);

        dlg.getDialogPane().setContent(grid);
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dlg.setResultConverter(bt -> {
            if (bt == saveType) {
                try {
                    r.setRoomType(typeC.getValue());
                    r.setPricePerNight(Double.parseDouble(priceF.getText().trim()));
                    r.setStatus(statC.getValue());
                    r.setDescription(descF.getText().trim());
                    r.setFloor(Integer.parseInt(floorF.getText().trim()));
                    r.setCapacity(Integer.parseInt(capF.getText().trim()));
                    return r;
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });

        dlg.showAndWait().ifPresent(updated -> {
            try {
                svc.updateRoom(updated);
                if (onDataChanged != null)
                    onDataChanged.run();
                refresh();
            } catch (Exception e) {
                alert(Alert.AlertType.ERROR, "Error", e.getMessage());
            }
        });
    }

    private void showCalendarView() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Room Availability Calendar");
        dialog.setHeaderText("View room availability by date");

        // Create calendar content
        VBox calendarContent = new VBox(16);
        calendarContent.setPadding(new Insets(20));
        calendarContent.setPrefWidth(800);
        calendarContent.setPrefHeight(600);

        // Month/year selector
        HBox monthSelector = new HBox(16);
        monthSelector.setAlignment(Pos.CENTER);

        Button prevMonth = new Button("< Previous");
        Label monthLabel = new Label();
        monthLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        Button nextMonth = new Button("Next >");

        // Current month display
        java.time.LocalDate currentMonth = java.time.LocalDate.now();
        updateMonthLabel(monthLabel, currentMonth);

        prevMonth.setOnAction(e -> {
            java.time.LocalDate newMonth = currentMonth.minusMonths(1);
            updateMonthLabel(monthLabel, newMonth);
        });

        nextMonth.setOnAction(e -> {
            java.time.LocalDate newMonth = currentMonth.plusMonths(1);
            updateMonthLabel(monthLabel, newMonth);
        });

        monthSelector.getChildren().addAll(prevMonth, monthLabel, nextMonth);

        // Calendar grid
        GridPane calendarGrid = createCalendarGrid(currentMonth);

        // Legend
        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER);
        legend.getChildren().addAll(
                createLegendItem("Available", "#10B981"),
                createLegendItem("Booked", "#F59E0B"),
                createLegendItem("Maintenance", "#EF4444"));

        calendarContent.getChildren().addAll(monthSelector, calendarGrid, legend);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(calendarContent);
        dialogPane.getButtonTypes().addAll(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    private void updateMonthLabel(Label label, java.time.LocalDate date) {
        String monthName = date.getMonth().toString().charAt(0) +
                date.getMonth().toString().substring(1).toLowerCase();
        label.setText(monthName + " " + date.getYear());
    }

    private GridPane createCalendarGrid(java.time.LocalDate month) {
        GridPane grid = new GridPane();
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setPadding(new Insets(10));

        // Day headers
        String[] days = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(days[i]);
            dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #6B7280; -fx-alignment: center;");
            dayLabel.setPrefWidth(100);
            dayLabel.setAlignment(Pos.CENTER);
            grid.add(dayLabel, i, 0);
        }

        // Calendar days
        java.time.LocalDate firstDay = month.withDayOfMonth(1);
        int startDay = firstDay.getDayOfWeek().getValue() % 7;
        int daysInMonth = month.lengthOfMonth();

        int day = 1;
        for (int week = 1; week <= 6 && day <= daysInMonth; week++) {
            for (int dayOfWeek = 0; dayOfWeek < 7 && day <= daysInMonth; dayOfWeek++) {
                if (week == 1 && dayOfWeek < startDay) {
                    // Empty cells before month starts
                    continue;
                }

                VBox dayCell = createDayCell(day, month);
                grid.add(dayCell, dayOfWeek, week);
                day++;
            }
        }

        return grid;
    }

    private VBox createDayCell(int day, java.time.LocalDate month) {
        VBox cell = new VBox(4);
        cell.setPrefSize(100, 80);
        cell.setPadding(new Insets(4));
        cell.setStyle("-fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-background-color: white;");

        Label dayLabel = new Label(String.valueOf(day));
        dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

        // Room availability summary
        Label availabilityLabel = new Label(getAvailabilitySummary(day, month));
        availabilityLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6B7280;");

        cell.getChildren().addAll(dayLabel, availabilityLabel);
        return cell;
    }

    private String getAvailabilitySummary(int day, java.time.LocalDate month) {
        java.time.LocalDate date = month.withDayOfMonth(day);
        long available = svc.getAllRooms().stream()
                .filter(r -> r.getStatus() == Room.RoomStatus.AVAILABLE)
                .count();
        long total = svc.getAllRooms().size();
        return available + "/" + total + " available";
    }

    private HBox createLegendItem(String label, String color) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.CENTER);

        Rectangle colorBox = new Rectangle(16, 16);
        colorBox.setFill(javafx.scene.paint.Color.web(color));

        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");

        item.getChildren().addAll(colorBox, labelNode);
        return item;
    }
}
