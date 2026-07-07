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
import service.HotelService;

import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CheckoutTab {

    private final HotelService svc = HotelService.getInstance();
    private Runnable onDataChanged;
    private DashboardTab dashboard;

    private ComboBox<String> bookingCombo;
    private Label rcptBookingId, rcptGuestName, rcptPhone, rcptEmail;
    private Label rcptRoom, rcptRoomType, rcptRate;
    private Label rcptCheckIn, rcptCheckOut, rcptNights, rcptTotal, rcptPayment;

    private Booking currentBooking;

    private TableView<CheckoutRow> tableView;
    private final ObservableList<CheckoutRow> tableData = FXCollections.observableArrayList();

    private final SplitPane root;

    public CheckoutTab(Runnable onDataChanged) {
        this(onDataChanged, null);
    }

    public CheckoutTab(Runnable onDataChanged, DashboardTab dashboard) {
        this.onDataChanged = onDataChanged;
        this.dashboard = dashboard;
        root = new SplitPane(buildLeftPanel(), buildRightPanel());
        root.setDividerPositions(0.44);
        refreshAll();
    }

    public Node getContent() {
        return root;
    }

    private ScrollPane buildLeftPanel() {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("booking-form-panel");

        HBox headerBand = new HBox();
        headerBand.getStyleClass().add("form-header-band");
        headerBand.setPadding(new Insets(18, 24, 18, 24));
        Label heading = new Label("CHECK OUT GUEST");
        heading.getStyleClass().add("panel-heading-light");
        headerBand.getChildren().add(heading);

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));

        // Booking selector
        bookingCombo = new ComboBox<>();
        bookingCombo.setMaxWidth(Double.MAX_VALUE);
        bookingCombo.setPromptText("Select Booking ID...");
        bookingCombo.getStyleClass().add("hotel-combo");
        bookingCombo.valueProperty().addListener((o, v, n) -> loadBookingDetails(n));

        VBox selectorSection = new VBox(8);
        Label selLabel = new Label("BOOKING ID");
        selLabel.getStyleClass().add("field-label");
        selectorSection.getChildren().addAll(selLabel, bookingCombo);
        content.getChildren().add(selectorSection);

        // Receipt Preview
        content.getChildren().add(buildReceiptPreview());

        // Actions
        Button checkoutBtn = new Button("✔  PROCESS CHECKOUT");
        checkoutBtn.getStyleClass().add("primary-btn");
        checkoutBtn.setMaxWidth(Double.MAX_VALUE);
        checkoutBtn.setOnAction(e -> handleCheckout());

        Button saveReceiptBtn = new Button("⤓  SAVE RECEIPT");
        saveReceiptBtn.getStyleClass().add("secondary-btn");
        saveReceiptBtn.setMaxWidth(Double.MAX_VALUE);
        saveReceiptBtn.setOnAction(e -> saveReceipt());

        content.getChildren().addAll(checkoutBtn, saveReceiptBtn);
        panel.getChildren().addAll(headerBand, content);

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("edge-to-edge");
        return sp;
    }

    private VBox buildReceiptPreview() {
        VBox card = new VBox(0);
        card.getStyleClass().add("receipt-card");

        HBox header = new HBox();
        header.getStyleClass().add("receipt-header");
        header.setPadding(new Insets(14, 18, 14, 18));
        Label title = new Label("INVOICE PREVIEW");
        title.getStyleClass().add("receipt-title");
        header.getChildren().add(title);

        VBox body = new VBox(0);
        body.getStyleClass().add("receipt-body");

        rcptBookingId = rcptLbl("—");
        rcptGuestName = rcptLbl("—");
        rcptPhone = rcptLbl("—");
        rcptEmail = rcptLbl("—");
        rcptRoom = rcptLbl("—");
        rcptRoomType = rcptLbl("—");
        rcptRate = rcptLbl("—");
        rcptCheckIn = rcptLbl("—");
        rcptCheckOut = rcptLbl("—");
        rcptNights = rcptLbl("—");
        rcptPayment = rcptLbl("—");
        rcptTotal = rcptLbl("₹0");
        rcptTotal.getStyleClass().add("receipt-total-val");

        body.getChildren().addAll(
                rcptRow("Booking ID", rcptBookingId),
                rcptRow("Guest Name", rcptGuestName),
                rcptRow("Phone", rcptPhone),
                rcptRow("Email", rcptEmail),
                new Separator(),
                rcptRow("Room No.", rcptRoom),
                rcptRow("Room Type", rcptRoomType),
                rcptRow("Rate/Night", rcptRate),
                new Separator(),
                rcptRow("Check-In", rcptCheckIn),
                rcptRow("Check-Out", rcptCheckOut),
                rcptRow("Nights", rcptNights),
                rcptRow("Payment", rcptPayment),
                new Separator(),
                rcptRowBold("TOTAL AMOUNT", rcptTotal));

        card.getChildren().addAll(header, body);
        return card;
    }

    private HBox rcptRow(String label, Label val) {
        HBox row = new HBox();
        row.getStyleClass().add("receipt-row");
        row.setPadding(new Insets(7, 18, 7, 18));
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("receipt-label");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        row.getChildren().addAll(lbl, sp, val);
        return row;
    }

    private HBox rcptRowBold(String label, Label val) {
        HBox row = rcptRow(label, val);
        row.getStyleClass().add("receipt-total-row");
        return row;
    }

    private Label rcptLbl(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("receipt-value");
        return l;
    }

    private VBox buildRightPanel() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("table-panel");
        panel.setPadding(new Insets(24, 28, 28, 24));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label heading = new Label("Active Bookings");
        heading.getStyleClass().add("page-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button refreshBtn = new Button("⟳  Refresh");
        refreshBtn.getStyleClass().add("secondary-btn");
        refreshBtn.setOnAction(e -> refreshAll());
        header.getChildren().addAll(heading, sp, refreshBtn);

        tableView = buildTable();
        VBox.setVgrow(tableView, Priority.ALWAYS);
        panel.getChildren().addAll(header, tableView);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private TableView<CheckoutRow> buildTable() {
        TableView<CheckoutRow> tv = new TableView<>(tableData);
        tv.getStyleClass().add("hotel-table");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setPlaceholder(new Label("No active bookings."));

        tv.getColumns().addAll(
                col("BOOKING ID", "bookingId", 90),
                col("GUEST", "guestName", 130),
                col("ROOM", "roomNumber", 60),
                col("CHECK-IN", "checkIn", 95),
                col("CHECK-OUT", "checkOut", 95),
                col("NIGHTS", "nights", 55),
                col("TOTAL", "total", 90));

        // Click to fill combo
        tv.setRowFactory(t -> {
            TableRow<CheckoutRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty())
                    bookingCombo.setValue(row.getItem().getBookingId());
            });
            return row;
        });
        return tv;
    }

    private <T> TableColumn<CheckoutRow, T> col(String t, String prop, double min) {
        TableColumn<CheckoutRow, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setMinWidth(min);
        return c;
    }

    private void loadBookingDetails(String bookingId) {
        if (bookingId == null)
            return;
        svc.findBooking(bookingId).ifPresent(b -> {
            currentBooking = b;
            svc.findRoom(b.getRoomNumber()).ifPresent(r -> {
                rcptBookingId.setText(b.getBookingId());
                rcptGuestName.setText(b.getCustomerName());
                rcptPhone.setText(b.getCustomerPhone());
                rcptEmail.setText(b.getCustomerEmail().isEmpty() ? "—" : b.getCustomerEmail());
                rcptRoom.setText(b.getRoomNumber());
                rcptRoomType.setText(r.getRoomType().name());
                rcptRate.setText("₹" + String.format("%.0f", r.getPricePerNight()) + "/night");
                rcptCheckIn.setText(b.getCheckInDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                rcptCheckOut.setText(b.getCheckOutDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                rcptNights.setText(String.valueOf(b.getNumberOfDays()));
                rcptPayment.setText(b.getPaymentMethod());
                rcptTotal.setText("₹" + String.format("%.0f", b.getTotalBill()));
            });
        });
    }

    private void handleCheckout() {
        String id = bookingCombo.getValue();
        if (id == null) {
            alert(Alert.AlertType.WARNING, "No Selection", "Please select a booking.");
            return;
        }
        try {
            Booking b = svc.checkout(id);
            if (onDataChanged != null)
                onDataChanged.run();
            if (dashboard != null)
                dashboard.addActivity(
                        DashboardTab.ActivityType.CHECKOUT,
                        "Room " + b.getRoomNumber() + " checked out",
                        b.getCustomerName() + " · ₹" + String.format("%.0f", b.getTotalBill()));
            alert(Alert.AlertType.INFORMATION, "Checkout Complete",
                    "Guest: " + b.getCustomerName()
                            + "\nRoom: " + b.getRoomNumber()
                            + "\nTotal Collected: ₹" + String.format("%.0f", b.getTotalBill()));
            refreshAll();
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Checkout Failed", ex.getMessage());
        }
    }

    private void saveReceipt() {
        if (currentBooking == null) {
            alert(Alert.AlertType.WARNING, "No Booking", "Select a booking first.");
            return;
        }
        Booking b = currentBooking;
        String content = """
                ==========================================
                  GRAND VISTA HOTEL — RECEIPT
                ==========================================
                Booking ID   : %s
                Date         : %s
                ------------------------------------------
                Guest        : %s
                Phone        : %s
                Email        : %s
                ------------------------------------------
                Room         : %s
                Nights       : %s
                Check-In     : %s
                Check-Out    : %s
                ------------------------------------------
                Payment      : %s
                TOTAL        : ₹%.0f
                ==========================================
                Thank you for staying at Grand Vista Hotel!
                """.formatted(
                b.getBookingId(),
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                b.getCustomerName(), b.getCustomerPhone(),
                b.getCustomerEmail().isEmpty() ? "—" : b.getCustomerEmail(),
                b.getRoomNumber(), b.getNumberOfDays(),
                b.getCheckInDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                b.getCheckOutDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                b.getPaymentMethod(), b.getTotalBill());
        String filename = "Receipt_" + b.getBookingId() + ".txt";
        try {
            Files.writeString(Paths.get(filename), content);
            alert(Alert.AlertType.INFORMATION, "Receipt Saved", "Saved as: " + filename);
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Save Failed", e.getMessage());
        }
    }

    public void refreshAll() {
        tableData.clear();
        bookingCombo.getItems().clear();
        svc.getActiveBookings().forEach(b -> {
            tableData.add(new CheckoutRow(
                    b.getBookingId(), b.getCustomerName(), b.getRoomNumber(),
                    b.getCheckInDate().toString(), b.getCheckOutDate().toString(),
                    String.valueOf(b.getNumberOfDays()),
                    "₹" + String.format("%.0f", b.getTotalBill())));
            bookingCombo.getItems().add(b.getBookingId());
        });
    }

    private void alert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public static class CheckoutRow {
        private final SimpleStringProperty bookingId, guestName, roomNumber,
                checkIn, checkOut, nights, total;

        CheckoutRow(String id, String n, String r, String ci, String co, String ni, String t) {
            bookingId = new SimpleStringProperty(id);
            guestName = new SimpleStringProperty(n);
            roomNumber = new SimpleStringProperty(r);
            checkIn = new SimpleStringProperty(ci);
            checkOut = new SimpleStringProperty(co);
            nights = new SimpleStringProperty(ni);
            total = new SimpleStringProperty(t);
        }

        public String getBookingId() {
            return bookingId.get();
        }

        public String getGuestName() {
            return guestName.get();
        }

        public String getRoomNumber() {
            return roomNumber.get();
        }

        public String getCheckIn() {
            return checkIn.get();
        }

        public String getCheckOut() {
            return checkOut.get();
        }

        public String getNights() {
            return nights.get();
        }

        public String getTotal() {
            return total.get();
        }
    }
}
