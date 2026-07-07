package ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import model.Booking;
import service.HotelService;

import java.util.*;

public class GuestsTab {

    private static final HotelService SVC = HotelService.getInstance();

    private ListView<String> guestList;
    private TextField searchBox;
    private TableView<GuestRow> historyTable;
    private final ObservableList<GuestRow> histData = FXCollections.observableArrayList();
    private Label guestHeader, statsLabel;

    private final Node content;

    public GuestsTab() {
        content = build();
        refresh();
    }

    public Node getContent() {
        return content;
    }

    private Node build() {
        SplitPane split = new SplitPane(leftPane(), rightPane());
        split.setDividerPositions(0.30);
        return split;
    }

    private VBox leftPane() {
        VBox pane = new VBox(0);
        pane.getStyleClass().add("guests-left");

        HBox headerBand = new HBox();
        headerBand.getStyleClass().add("form-header-band");
        headerBand.setPadding(new Insets(16, 18, 16, 18));
        Label h = new Label("GUEST DIRECTORY");
        h.getStyleClass().add("panel-heading-light");
        headerBand.getChildren().add(h);

        searchBox = new TextField();
        searchBox.setPromptText("⊘  Search guests...");
        searchBox.getStyleClass().add("hotel-input");
        searchBox.setPadding(new Insets(10, 16, 10, 16));
        VBox searchWrap = new VBox(searchBox);
        searchWrap.setPadding(new Insets(12, 12, 12, 12));
        searchBox.textProperty().addListener((o, v, n) -> filterGuests(n));

        guestList = new ListView<>();
        guestList.getStyleClass().add("guest-list");
        VBox.setVgrow(guestList, Priority.ALWAYS);

        guestList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                StackPane avatar = new StackPane();
                avatar.getStyleClass().add("guest-avatar");
                avatar.setMinSize(30, 30);
                avatar.setMaxSize(30, 30);
                Label initials = new Label(item.substring(0, Math.min(2, item.length())).toUpperCase());
                initials.getStyleClass().add("guest-avatar-text");
                avatar.getChildren().add(initials);
                Label name = new Label(item);
                name.getStyleClass().add("guest-list-name");
                row.getChildren().addAll(avatar, name);
                setGraphic(row);
                setText(null);
            }
        });

        guestList.getSelectionModel().selectedItemProperty().addListener((o, v, n) -> {
            if (n != null)
                showGuestHistory(n);
        });

        pane.getChildren().addAll(headerBand, searchWrap, guestList);
        return pane;
    }

    private VBox rightPane() {
        VBox pane = new VBox(14);
        pane.getStyleClass().add("table-panel");
        pane.setPadding(new Insets(24, 28, 28, 24));

        guestHeader = new Label("Select a guest to view history");
        guestHeader.getStyleClass().add("page-title");

        statsLabel = new Label();
        statsLabel.getStyleClass().add("guest-stats");

        historyTable = buildHistoryTable();
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        pane.getChildren().addAll(guestHeader, statsLabel, historyTable);
        return pane;
    }

    @SuppressWarnings("unchecked")
    private TableView<GuestRow> buildHistoryTable() {
        TableView<GuestRow> tv = new TableView<>(histData);
        tv.getStyleClass().add("hotel-table");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setPlaceholder(new Label("Select a guest from the left panel."));

        tv.getColumns().addAll(
                col("BOOKING ID", "bookingId", 90),
                col("ROOM", "room", 70),
                col("CHECK-IN", "checkIn", 100),
                col("CHECK-OUT", "checkOut", 100),
                col("NIGHTS", "nights", 60),
                col("TOTAL", "total", 90),
                col("PAYMENT", "payment", 80),
                col("STATUS", "status", 90));
        return tv;
    }

    private <T> TableColumn<GuestRow, T> col(String t, String prop, double min) {
        TableColumn<GuestRow, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setMinWidth(min);
        return c;
    }

    private void showGuestHistory(String name) {
        guestHeader.setText(name);
        histData.clear();
        List<Booking> bookings = SVC.getBookingsForGuest(name);

        double totalSpent = 0;
        for (Booking b : bookings) {
            totalSpent += b.getTotalBill();
            histData.add(new GuestRow(
                    b.getBookingId(), b.getRoomNumber(),
                    b.getCheckInDate().toString(), b.getCheckOutDate().toString(),
                    String.valueOf(b.getNumberOfDays()),
                    "₹" + String.format("%.0f", b.getTotalBill()),
                    b.getPaymentMethod(),
                    b.isCheckedOut() ? "CHECKED OUT" : "ACTIVE"));
        }

        statsLabel.setText(String.format(
                "Total stays: %d  ·  Total spent: ₹%.0f  ·  Phone: %s",
                bookings.size(), totalSpent,
                bookings.isEmpty() ? "—" : bookings.get(0).getCustomerPhone()));
    }

    private void filterGuests(String query) {
        guestList.getItems().clear();
        SVC.getAllGuestNames().stream()
                .filter(n -> n.toLowerCase().contains(query.toLowerCase()))
                .forEach(n -> guestList.getItems().add(n));
    }

    public void refresh() {
        String currentSelection = guestList.getSelectionModel().getSelectedItem();

        filterGuests(searchBox != null ? searchBox.getText() : "");

        if (currentSelection != null && guestList.getItems().contains(currentSelection)) {
            guestList.getSelectionModel().select(currentSelection);
            showGuestHistory(currentSelection);
        }
    }

    public static class GuestRow {
        private final SimpleStringProperty bookingId, room, checkIn, checkOut,
                nights, total, payment, status;

        GuestRow(String id, String r, String ci, String co, String n, String t, String pay, String s) {
            bookingId = new SimpleStringProperty(id);
            room = new SimpleStringProperty(r);
            checkIn = new SimpleStringProperty(ci);
            checkOut = new SimpleStringProperty(co);
            nights = new SimpleStringProperty(n);
            total = new SimpleStringProperty(t);
            payment = new SimpleStringProperty(pay);
            status = new SimpleStringProperty(s);
        }

        public String getBookingId() {
            return bookingId.get();
        }

        public String getRoom() {
            return room.get();
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

        public String getPayment() {
            return payment.get();
        }

        public String getStatus() {
            return status.get();
        }
    }
}
