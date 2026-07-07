package ui;

import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import model.Booking;
import model.Room;
import model.Room.RoomStatus;
import service.HotelService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class GuestManagementTab {

    private final HotelService svc = HotelService.getInstance();
    private Runnable onDataChanged;

    private TableView<Booking> bookingTable;
    private TextField searchField;
    private final BorderPane rootPane;

    public GuestManagementTab(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        rootPane = buildRoot();
        refresh();
    }

    public Node getContent() {
        return rootPane;
    }

    private BorderPane buildRoot() {
        BorderPane bp = new BorderPane();
        bp.getStyleClass().add("guest-root");
        bp.setTop(buildToolbar());

        bookingTable = new TableView<>();
        bookingTable.getStyleClass().add("hotel-table");
        bookingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Guest Name column
        TableColumn<Booking, String> guestCol = new TableColumn<>("Guest Name");
        guestCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCustomerName()));

        // Room Number column
        TableColumn<Booking, String> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().getRoomNumber())));

        // Check-in Date column
        TableColumn<Booking, String> checkInCol = new TableColumn<>("Check-in");
        checkInCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getCheckInDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));

        // Check-out Date column
        TableColumn<Booking, String> checkOutCol = new TableColumn<>("Check-out");
        checkOutCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getCheckOutDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));

        // Nights column
        TableColumn<Booking, String> nightsCol = new TableColumn<>("Nights");
        nightsCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().getNumberOfDays())));

        // Status column
        TableColumn<Booking, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> {
            Booking booking = data.getValue();
            String status = "Pending";
            if (booking.isCheckedOut()) {
                status = "Checked Out";
            } else if (booking.getCheckInDate().isBefore(LocalDate.now())
                    || booking.getCheckInDate().isEqual(LocalDate.now())) {
                status = "Checked In";
            }
            return new javafx.beans.property.SimpleStringProperty(status);
        });

        // Actions column
        TableColumn<Booking, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<Booking, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setStyle("");
                } else {
                    Booking booking = getTableView().getItems().get(getIndex());
                    HBox pane = new HBox(6);
                    pane.setAlignment(Pos.CENTER);

                    boolean isCheckedIn = !booking.isCheckedOut() &&
                            (booking.getCheckInDate().isBefore(LocalDate.now())
                                    || booking.getCheckInDate().isEqual(LocalDate.now()));

                    if (!isCheckedIn && !booking.isCheckedOut()) {
                        Button checkInBtn = new Button("Check In");
                        checkInBtn.getStyleClass().add("table-action-btn");
                        checkInBtn.setOnAction(e -> performCheckIn(booking));
                        pane.getChildren().add(checkInBtn);
                    }

                    if (isCheckedIn && !booking.isCheckedOut()) {
                        Button checkOutBtn = new Button("Check Out");
                        checkOutBtn.getStyleClass().add("danger-btn-small");
                        checkOutBtn.setOnAction(e -> performCheckOut(booking));
                        pane.getChildren().add(checkOutBtn);
                    }

                    setGraphic(pane);
                }
            }
        });

        bookingTable.getColumns().addAll(guestCol, roomCol, checkInCol, checkOutCol, nightsCol, statusCol, actionsCol);

        ScrollPane scroll = new ScrollPane(bookingTable);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        bp.setCenter(scroll);

        return bp;
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("guest-toolbar");
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Guest Management");
        title.getStyleClass().add("page-title");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Search guests...");
        searchField.getStyleClass().add("hotel-input");
        searchField.setPrefWidth(250);
        searchField.textProperty().addListener((o, v, n) -> applyFilter());

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("secondary-btn");
        refreshBtn.setOnAction(e -> refresh());

        bar.getChildren().addAll(title, sp, searchField, refreshBtn);
        return bar;
    }

    private void performCheckIn(Booking booking) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Check-In");
        confirm.setHeaderText("Check-in Guest");
        confirm.setContentText("Check in " + booking.getCustomerName() + " to Room " + booking.getRoomNumber() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Update room status
                svc.findRoom(booking.getRoomNumber()).ifPresent(room -> {
                    room.setStatus(RoomStatus.BOOKED);
                    svc.updateRoom(room);
                });

                // Note: In this simplified version, we don't update booking status
                // since the Booking model doesn't have setCheckedIn method
                // The status is determined by check-in date logic

                if (onDataChanged != null) {
                    onDataChanged.run();
                }

                refresh();

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Check-In Successful");
                success.setHeaderText(null);
                success.setContentText(booking.getCustomerName() + " has been checked in successfully.");
                success.showAndWait();

            } catch (Exception e) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Check-In Failed");
                error.setHeaderText(null);
                error.setContentText("Failed to check in guest: " + e.getMessage());
                error.showAndWait();
            }
        }
    }

    private void performCheckOut(Booking booking) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Check-Out");
        confirm.setHeaderText("Check-out Guest");
        confirm.setContentText("Check out " + booking.getCustomerName() + " from Room " + booking.getRoomNumber()
                + "?\n\nTotal amount: ₹" + String.format("%.2f", booking.getTotalBill()));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Update room status to available
                svc.findRoom(booking.getRoomNumber()).ifPresent(room -> {
                    room.setStatus(RoomStatus.AVAILABLE);
                    svc.updateRoom(room);
                });

                // Update booking status
                booking.setCheckedOut(true);

                if (onDataChanged != null) {
                    onDataChanged.run();
                }

                refresh();

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Check-Out Successful");
                success.setHeaderText(null);
                success.setContentText(
                        booking.getCustomerName() + " has been checked out successfully.\nTotal collected: ₹"
                                + String.format("%.2f", booking.getTotalBill()));
                success.showAndWait();

            } catch (Exception e) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Check-Out Failed");
                error.setHeaderText(null);
                error.setContentText("Failed to check out guest: " + e.getMessage());
                error.showAndWait();
            }
        }
    }

    private void applyFilter() {
        String query = searchField.getText().toLowerCase().trim();
        List<Booking> bookings = svc.getAllBookings();

        if (query.isEmpty()) {
            bookingTable.getItems().setAll(bookings);
        } else {
            bookingTable.getItems().setAll(bookings.stream()
                    .filter(b -> b.getCustomerName().toLowerCase().contains(query) ||
                            String.valueOf(b.getRoomNumber()).contains(query))
                    .toList());
        }
    }

    public void refresh() {
        applyFilter();
    }
}
