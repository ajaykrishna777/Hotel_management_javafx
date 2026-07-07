package ui;

import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.util.Duration;
import model.Booking;
import model.Room;
import service.HotelService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardTab {

    private final HotelService svc = HotelService.getInstance();

    private Label availableVal, bookedVal, maintenanceVal, totalVal;
    private Label revenueVal, pendingVal, activeBookVal;
    private Label occupancyPct, dateLabel;
    private Arc occupancyArc;
    private VBox activityLogContainer;
    private Label revCollectedVal, revPendingVal;
    private StackPane collectedBar, pendingBar;
    private Label insightRoomsService, insightOccupied, insightAvgNights, insightGuestsToday;

    private final List<ActivityEntry> activityLog = new ArrayList<>();
    private final Node contentNode;

    public DashboardTab() {
        contentNode = buildContent();
        refresh();
    }

    public Node getContent() {
        return contentNode;
    }

    private Node buildContent() {
        VBox page = new VBox(28);
        page.getStyleClass().add("dash-page");
        page.setPadding(new Insets(24));
        page.setStyle("-fx-border-color: transparent;");

        Node hero = buildHeroBanner();
        Node metrics = buildMetricStrip();
        Node bottom = buildBottomRow();

        VBox.setMargin(bottom, new Insets(16, 0, 0, 0));
        page.getChildren().addAll(hero, metrics, bottom);
        VBox.setVgrow(bottom, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setPrefViewportHeight(800);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }

    private Node buildHeroBanner() {
        HBox hero = new HBox(20);
        hero.getStyleClass().add("hero-banner");
        hero.setPadding(new Insets(24, 32, 24, 32));
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setMinHeight(210);
        hero.setPrefHeight(210);
        hero.setMaxHeight(210);

        VBox textArea = new VBox(8);
        Label title = new Label("Front Desk Overview");
        title.getStyleClass().add("hero-title");

        dateLabel = new Label();
        dateLabel.getStyleClass().add("hero-subtitle");
        refreshDateLabel();

        Label property = new Label("Grand Vista Hotel  ·  Bengaluru, Karnataka");
        property.getStyleClass().add("hero-tagline");

        HBox row1 = new HBox(16, title, buildLivePill());
        row1.setAlignment(Pos.CENTER_LEFT);
        textArea.getChildren().addAll(row1, dateLabel, property);

        StackPane ring = buildOccupancyRing();
        ring.setMinSize(160, 160);
        ring.setMaxSize(160, 160);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hero.getChildren().addAll(textArea, spacer, ring);
        return hero;
    }

    private HBox buildLivePill() {
        HBox pill = new HBox(6);
        pill.getStyleClass().add("live-pill");
        pill.setAlignment(Pos.CENTER_LEFT);
        pill.setMaxWidth(Region.USE_PREF_SIZE);
        pill.setPadding(new Insets(4, 12, 4, 12));

        Circle dot = new Circle(4, Color.web("#FFFFFF"));
        FadeTransition ft = new FadeTransition(Duration.millis(900), dot);
        ft.setFromValue(1.0);
        ft.setToValue(0.3);
        ft.setAutoReverse(true);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.play();

        Label lbl = new Label("LIVE");
        lbl.getStyleClass().add("live-pill-text");
        pill.getChildren().addAll(dot, lbl);
        return pill;
    }

    private StackPane buildOccupancyRing() {
        StackPane stack = new StackPane();
        stack.setPrefSize(140, 140);

        Arc bgArc = new Arc(70, 70, 54, 54, 90, -360);
        bgArc.setType(ArcType.OPEN);
        bgArc.setStroke(Color.web("#FFFFFF", 0.15));
        bgArc.setStrokeWidth(10);
        bgArc.setFill(Color.TRANSPARENT);
        bgArc.setStrokeLineCap(StrokeLineCap.ROUND);

        occupancyArc = new Arc(70, 70, 54, 54, 90, 0);
        occupancyArc.setType(ArcType.OPEN);
        occupancyArc.setStroke(Color.web("#FFFFFF"));
        occupancyArc.setStrokeWidth(10);
        occupancyArc.setFill(Color.TRANSPARENT);
        occupancyArc.setStrokeLineCap(StrokeLineCap.ROUND);

        Pane arcPane = new Pane(bgArc, occupancyArc);
        arcPane.setPrefSize(140, 140);

        occupancyPct = new Label("0%");
        occupancyPct.getStyleClass().add("ring-pct");

        Label ringLbl = new Label("OCCUPIED");
        ringLbl.getStyleClass().add("ring-label");

        VBox center = new VBox(2, occupancyPct, ringLbl);
        center.setAlignment(Pos.CENTER);
        stack.getChildren().addAll(arcPane, center);
        return stack;
    }

    private Node buildMetricStrip() {
        availableVal = new Label("—");
        bookedVal = new Label("—");
        maintenanceVal = new Label("—");
        totalVal = new Label("—");
        revenueVal = new Label("—");
        pendingVal = new Label("—");
        activeBookVal = new Label("—");

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(24);
        grid.setPadding(new Insets(0));
        grid.setMinHeight(Region.USE_PREF_SIZE);
        grid.getStyleClass().add("metric-strip");

        grid.add(metricCell("Available Rooms", availableVal, "#22C55E"), 0, 0);
        grid.add(metricCell("Booked", bookedVal, "#F97316"), 1, 0);
        grid.add(metricCell("Maintenance", maintenanceVal, "#EF4444"), 2, 0);
        grid.add(metricCell("Total Rooms", totalVal, "#3B82F6"), 3, 0);
        grid.add(metricCell("Revenue (₹)", revenueVal, "#22C55E"), 0, 1);
        grid.add(metricCell("Pending (₹)", pendingVal, "#F97316"), 1, 1);
        grid.add(metricCell("Active Bookings", activeBookVal, "#3B82F6"), 2, 1);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setHgrow(Priority.ALWAYS);
            c.setFillWidth(true);
            grid.getColumnConstraints().add(c);
        }

        return grid;
    }

    private VBox metricCell(String title, Label val, String color) {
        VBox cell = new VBox(4);
        cell.getStyleClass().add("metric-cell");
        cell.setAlignment(Pos.CENTER);
        cell.setPadding(new Insets(16, 12, 16, 12));
        val.setStyle("-fx-text-fill:" + color + "; -fx-font-size:22px; -fx-font-weight:800;");
        Label t = new Label(title);
        t.getStyleClass().add("metric-title");
        cell.getChildren().addAll(val, t);
        return cell;
    }

    private Rectangle vDiv() {
        Rectangle r = new Rectangle(1, 44);
        r.setFill(Color.web("#E5E7EB"));
        return r;
    }

    private Node buildBottomRow() {
        HBox row = new HBox(36);
        row.setStyle("-fx-padding: 0; -fx-background-color: transparent; -fx-border-color: transparent;");
        row.setAlignment(Pos.TOP_LEFT);
        row.setMinHeight(Region.USE_COMPUTED_SIZE);
        row.setPrefHeight(Region.USE_COMPUTED_SIZE);
        row.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(row, Priority.ALWAYS);

        VBox actCard = buildActivityCard();
        actCard.setPrefWidth(450);
        actCard.setMinWidth(300);
        HBox.setHgrow(actCard, Priority.ALWAYS);

        VBox revCard = buildRevenueCard();
        revCard.setPrefWidth(320);
        revCard.setMinWidth(280);
        revCard.setMaxWidth(400);
        HBox.setHgrow(revCard, Priority.SOMETIMES);

        row.getChildren().addAll(actCard, revCard);
        return row;
    }

    private VBox buildActivityCard() {
        VBox card = new VBox(0);
        card.getStyleClass().add("dash-card");
        card.setStyle("-fx-border-color: transparent;");
        card.setMinHeight(320);
        card.setPrefHeight(400);
        card.setMaxHeight(Double.MAX_VALUE);

        HBox header = new HBox();
        header.getStyleClass().add("card-header-bar");
        header.setPadding(new Insets(14, 18, 14, 18));
        header.setAlignment(Pos.CENTER_LEFT);
        Label h = new Label("Recent Activity");
        h.getStyleClass().add("card-heading");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label sub = new Label("This session");
        sub.getStyleClass().add("card-subheading");
        header.getChildren().addAll(h, sp, sub);

        activityLogContainer = new VBox(0);
        activityLogContainer.getStyleClass().add("activity-list");

        ScrollPane scroll = new ScrollPane(activityLogContainer);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        card.getChildren().addAll(header, new Separator(), scroll);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox buildRevenueCard() {
        VBox card = new VBox(0);
        card.getStyleClass().add("dash-card");
        card.setStyle("-fx-border-color: transparent;");
        card.setMinHeight(280);
        card.setPrefHeight(Region.USE_COMPUTED_SIZE);
        card.setMaxHeight(Double.MAX_VALUE);

        HBox header = new HBox();
        header.getStyleClass().add("card-header-bar");
        header.setPadding(new Insets(14, 18, 14, 18));
        Label h = new Label("Revenue Summary");
        h.getStyleClass().add("card-heading");
        header.getChildren().add(h);

        VBox body = new VBox(20);
        body.setPadding(new Insets(18));

        revCollectedVal = new Label("₹0");
        revPendingVal = new Label("₹0");

        collectedBar = new StackPane();
        collectedBar.getStyleClass().add("rev-bar-track");
        pendingBar = new StackPane();
        pendingBar.getStyleClass().add("rev-bar-track");
        collectedBar.setAlignment(Pos.CENTER_LEFT);
        collectedBar.setMinHeight(7);
        collectedBar.setPrefHeight(7);
        pendingBar.setAlignment(Pos.CENTER_LEFT);
        pendingBar.setMinHeight(7);
        pendingBar.setPrefHeight(7);

        Region cFill = new Region();
        cFill.getStyleClass().add("rev-bar-fill");
        cFill.setPrefHeight(7);
        cFill.setMaxWidth(0);
        Region pFill = new Region();
        pFill.getStyleClass().add("rev-bar-fill-pending");
        pFill.setPrefHeight(7);
        pFill.setMaxWidth(0);
        collectedBar.getChildren().add(cFill);
        pendingBar.getChildren().add(pFill);

        body.getChildren().addAll(
                revRow("Collected", revCollectedVal, collectedBar, "#22C55E"),
                revRow("Pending", revPendingVal, pendingBar, "#F97316"));

        // Insights
        Separator sep = new Separator();
        sep.setOpacity(0.5);
        Label insTitle = new Label("OCCUPANCY INSIGHT");
        insTitle.getStyleClass().add("insight-label");

        VBox insights = new VBox(10);
        insights.getChildren().addAll(
                insightRow("Rooms in service", "insight_rooms_service"),
                insightRow("Currently occupied", "insight_occupied"),
                insightRow("Avg. nights/booking", "insight_avg_nights"),
                insightRow("Total guests today", "insight_guests_today"));

        body.getChildren().addAll(sep, insTitle, insights);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        card.getChildren().addAll(header, new Separator(), scroll);
        return card;
    }

    private VBox revRow(String label, Label val, StackPane bar, String color) {
        VBox group = new VBox(6);
        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("rev-row-label");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        val.setStyle("-fx-text-fill:" + color + "; -fx-font-size:15px; -fx-font-weight:700;");
        top.getChildren().addAll(lbl, sp, val);
        group.getChildren().addAll(top, bar);
        return group;
    }

    private HBox insightRow(String label, String id) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("insight-row-label");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label val = new Label("—");
        val.setId(id);
        val.getStyleClass().add("insight-row-val");

        // Store references to insight labels
        if ("insight_rooms_service".equals(id)) {
            insightRoomsService = val;
        } else if ("insight_occupied".equals(id)) {
            insightOccupied = val;
        } else if ("insight_avg_nights".equals(id)) {
            insightAvgNights = val;
        } else if ("insight_guests_today".equals(id)) {
            insightGuestsToday = val;
        }

        row.getChildren().addAll(lbl, sp, val);
        return row;
    }

    public void refresh() {
        availableVal.setText(String.valueOf(svc.getAvailableRoomCount()));
        bookedVal.setText(String.valueOf(svc.getBookedRooms()));
        maintenanceVal.setText(String.valueOf(svc.getMaintenanceRooms()));
        totalVal.setText(String.valueOf(svc.getTotalRooms()));
        revenueVal.setText(fmt(svc.getTotalRevenue()));
        pendingVal.setText(fmt(svc.getPendingRevenue()));
        activeBookVal.setText(String.valueOf(svc.getActiveBookingCount()));

        int total = svc.getTotalRooms();
        int booked = svc.getBookedRooms();
        double pct = total > 0 ? booked * 100.0 / total : 0;
        occupancyPct.setText(String.format("%.0f%%", pct));
        double target = -(pct / 100.0) * 360.0;
        new Timeline(new KeyFrame(Duration.millis(700),
                new KeyValue(occupancyArc.lengthProperty(), target, Interpolator.EASE_BOTH))).play();

        refreshDateLabel();

        double collected = svc.getTotalRevenue();
        double pending = svc.getPendingRevenue();
        double maxRev = collected + pending;
        revCollectedVal.setText(fmt(collected));
        revPendingVal.setText(fmt(pending));
        animBar(collectedBar, collected, maxRev);
        animBar(pendingBar, pending, maxRev);

        // Update occupancy insights using stored label references
        if (insightRoomsService != null)
            insightRoomsService.setText(String.valueOf(total));
        if (insightOccupied != null)
            insightOccupied.setText(booked + " / " + total);
        if (insightAvgNights != null) {
            double avg = svc.getAllBookings().stream().mapToInt(b -> b.getNumberOfDays()).average().orElse(0);
            insightAvgNights.setText(avg == 0 ? "—" : String.format("%.1f", avg));
        }
        if (insightGuestsToday != null)
            insightGuestsToday.setText(String.valueOf(svc.getActiveBookingCount()));

        rebuildActivityLog();
    }

    private void animBar(StackPane track, double value, double max) {
        if (track.getChildren().isEmpty())
            return;
        Region fill = (Region) track.getChildren().get(0);
        double ratio = max > 0 ? Math.min(value / max, 1.0) : 0;
        if (track.getWidth() > 0) {
            new Timeline(new KeyFrame(Duration.millis(600),
                    new KeyValue(fill.maxWidthProperty(), track.getWidth() * ratio, Interpolator.EASE_BOTH))).play();
        }
    }

    private void rebuildActivityLog() {
        activityLogContainer.getChildren().clear();
        if (activityLog.isEmpty())
            seedActivity();
        if (activityLog.isEmpty()) {
            Label empty = new Label("No activity recorded yet.");
            empty.getStyleClass().add("activity-empty");
            empty.setPadding(new Insets(20, 18, 20, 18));
            activityLogContainer.getChildren().add(empty);
            return;
        }
        for (int i = activityLog.size() - 1; i >= 0; i--) {
            activityLogContainer.getChildren().add(buildActivityRow(activityLog.get(i)));
            if (i > 0) {
                Separator s = new Separator();
                s.setOpacity(0.4);
                activityLogContainer.getChildren().add(s);
            }
        }
    }

    private void seedActivity() {
        for (Booking b : svc.getAllBookings()) {
            if (b.isCheckedOut())
                addActivity(ActivityType.CHECKOUT,
                        "Room " + b.getRoomNumber() + " checked out",
                        b.getCustomerName() + " · ₹" + String.format("%.0f", b.getTotalBill()),
                        b.getCheckOutDate().atStartOfDay());
            else
                addActivity(ActivityType.BOOKING,
                        "Room " + b.getRoomNumber() + " booked",
                        b.getCustomerName() + " · " + b.getNumberOfDays() + " night(s)",
                        b.getCheckInDate().atStartOfDay());
        }
        for (Room r : svc.getAllRooms())
            if (r.getStatus() == Room.RoomStatus.MAINTENANCE)
                addActivity(ActivityType.MAINTENANCE,
                        "Room " + r.getRoomNumber() + " in maintenance", r.getDescription(),
                        LocalDateTime.now().minusHours(1));
    }

    private Node buildActivityRow(ActivityEntry e) {
        HBox row = new HBox(14);
        row.getStyleClass().add("activity-row");
        row.setPadding(new Insets(11, 18, 11, 18));
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane badge = new StackPane();
        badge.getStyleClass().addAll("activity-badge", e.type.cssClass());
        badge.setMinSize(34, 34);
        badge.setMaxSize(34, 34);
        Label icon = new Label(e.type.icon());
        icon.setStyle("-fx-font-size:14px;");
        badge.getChildren().add(icon);

        VBox texts = new VBox(2);
        Label prim = new Label(e.primary);
        prim.getStyleClass().add("activity-primary");
        Label sec = new Label(e.secondary);
        sec.getStyleClass().add("activity-secondary");
        texts.getChildren().addAll(prim, sec);
        HBox.setHgrow(texts, Priority.ALWAYS);

        Label time = new Label(fmtTime(e.time));
        time.getStyleClass().add("activity-time");
        row.getChildren().addAll(badge, texts, time);
        return row;
    }

    private String fmtTime(LocalDateTime dt) {
        long mins = java.time.Duration.between(dt, LocalDateTime.now()).toMinutes();
        if (mins < 1)
            return "just now";
        if (mins < 60)
            return mins + "m ago";
        long hrs = mins / 60;
        if (hrs < 24)
            return hrs + "h ago";
        return dt.format(DateTimeFormatter.ofPattern("dd MMM"));
    }

    public void addActivity(ActivityType type, String primary, String secondary, LocalDateTime time) {
        activityLog.add(new ActivityEntry(type, primary, secondary, time));
        if (activityLog.size() > 50)
            activityLog.remove(0);
    }

    public void addActivity(ActivityType type, String primary, String secondary) {
        addActivity(type, primary, secondary, LocalDateTime.now());
    }

    private void refreshDateLabel() {
        if (dateLabel != null)
            dateLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy  ·  HH:mm")));
    }

    private String fmt(double v) {
        return String.format("₹%.0f", v);
    }

    public enum ActivityType {
        BOOKING, CHECKOUT, MAINTENANCE, ROOM_ADDED;

        public String icon() {
            return switch (this) {
                case BOOKING -> "✦";
                case CHECKOUT -> "↗";
                case MAINTENANCE -> "⚙";
                case ROOM_ADDED -> "+";
            };
        }

        public String cssClass() {
            return switch (this) {
                case BOOKING -> "badge-booking";
                case CHECKOUT -> "badge-checkout";
                case MAINTENANCE -> "badge-maintenance";
                case ROOM_ADDED -> "badge-room";
            };
        }
    }

    public record ActivityEntry(ActivityType type, String primary, String secondary, LocalDateTime time) {
    }
}
