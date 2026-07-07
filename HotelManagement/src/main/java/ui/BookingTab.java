package ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import model.Booking;
import model.Room;
import model.Room.RoomStatus;
import service.HotelService;

public class BookingTab {

    private final HotelService svc = HotelService.getInstance();
    private Runnable onDataChanged;
    private DashboardTab dashboard;

    private TextField nameField, phoneField, emailField, specialField;
    private ComboBox<String> paymentCombo;
    private Spinner<Integer> daysSpinner;
    private Label billPreviewAmt, priceInfoLabel;
    private FlowPane roomSelectorGrid;
    private String selectedRoomNumber;
    private Label selectedRoomLabel;

    private TableView<BookingRow> tableView;
    private final ObservableList<BookingRow> tableData = FXCollections.observableArrayList();

    private final SplitPane root;

    public BookingTab(Runnable onDataChanged) {
        this(onDataChanged, null);
    }

    public BookingTab(Runnable onDataChanged, DashboardTab dashboard) {
        this.onDataChanged = onDataChanged;
        this.dashboard = dashboard;
        root = new SplitPane(buildFormPane(), buildTablePane());
        root.setDividerPositions(0.44);
        refreshAll();
    }

    public Node getContent() {
        return root;
    }

    private ScrollPane buildFormPane() {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("booking-form-panel");

        HBox headerBand = new HBox();
        headerBand.getStyleClass().add("form-header-band");
        headerBand.setPadding(new Insets(18, 24, 18, 24));
        Label heading = new Label("NEW BOOKING");
        heading.getStyleClass().add("panel-heading-light");
        headerBand.getChildren().add(heading);

        VBox content = new VBox(18);
        content.setPadding(new Insets(24));

        // Step 1: Guest Details
        nameField = input("Guest's full name");
        phoneField = input("10-digit mobile number");
        emailField = input("Email address (optional)");
        content.getChildren().add(stepSection("1", "GUEST DETAILS",
                group("FULL NAME", nameField),
                group("PHONE NUMBER", phoneField),
                group("EMAIL", emailField)));

        // Step 2: Room Selection
        content.getChildren().add(buildRoomSelectorSection());

        // Step 3: Stay Details
        daysSpinner = new Spinner<>(1, 365, 1);
        daysSpinner.setEditable(true);
        daysSpinner.getStyleClass().add("hotel-spinner");
        daysSpinner.valueProperty().addListener((o, v, n) -> recalcBill());

        specialField = input("Any special requests...");
        paymentCombo = new ComboBox<>();
        paymentCombo.getItems().addAll("CASH", "CARD", "UPI", "BANK TRANSFER");
        paymentCombo.setValue("CASH");
        paymentCombo.setMaxWidth(Double.MAX_VALUE);
        paymentCombo.getStyleClass().add("hotel-combo");

        content.getChildren().add(stepSection("3", "STAY DETAILS",
                group("NIGHTS", daysSpinner),
                group("PAYMENT METHOD", paymentCombo),
                group("SPECIAL REQUESTS", specialField)));

        // Bill Preview
        content.getChildren().add(buildBillPreview());

        Button bookBtn = new Button("Confirm Booking");
        bookBtn.getStyleClass().add("primary-btn");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setOnAction(e -> handleBook());

        Button clearBtn = new Button("Reset Form");
        clearBtn.getStyleClass().add("secondary-btn");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> clearForm());

        content.getChildren().addAll(bookBtn, clearBtn);
        panel.getChildren().addAll(headerBand, content);

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("edge-to-edge");
        return sp;
    }

    private VBox buildRoomSelectorSection() {
        VBox section = new VBox(10);
        HBox header = stepHeader("2", "SELECT ROOM");

        selectedRoomLabel = new Label("No room selected");
        selectedRoomLabel.getStyleClass().add("selected-room-hint");

        roomSelectorGrid = new FlowPane();
        roomSelectorGrid.setHgap(8);
        roomSelectorGrid.setVgap(8);
        roomSelectorGrid.getStyleClass().add("room-selector-grid");

        section.getChildren().addAll(header, selectedRoomLabel, roomSelectorGrid);
        return section;
    }

    private Node buildRoomCard(Room r) {
        VBox card = new VBox(5);
        card.getStyleClass().add("room-select-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setMinWidth(110);
        card.setMaxWidth(125);

        Label num = new Label("Room " + r.getRoomNumber());
        num.getStyleClass().add("sel-card-num");
        Label type = new Label(r.getRoomType().name());
        type.getStyleClass().add("sel-card-type");
        Label price = new Label("₹" + String.format("%.0f", r.getPricePerNight()));
        price.getStyleClass().add("sel-card-price");
        card.getChildren().addAll(num, type, price);

        card.setOnMouseClicked(e -> selectRoom(r, card));
        card.setOnMouseEntered(e -> {
            if (!r.getRoomNumber().equals(selectedRoomNumber))
                card.getStyleClass().add("room-select-card-hover");
        });
        card.setOnMouseExited(e -> card.getStyleClass().remove("room-select-card-hover"));
        return card;
    }

    private void selectRoom(Room r, VBox clicked) {
        roomSelectorGrid.getChildren().forEach(n -> n.getStyleClass().remove("room-select-card-active"));
        clicked.getStyleClass().add("room-select-card-active");
        selectedRoomNumber = r.getRoomNumber();
        selectedRoomLabel.setText("✦  Room " + r.getRoomNumber() + " — " + r.getRoomType().name());
        selectedRoomLabel.setStyle("-fx-text-fill:#F97316; -fx-font-weight:bold;");
        recalcBill();
    }

    private VBox buildBillPreview() {
        VBox card = new VBox(8);
        card.getStyleClass().add("bill-preview-card");
        card.setPadding(new Insets(18, 20, 18, 20));

        Label title = new Label("ESTIMATED BILL");
        title.getStyleClass().add("bill-card-title");

        billPreviewAmt = new Label("₹0");
        billPreviewAmt.getStyleClass().add("bill-amount");

        priceInfoLabel = new Label("Select a room and set duration");
        priceInfoLabel.getStyleClass().add("bill-note");

        card.getChildren().addAll(title, billPreviewAmt, priceInfoLabel);
        return card;
    }

    private VBox buildTablePane() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("table-panel");
        panel.setPadding(new Insets(24, 28, 28, 24));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label heading = new Label("Booking History");
        heading.getStyleClass().add("page-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button refreshBtn = new Button("⟳  Refresh");
        refreshBtn.getStyleClass().add("secondary-btn");
        refreshBtn.setOnAction(e -> refreshTable());
        header.getChildren().addAll(heading, sp, refreshBtn);

        tableView = buildTable();
        VBox.setVgrow(tableView, Priority.ALWAYS);
        panel.getChildren().addAll(header, tableView);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private TableView<BookingRow> buildTable() {
        TableView<BookingRow> tv = new TableView<>(tableData);
        tv.getStyleClass().add("hotel-table");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setPlaceholder(new Label("No bookings yet. Create your first booking →"));

        tv.getColumns().addAll(
                col("ID", "bookingId", 90),
                col("GUEST", "customerName", 130),
                col("PHONE", "phone", 110),
                col("ROOM", "roomNumber", 60),
                col("NIGHTS", "days", 55),
                col("CHECK-IN", "checkIn", 95),
                col("CHECK-OUT", "checkOut", 95),
                col("TOTAL", "totalBill", 90),
                col("PAYMENT", "payment", 80),
                col("STATUS", "status", 90));

        TableColumn<BookingRow, String> statusCol = (TableColumn<BookingRow, String>) tv.getColumns().get(9);
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-active", "status-out");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                getStyleClass().add(item.equals("ACTIVE") ? "status-active" : "status-out");
            }
        });
        return tv;
    }

    private <T> TableColumn<BookingRow, T> col(String t, String prop, double min) {
        TableColumn<BookingRow, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setMinWidth(min);
        return c;
    }

    private void handleBook() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String special = specialField.getText().trim();
        String payment = paymentCombo.getValue();

        if (name.isEmpty() || phone.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Incomplete", "Guest name and phone are required.");
            return;
        }
        if (!phone.matches("\\d{10,}")) {
            alert(Alert.AlertType.WARNING, "Invalid Phone", "Phone must be at least 10 digits.");
            return;
        }
        if (selectedRoomNumber == null) {
            alert(Alert.AlertType.WARNING, "No Room", "Please select a room.");
            return;
        }
        try {
            Booking b = svc.bookRoom(name, phone, email, selectedRoomNumber,
                    daysSpinner.getValue(), special, payment);
            refreshAll();
            if (onDataChanged != null)
                onDataChanged.run();
            if (dashboard != null)
                dashboard.addActivity(
                        DashboardTab.ActivityType.BOOKING,
                        "Room " + b.getRoomNumber() + " booked",
                        b.getCustomerName() + " · " + b.getNumberOfDays() + " night(s)");
            alert(Alert.AlertType.INFORMATION, "Booking Confirmed",
                    "Booking ID: " + b.getBookingId()
                            + "\nRoom: " + b.getRoomNumber()
                            + "\nGuest: " + b.getCustomerName()
                            + "\nTotal: ₹" + String.format("%.0f", b.getTotalBill()));
            clearForm();
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Booking Failed", ex.getMessage());
        }
    }

    private void recalcBill() {
        if (selectedRoomNumber == null)
            return;
        svc.findRoom(selectedRoomNumber).ifPresent(r -> {
            double total = r.getPricePerNight() * daysSpinner.getValue();
            billPreviewAmt.setText(String.format("₹%.0f", total));
            priceInfoLabel.setText(r.getRoomType().name() + " · ₹"
                    + String.format("%.0f", r.getPricePerNight()) + "/night × "
                    + daysSpinner.getValue() + " nights");
        });
    }

    private void clearForm() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        specialField.clear();
        selectedRoomNumber = null;
        selectedRoomLabel.setText("No room selected");
        selectedRoomLabel.setStyle("");
        daysSpinner.getValueFactory().setValue(1);
        billPreviewAmt.setText("₹0");
        priceInfoLabel.setText("Select a room and set duration");
        paymentCombo.setValue("CASH");
        refreshRoomGrid();
    }

    public void refreshAll() {
        refreshRoomGrid();
        refreshTable();
    }

    private void refreshRoomGrid() {
        if (roomSelectorGrid == null)
            return;
        roomSelectorGrid.getChildren().clear();
        svc.getRoomsByStatus(RoomStatus.AVAILABLE)
                .forEach(r -> roomSelectorGrid.getChildren().add(buildRoomCard(r)));
        if (roomSelectorGrid.getChildren().isEmpty()) {
            Label none = new Label("No available rooms at the moment.");
            none.getStyleClass().add("empty-state-small");
            roomSelectorGrid.getChildren().add(none);
        }
    }

    private void refreshTable() {
        tableData.clear();
        svc.getAllBookings().forEach(b -> tableData.add(new BookingRow(
                b.getBookingId(),
                b.getCustomerName(),
                b.getCustomerPhone(),
                b.getRoomNumber(),
                String.valueOf(b.getNumberOfDays()),
                b.getCheckInDate().toString(),
                b.getCheckOutDate().toString(),
                String.format("₹%.0f", b.getTotalBill()),
                b.getPaymentMethod(),
                b.isCheckedOut() ? "CHECKED OUT" : "ACTIVE")));
    }

    // Helpers
    private VBox stepSection(String num, String title, VBox... groups) {
        VBox section = new VBox(12);
        section.getChildren().add(stepHeader(num, title));
        section.getChildren().addAll(groups);
        return section;
    }

    private HBox stepHeader(String num, String title) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        StackPane circle = new StackPane();
        circle.getStyleClass().add("step-circle");
        Label numLbl = new Label(num);
        numLbl.getStyleClass().add("step-num");
        circle.getChildren().add(numLbl);
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("step-title");
        row.getChildren().addAll(circle, titleLbl);
        return row;
    }

    private TextField input(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("hotel-input");
        return tf;
    }

    private VBox group(String label, Control ctrl) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("field-label");
        ctrl.setMaxWidth(Double.MAX_VALUE);
        return new VBox(5, lbl, ctrl);
    }

    private void alert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public static class BookingRow {
        private final SimpleStringProperty bookingId, customerName, phone, roomNumber,
                days, checkIn, checkOut, totalBill, payment, status;

        BookingRow(String i, String n, String p, String r, String d,
                String ci, String co, String b, String pay, String s) {
            bookingId = new SimpleStringProperty(i);
            customerName = new SimpleStringProperty(n);
            phone = new SimpleStringProperty(p);
            roomNumber = new SimpleStringProperty(r);
            days = new SimpleStringProperty(d);
            checkIn = new SimpleStringProperty(ci);
            checkOut = new SimpleStringProperty(co);
            totalBill = new SimpleStringProperty(b);
            payment = new SimpleStringProperty(pay);
            status = new SimpleStringProperty(s);
        }

        public String getBookingId() {
            return bookingId.get();
        }

        public String getCustomerName() {
            return customerName.get();
        }

        public String getPhone() {
            return phone.get();
        }

        public String getRoomNumber() {
            return roomNumber.get();
        }

        public String getDays() {
            return days.get();
        }

        public String getCheckIn() {
            return checkIn.get();
        }

        public String getCheckOut() {
            return checkOut.get();
        }

        public String getTotalBill() {
            return totalBill.get();
        }

        public String getPayment() {
            return payment.get();
        }

        public String getStatus() {
            return status.get();
        }
    }
}