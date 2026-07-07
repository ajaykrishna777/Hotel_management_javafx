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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportsTab {

    private final HotelService svc = HotelService.getInstance();
    private final Node content;

    private Label totalRevLabel, pendingRevLabel, occupancyLabel, totalBookings;
    private Label topRoomLabel, avgStayLabel, totalGuestsLabel;
    private TableView<ReportRow> reportTable;
    private final ObservableList<ReportRow> tableData = FXCollections.observableArrayList();

    public ReportsTab() {
        content = build();
        refresh();
    }

    public Node getContent() {
        return content;
    }

    private Node build() {
        VBox page = new VBox(20);
        page.getStyleClass().add("dash-page");
        page.setPadding(new Insets(20));

        // Header section
        HBox header = buildHeader();

        // Stat cards section
        VBox statsSection = buildStatsSection();

        // Table section
        VBox tableSection = buildTableSection();
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        page.getChildren().addAll(header, statsSection, tableSection);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0));

        Label title = new Label("Reports & Analytics");
        title.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportBtn = new Button("Export CSV");
        exportBtn.getStyleClass().add("primary-btn");
        exportBtn.setOnAction(e -> exportCsv());

        header.getChildren().addAll(title, spacer, exportBtn);
        return header;
    }

    private VBox buildStatsSection() {
        VBox statsContainer = new VBox(12);
        statsContainer.setPadding(new Insets(0));

        // First row: 4 cards
        HBox firstRow = new HBox(14);
        firstRow.setAlignment(Pos.CENTER);

        totalRevLabel = statVal("₹0");
        pendingRevLabel = statVal("₹0");
        occupancyLabel = statVal("0%");
        totalBookings = statVal("0");
        topRoomLabel = statVal("—");
        avgStayLabel = statVal("—");
        totalGuestsLabel = statVal("0");

        VBox card1 = statCard("Total Revenue", totalRevLabel, "#22C55E");
        VBox card2 = statCard("Pending Revenue", pendingRevLabel, "#F97316");
        VBox card3 = statCard("Occupancy Rate", occupancyLabel, "#3B82F6");
        VBox card4 = statCard("Total Bookings", totalBookings, "#8B5CF6");

        firstRow.getChildren().addAll(card1, card2, card3, card4);
        for (Node n : firstRow.getChildren())
            HBox.setHgrow(n, Priority.ALWAYS);

        // Second row: 3 cards
        HBox secondRow = new HBox(14);
        secondRow.setAlignment(Pos.CENTER);

        VBox card5 = statCard("Unique Guests", totalGuestsLabel, "#EC4899");
        VBox card6 = statCard("Avg. Stay", avgStayLabel, "#14B8A6");
        VBox card7 = statCard("Most Booked Room", topRoomLabel, "#F59E0B");

        secondRow.getChildren().addAll(card5, card6, card7);
        for (Node n : secondRow.getChildren())
            HBox.setHgrow(n, Priority.ALWAYS);

        statsContainer.getChildren().addAll(firstRow, secondRow);
        return statsContainer;
    }

    private VBox buildTableSection() {
        VBox tableSection = new VBox(12);
        tableSection.getStyleClass().add("report-card");
        tableSection.setPadding(new Insets(20));
        tableSection.setMinHeight(400);

        Label tableTitle = new Label("Booking History");
        tableTitle.getStyleClass().add("section-title");

        reportTable = buildTable();
        reportTable.setMinHeight(300);
        reportTable.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(reportTable, Priority.ALWAYS);

        ScrollPane tableScroll = new ScrollPane(reportTable);
        tableScroll.setFitToWidth(true);
        tableScroll.setFitToHeight(true);
        tableScroll.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(tableScroll, Priority.ALWAYS);

        tableSection.getChildren().addAll(tableTitle, tableScroll);
        return tableSection;
    }

    private VBox statCard(String label, Label val, String color) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(16, 14, 16, 14));
        card.setAlignment(Pos.CENTER);
        card.setMinHeight(100);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("stat-label");
        lbl.setWrapText(true);
        lbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        lbl.setMaxWidth(Double.MAX_VALUE);

        val.setStyle("-fx-text-fill:" + color + "; -fx-font-size: 24px; -fx-font-weight: bold;");
        val.setWrapText(true);
        val.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        val.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(lbl, val);
        return card;
    }

    private Label statVal(String text) {
        return new Label(text);
    }

    @SuppressWarnings("unchecked")
    private TableView<ReportRow> buildTable() {
        TableView<ReportRow> tv = new TableView<>(tableData);
        tv.getStyleClass().add("hotel-table");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setPlaceholder(new Label("No booking data available."));

        tv.getColumns().addAll(
                col("BOOKING ID", "bookingId", 90),
                col("GUEST", "guest", 120),
                col("PHONE", "phone", 110),
                col("ROOM", "room", 60),
                col("CHECK-IN", "checkIn", 95),
                col("CHECKOUT", "checkOut", 95),
                col("NIGHTS", "nights", 55),
                col("TOTAL ₹", "total", 90),
                col("PAYMENT", "payment", 80),
                col("STATUS", "status", 90));
        return tv;
    }

    private <T> TableColumn<ReportRow, T> col(String t, String prop, double min) {
        TableColumn<ReportRow, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setMinWidth(min);
        return c;
    }

    public void refresh() {
        List<Booking> all = new ArrayList<>(svc.getAllBookings());

        totalRevLabel.setText(fmt(svc.getTotalRevenue()));
        pendingRevLabel.setText(fmt(svc.getPendingRevenue()));

        int total = svc.getTotalRooms();
        int booked = svc.getBookedRooms();
        occupancyLabel.setText(total > 0 ? String.format("%.0f%%", booked * 100.0 / total) : "0%");
        totalBookings.setText(String.valueOf(all.size()));
        totalGuestsLabel.setText(String.valueOf(svc.getAllGuestNames().size()));

        double avg = all.stream().mapToInt(Booking::getNumberOfDays).average().orElse(0);
        avgStayLabel.setText(avg == 0 ? "—" : String.format("%.1f nights", avg));

        // Top room
        all.stream()
                .collect(Collectors.groupingBy(Booking::getRoomNumber, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(e -> topRoomLabel.setText("Room " + e.getKey() + " (" + e.getValue() + "x)"));

        tableData.clear();
        all.forEach(b -> tableData.add(new ReportRow(
                b.getBookingId(), b.getCustomerName(), b.getCustomerPhone(),
                b.getRoomNumber(),
                b.getCheckInDate().format(DateTimeFormatter.ofPattern("dd/MM/yy")),
                b.getCheckOutDate().format(DateTimeFormatter.ofPattern("dd/MM/yy")),
                String.valueOf(b.getNumberOfDays()),
                String.format("%.0f", b.getTotalBill()),
                b.getPaymentMethod(),
                b.isCheckedOut() ? "CHECKED OUT" : "ACTIVE")));
    }

    private void exportCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking ID,Guest,Phone,Room,Check-In,Check-Out,Nights,Total,Payment,Status\n");
        svc.getAllBookings().forEach(b -> sb.append(String.join(",",
                b.getBookingId(), b.getCustomerName(), b.getCustomerPhone(),
                b.getRoomNumber(), b.getCheckInDate().toString(), b.getCheckOutDate().toString(),
                String.valueOf(b.getNumberOfDays()),
                String.format("%.0f", b.getTotalBill()),
                b.getPaymentMethod(),
                b.isCheckedOut() ? "CHECKED_OUT" : "ACTIVE")).append("\n"));
        String filename = "report_" + LocalDate.now() + ".csv";
        try {
            Files.writeString(Paths.get(filename), sb.toString());
            new Alert(Alert.AlertType.INFORMATION, "Exported: " + filename).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()).showAndWait();
        }
    }

    private String fmt(double v) {
        return String.format("₹%.0f", v);
    }

    public static class ReportRow {
        private final SimpleStringProperty bookingId, guest, phone, room,
                checkIn, checkOut, nights, total, payment, status;

        ReportRow(String id, String g, String ph, String r, String ci, String co,
                String n, String t, String pay, String s) {
            bookingId = new SimpleStringProperty(id);
            guest = new SimpleStringProperty(g);
            phone = new SimpleStringProperty(ph);
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

        public String getGuest() {
            return guest.get();
        }

        public String getPhone() {
            return phone.get();
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
