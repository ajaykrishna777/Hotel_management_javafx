package service;

import model.*;
import model.Room.RoomStatus;
import model.Room.RoomType;
import model.Bill.PaymentStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * HotelService — in-memory data layer / service singleton.
 */
public class HotelService {

    // ── Singleton ────────────────────────────────────────────────────────────
    private static HotelService instance;

    public static HotelService getInstance() {
        if (instance == null)
            instance = new HotelService();
        return instance;
    }

    // ── Data stores ─────────────────────────────────────────────────────────
    private final List<Room> rooms = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final List<Guest> guests = new ArrayList<>();
    private final List<User> users = new ArrayList<>();
    private final List<Bill> bills = new ArrayList<>();

    private final AtomicInteger bookingCounter = new AtomicInteger(1000);
    private final AtomicInteger billCounter = new AtomicInteger(5000);
    private final AtomicInteger guestCounter = new AtomicInteger(100);

    private static final Path USER_FILE = Paths.get("data", "users.txt");
    private static final Path ROOMS_FILE = Paths.get("data", "rooms.txt");
    private static final Path BOOKINGS_FILE = Paths.get("data", "bookings.txt");
    private static final Path GUESTS_FILE = Paths.get("data", "guests.txt");
    private static final Path BILLS_FILE = Paths.get("data", "bills.txt");
    private static final Path COUNTERS_FILE = Paths.get("data", "counters.txt");

    // ── Constructor / seed data ─────────────────────────────────────────────
    private HotelService() {
        loadCountersFromFile();
        loadRoomsFromFile();
        loadBookingsFromFile();
        loadGuestsFromFile();
        loadBillsFromFile();
        loadUsersFromFile();
        seedUsers();
    }

    private void seedRooms() {
        rooms.add(new Room("101", RoomType.STANDARD, 1500, RoomStatus.AVAILABLE,
                "Cozy standard room with garden view", 1, 2));
        rooms.add(new Room("102", RoomType.STANDARD, 1500, RoomStatus.BOOKED,
                "Standard room near elevator", 1, 2));
        rooms.add(new Room("103", RoomType.STANDARD, 1600, RoomStatus.AVAILABLE,
                "Standard room with extra sofa", 1, 3));
        rooms.add(new Room("201", RoomType.DELUXE, 2800, RoomStatus.AVAILABLE,
                "Deluxe room with city view", 2, 2));
        rooms.add(new Room("202", RoomType.DELUXE, 3000, RoomStatus.BOOKED,
                "Deluxe room with balcony", 2, 3));
        rooms.add(new Room("203", RoomType.DELUXE, 2900, RoomStatus.MAINTENANCE,
                "Renovating — back in service soon", 2, 2));
        rooms.add(new Room("301", RoomType.SUITE, 5500, RoomStatus.AVAILABLE,
                "Executive suite with lounge", 3, 4));
        rooms.add(new Room("302", RoomType.SUITE, 6000, RoomStatus.BOOKED,
                "Presidential suite with jacuzzi", 3, 4));
        rooms.add(new Room("401", RoomType.STANDARD, 1700, RoomStatus.AVAILABLE,
                "Top-floor standard with panorama", 4, 2));
        rooms.add(new Room("402", RoomType.DELUXE, 3200, RoomStatus.AVAILABLE,
                "Corner deluxe with dual views", 4, 3));
    }

    private void seedBookings() {
        LocalDate today = LocalDate.now();

        // Active booking for room 102
        Booking b1 = new Booking(
                "BK-" + bookingCounter.getAndIncrement(),
                "Arjun Sharma", "9876543210", "arjun.sharma@email.com",
                "102", 3, today.minusDays(1), 4500, "CASH", "Extra pillows please");
        bookings.add(b1);
        addGuestIfNew("Arjun Sharma", "9876543210", "arjun.sharma@email.com");

        // Active booking for room 202
        Booking b2 = new Booking(
                "BK-" + bookingCounter.getAndIncrement(),
                "Priya Nair", "9123456789", "priya.nair@email.com",
                "202", 5, today, 15000, "CARD", "");
        bookings.add(b2);
        addGuestIfNew("Priya Nair", "9123456789", "priya.nair@email.com");

        // Active booking for room 302
        Booking b3 = new Booking(
                "BK-" + bookingCounter.getAndIncrement(),
                "Rohit Mehra", "9988776655", "",
                "302", 2, today, 12000, "UPI", "Late check-in around midnight");
        bookings.add(b3);
        addGuestIfNew("Rohit Mehra", "9988776655", "");

        // Checked-out historical booking
        Booking b4 = new Booking(
                "BK-" + bookingCounter.getAndIncrement(),
                "Sunita Patel", "9001122334", "sunita.patel@email.com",
                "201", 4, today.minusDays(10), 11200, "BANK TRANSFER", "");
        b4.setCheckedOut(true);
        bookings.add(b4);
        addGuestIfNew("Sunita Patel", "9001122334", "sunita.patel@email.com");

        // Another checked-out
        Booking b5 = new Booking(
                "BK-" + bookingCounter.getAndIncrement(),
                "Kiran Rao", "9777888999", "kiran.rao@email.com",
                "101", 2, today.minusDays(5), 3000, "CASH", "");
        b5.setCheckedOut(true);
        bookings.add(b5);
        addGuestIfNew("Kiran Rao", "9777888999", "kiran.rao@email.com");

        // Repeat guest
        Booking b6 = new Booking(
                "BK-" + bookingCounter.getAndIncrement(),
                "Arjun Sharma", "9876543210", "arjun.sharma@email.com",
                "201", 6, today.minusDays(20), 16800, "CARD", "Honeymoon suite");
        b6.setCheckedOut(true);
        bookings.add(b6);

        // Generate bills for checked-out bookings
        createBill(b4.getBookingId(), b4.getTotalBill(), PaymentStatus.PAID);
        createBill(b5.getBookingId(), b5.getTotalBill(), PaymentStatus.PAID);
        createBill(b6.getBookingId(), b6.getTotalBill(), PaymentStatus.PAID);
        createBill(b1.getBookingId(), b1.getTotalBill(), PaymentStatus.PENDING);
        createBill(b2.getBookingId(), b2.getTotalBill(), PaymentStatus.PENDING);
        createBill(b3.getBookingId(), b3.getTotalBill(), PaymentStatus.PENDING);
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private void loadUsersFromFile() {
        try {
            if (!Files.exists(USER_FILE)) {
                Files.createDirectories(USER_FILE.getParent());
                Files.createFile(USER_FILE);
                return;
            }

            List<String> lines = Files.readAllLines(USER_FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.isBlank())
                    continue;
                String[] parts = line.split("\\|", 3);
                if (parts.length < 3)
                    continue;
                users.add(new User(parts[0], parts[1], parts[2]));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load users from file", e);
        }
    }

    private void saveUsersToFile() {
        try {
            if (!Files.exists(USER_FILE)) {
                Files.createDirectories(USER_FILE.getParent());
                Files.createFile(USER_FILE);
            }
            List<String> lines = new ArrayList<>();
            for (User u : users) {
                lines.add(u.getUsername() + "|" + u.getPassword() + "|" + u.getRole());
            }
            Files.write(USER_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save users to file", e);
        }
    }

    private void seedUsers() {
        if (users.isEmpty()) {
            users.add(new User("manager", hashPassword("hotel123"), "Manager"));
            users.add(new User("admin", hashPassword("admin123"), "Administrator"));
            saveUsersToFile();
        }
    }

    // ── File Persistence Helpers ────────────────────────────────────────────

    private void ensureDataDirectory() {
        try {
            if (!Files.exists(ROOMS_FILE.getParent())) {
                Files.createDirectories(ROOMS_FILE.getParent());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create data directory", e);
        }
    }

    // ── Counter Persistence ────────────────────────────────────────────────

    private void loadCountersFromFile() {
        try {
            if (!Files.exists(COUNTERS_FILE)) {
                return;
            }
            List<String> lines = Files.readAllLines(COUNTERS_FILE, StandardCharsets.UTF_8);
            if (lines.size() >= 3) {
                bookingCounter.set(Integer.parseInt(lines.get(0)));
                billCounter.set(Integer.parseInt(lines.get(1)));
                guestCounter.set(Integer.parseInt(lines.get(2)));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load counters from file", e);
        }
    }

    private void saveCountersToFile() {
        try {
            ensureDataDirectory();
            List<String> lines = new ArrayList<>();
            lines.add(String.valueOf(bookingCounter.get()));
            lines.add(String.valueOf(billCounter.get()));
            lines.add(String.valueOf(guestCounter.get()));
            Files.write(COUNTERS_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save counters to file", e);
        }
    }

    // ── Room Persistence ───────────────────────────────────────────────────

    private void loadRoomsFromFile() {
        try {
            if (!Files.exists(ROOMS_FILE)) {
                seedRooms();
                saveRoomsToFile();
                return;
            }

            List<String> lines = Files.readAllLines(ROOMS_FILE, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                seedRooms();
                saveRoomsToFile();
                return;
            }

            for (String line : lines) {
                if (line.isBlank())
                    continue;
                String[] parts = line.split("\\|", 7);
                if (parts.length < 7)
                    continue;
                Room room = new Room(parts[0], RoomType.valueOf(parts[1]), Double.parseDouble(parts[2]),
                        RoomStatus.valueOf(parts[3]), parts[4], Integer.parseInt(parts[5]),
                        Integer.parseInt(parts[6]));
                rooms.add(room);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load rooms from file", e);
        }
    }

    private void saveRoomsToFile() {
        try {
            ensureDataDirectory();
            List<String> lines = new ArrayList<>();
            for (Room r : rooms) {
                lines.add(r.getRoomNumber() + "|" + r.getRoomType() + "|" + r.getPricePerNight() + "|"
                        + r.getStatus() + "|" + r.getDescription() + "|" + r.getFloor() + "|" + r.getCapacity());
            }
            Files.write(ROOMS_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save rooms to file", e);
        }
    }

    // ── Guest Persistence ──────────────────────────────────────────────────

    private void loadGuestsFromFile() {
        try {
            if (!Files.exists(GUESTS_FILE)) {
                return;
            }

            List<String> lines = Files.readAllLines(GUESTS_FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.isBlank())
                    continue;
                String[] parts = line.split("\\|", 4);
                if (parts.length < 4)
                    continue;
                Guest g = new Guest(parts[0], parts[1], parts[2], parts[3]);
                guests.add(g);
                // Update counter to avoid ID conflicts
                try {
                    int id = Integer.parseInt(parts[0].substring(2));
                    if (id >= guestCounter.get()) {
                        guestCounter.set(id + 1);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load guests from file", e);
        }
    }

    private void saveGuestsToFile() {
        try {
            ensureDataDirectory();
            List<String> lines = new ArrayList<>();
            for (Guest g : guests) {
                lines.add(g.getGuestId() + "|" + g.getName() + "|"
                        + (g.getPhone() != null ? g.getPhone() : "") + "|"
                        + (g.getEmail() != null ? g.getEmail() : ""));
            }
            Files.write(GUESTS_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save guests to file", e);
        }
    }

    // ── Booking Persistence ────────────────────────────────────────────────

    private void loadBookingsFromFile() {
        try {
            if (!Files.exists(BOOKINGS_FILE)) {
                seedBookings();
                saveBookingsToFile();
                return;
            }

            List<String> lines = Files.readAllLines(BOOKINGS_FILE, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                seedBookings();
                saveBookingsToFile();
                return;
            }

            for (String line : lines) {
                if (line.isBlank())
                    continue;
                String[] parts = line.split("\\|", 12);
                if (parts.length < 12)
                    continue;
                Booking b = new Booking(parts[0], parts[1], parts[2], parts[3], parts[4],
                        Integer.parseInt(parts[5]), LocalDate.parse(parts[6]),
                        Double.parseDouble(parts[7]), parts[8], parts[9]);
                b.setCheckedOut(Boolean.parseBoolean(parts[10]));
                if (!parts[11].isEmpty()) {
                    b.setCheckOutDate(LocalDate.parse(parts[11]));
                }
                bookings.add(b);
                // Update counter to avoid ID conflicts
                try {
                    int id = Integer.parseInt(parts[0].substring(3));
                    if (id >= bookingCounter.get()) {
                        bookingCounter.set(id + 1);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load bookings from file", e);
        }
    }

    private void saveBookingsToFile() {
        try {
            ensureDataDirectory();
            List<String> lines = new ArrayList<>();
            for (Booking b : bookings) {
                lines.add(b.getBookingId() + "|" + b.getCustomerName() + "|" + b.getCustomerPhone() + "|"
                        + b.getCustomerEmail() + "|" + b.getRoomNumber() + "|"
                        + b.getNumberOfDays() + "|" + b.getCheckInDate() + "|" + b.getTotalBill() + "|"
                        + b.getPaymentMethod() + "|" + b.getSpecialRequests()
                        + "|" + b.isCheckedOut() + "|"
                        + (b.getCheckOutDate() != null ? b.getCheckOutDate() : ""));
            }
            Files.write(BOOKINGS_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save bookings to file", e);
        }
    }

    // ── Bill Persistence ───────────────────────────────────────────────────

    private void loadBillsFromFile() {
        try {
            if (!Files.exists(BILLS_FILE)) {
                return;
            }

            List<String> lines = Files.readAllLines(BILLS_FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.isBlank())
                    continue;
                String[] parts = line.split("\\|", 4);
                if (parts.length < 4)
                    continue;
                Bill bill = new Bill(parts[0], parts[1], Double.parseDouble(parts[2]),
                        PaymentStatus.valueOf(parts[3]));
                bills.add(bill);
                // Update counter to avoid ID conflicts
                try {
                    int id = Integer.parseInt(parts[0].substring(5));
                    if (id >= billCounter.get()) {
                        billCounter.set(id + 1);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load bills from file", e);
        }
    }

    private void saveBillsToFile() {
        try {
            ensureDataDirectory();
            List<String> lines = new ArrayList<>();
            for (Bill b : bills) {
                lines.add(b.getBillId() + "|" + b.getBookingId() + "|" + b.getAmount() + "|"
                        + b.getPaymentStatus());
            }
            Files.write(BILLS_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save bills to file", e);
        }
    }

    // ── Room Operations ─────────────────────────────────────────────────────

    public void addRoom(Room room) {
        boolean exists = rooms.stream().anyMatch(r -> r.getRoomNumber().equalsIgnoreCase(room.getRoomNumber()));
        if (exists)
            throw new IllegalArgumentException("Room " + room.getRoomNumber() + " already exists.");
        rooms.add(room);
        saveRoomsToFile();
    }

    public List<Room> getAllRooms() {
        return Collections.unmodifiableList(rooms);
    }

    public List<Room> getRoomsByStatus(RoomStatus status) {
        return rooms.stream().filter(r -> r.getStatus() == status).collect(Collectors.toList());
    }

    public List<Room> getAvailableRooms() {
        return getRoomsByStatus(RoomStatus.AVAILABLE);
    }

    public Optional<Room> findRoom(String roomNumber) {
        return rooms.stream().filter(r -> r.getRoomNumber().equalsIgnoreCase(roomNumber)).findFirst();
    }

    public void updateRoom(Room updated) {
        findRoom(updated.getRoomNumber()).ifPresent(r -> {
            r.setRoomType(updated.getRoomType());
            r.setPricePerNight(updated.getPricePerNight());
            r.setStatus(updated.getStatus());
            r.setDescription(updated.getDescription());
            r.setFloor(updated.getFloor());
            r.setCapacity(updated.getCapacity());
        });
        saveRoomsToFile();
    }

    public void deleteRoom(String roomNumber) {
        boolean active = bookings.stream()
                .anyMatch(b -> b.getRoomNumber().equals(roomNumber) && !b.isCheckedOut());
        if (active)
            throw new IllegalStateException("Cannot delete — room has an active booking.");
        rooms.removeIf(r -> r.getRoomNumber().equalsIgnoreCase(roomNumber));
        saveRoomsToFile();
    }

    public void setRoomMaintenance(String roomNumber, boolean maintenance) {
        findRoom(roomNumber).ifPresent(r -> {
            if (maintenance && r.getStatus() == RoomStatus.BOOKED)
                throw new IllegalStateException("Cannot mark booked room as maintenance.");
            r.setStatus(maintenance ? RoomStatus.MAINTENANCE : RoomStatus.AVAILABLE);
        });
        saveRoomsToFile();
    }

    // ── User Operations ─────────────────────────────────────────────────────

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public void addUser(User user) {
        boolean exists = users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(user.getUsername()));
        if (exists)
            throw new IllegalArgumentException("User " + user.getUsername() + " already exists.");

        String hashed = hashPassword(user.getPassword());
        user.setPassword(hashed);
        users.add(user);
        saveUsersToFile();
    }

    public void removeUser(String username) {
        users.removeIf(u -> u.getUsername().equalsIgnoreCase(username));
        saveUsersToFile();
    }

    public boolean authenticate(String username, String password) {
        String hashed = hashPassword(password);
        return users.stream().anyMatch(u -> u.getUsername().equals(username) && u.getPassword().equals(hashed));
    }

    public String getUserRole(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .map(User::getRole)
                .findFirst()
                .orElse("User");
    }

    // ── Booking Operations ───────────────────────────────────────────────────

    public Booking bookRoom(String name, String phone, String email,
            String roomNumber, int days,
            String specialRequests, String paymentMethod) {
        Room room = findRoom(roomNumber)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomNumber));
        if (room.getStatus() != RoomStatus.AVAILABLE)
            throw new IllegalStateException("Room " + roomNumber + " is not available.");

        double total = room.getPricePerNight() * days;
        String id = "BK-" + bookingCounter.getAndIncrement();
        Booking booking = new Booking(id, name, phone, email, roomNumber,
                days, LocalDate.now(), total, paymentMethod, specialRequests);
        bookings.add(booking);
        room.setStatus(RoomStatus.BOOKED);
        addGuestIfNew(name, phone, email);
        createBill(id, total, PaymentStatus.PENDING);
        saveBookingsToFile();
        saveRoomsToFile();
        saveCountersToFile();
        saveBillsToFile();
        return booking;
    }

    public Booking checkout(String bookingId) {
        Booking b = findBooking(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        if (b.isCheckedOut())
            throw new IllegalStateException("Booking " + bookingId + " is already checked out.");
        b.setCheckedOut(true);
        b.setCheckOutDate(LocalDate.now());
        findRoom(b.getRoomNumber()).ifPresent(r -> r.setStatus(RoomStatus.AVAILABLE));
        // Mark bill as paid
        bills.stream()
                .filter(bill -> bill.getBookingId().equals(bookingId))
                .forEach(bill -> bill.setPaymentStatus(PaymentStatus.PAID));
        saveBookingsToFile();
        saveRoomsToFile();
        saveBillsToFile();
        return b;
    }

    public Optional<Booking> findBooking(String bookingId) {
        return bookings.stream().filter(b -> b.getBookingId().equals(bookingId)).findFirst();
    }

    public List<Booking> getAllBookings() {
        return Collections.unmodifiableList(bookings);
    }

    public List<Booking> getActiveBookings() {
        return bookings.stream().filter(b -> !b.isCheckedOut()).collect(Collectors.toList());
    }

    public List<Booking> getBookingsForGuest(String name) {
        return bookings.stream()
                .filter(b -> b.getCustomerName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
    }

    // ── Guest Operations ─────────────────────────────────────────────────────

    public List<Guest> getGuests() {
        return Collections.unmodifiableList(guests);
    }

    /**
     * Names for the guest directory: registered guests plus any customer on a
     * booking
     * (so the list stays in sync even if registry dedupe skipped someone).
     */
    public List<String> getAllGuestNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Guest g : guests) {
            if (g.getName() != null && !g.getName().isBlank())
                names.add(g.getName().trim());
        }
        for (Booking b : bookings) {
            if (b.getCustomerName() != null && !b.getCustomerName().isBlank())
                names.add(b.getCustomerName().trim());
        }
        return names.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    /**
     * Adds a guest profile when missing. Duplicate detection: non-empty phone must
     * be unique;
     * empty phone dedupes by name (case-insensitive) so multiple blank-phone seed
     * guests
     * no longer block every new guest from appearing in the directory.
     */
    private void addGuestIfNew(String name, String phone, String email) {
        if (name == null || name.isBlank())
            return;
        String n = name.trim();
        String p = phone == null ? "" : phone.trim();
        String em = email == null ? "" : email.trim();

        boolean exists;
        if (!p.isEmpty()) {
            exists = guests.stream().anyMatch(g -> p.equals(g.getPhone() != null ? g.getPhone().trim() : ""));
        } else {
            exists = guests.stream().anyMatch(g -> {
                String gn = g.getName() != null ? g.getName().trim() : "";
                String gp = g.getPhone() != null ? g.getPhone().trim() : "";
                return n.equalsIgnoreCase(gn) && gp.isEmpty();
            });
        }
        if (!exists) {
            guests.add(new Guest("G-" + guestCounter.getAndIncrement(), n, p, em));
            saveGuestsToFile();
            saveCountersToFile();
        }
    }

    // ── Bill Operations ──────────────────────────────────────────────────────

    public Bill generateBill(String bookingId) {
        return findBooking(bookingId)
                .map(b -> {
                    Bill bill = new Bill("BILL-" + billCounter.getAndIncrement(),
                            bookingId, b.getTotalBill(), PaymentStatus.PENDING);
                    bills.add(bill);
                    saveCountersToFile();
                    saveBillsToFile();
                    return bill;
                }).orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
    }

    private void createBill(String bookingId, double amount, PaymentStatus status) {
        bills.add(new Bill("BILL-" + billCounter.getAndIncrement(), bookingId, amount, status));
    }

    public List<Bill> getBills() {
        return Collections.unmodifiableList(bills);
    }

    // ── Reports / Statistics ─────────────────────────────────────────────────

    public int getTotalRooms() {
        return rooms.size();
    }

    public int getAvailableRoomCount() {
        return (int) rooms.stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).count();
    }

    public int getBookedRooms() {
        return (int) rooms.stream().filter(r -> r.getStatus() == RoomStatus.BOOKED).count();
    }

    public int getMaintenanceRooms() {
        return (int) rooms.stream().filter(r -> r.getStatus() == RoomStatus.MAINTENANCE).count();
    }

    public int getActiveBookingCount() {
        return (int) bookings.stream().filter(b -> !b.isCheckedOut()).count();
    }

    public double getTotalRevenue() {
        return bookings.stream().filter(Booking::isCheckedOut).mapToDouble(Booking::getTotalBill).sum();
    }

    public double getPendingRevenue() {
        return bookings.stream().filter(b -> !b.isCheckedOut()).mapToDouble(Booking::getTotalBill).sum();
    }

    public double getRevenueByPaymentStatus(boolean checkedOut) {
        return bookings.stream()
                .filter(b -> b.isCheckedOut() == checkedOut)
                .mapToDouble(Booking::getTotalBill)
                .sum();
    }

    public Map<String, Object> getReports() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalRooms", getTotalRooms());
        r.put("availableRooms", getAvailableRoomCount());
        r.put("bookedRooms", getBookedRooms());
        r.put("maintenanceRooms", getMaintenanceRooms());
        r.put("totalBookings", bookings.size());
        r.put("activeBookings", getActiveBookingCount());
        r.put("totalRevenue", getTotalRevenue());
        r.put("pendingRevenue", getPendingRevenue());
        r.put("totalGuests", guests.size());
        return r;
    }

    // ── Settings helpers ─────────────────────────────────────────────────────

    public void resetData() {
        rooms.clear();
        bookings.clear();
        guests.clear();
        bills.clear();
        bookingCounter.set(1000);
        billCounter.set(5000);
        guestCounter.set(100);
        seedRooms();
        seedBookings();
        saveRoomsToFile();
        saveBookingsToFile();
        saveGuestsToFile();
        saveBillsToFile();
        saveCountersToFile();
    }
}
