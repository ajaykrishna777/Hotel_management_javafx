package model;

public class Room {

    public enum RoomType {
        STANDARD, DELUXE, SUITE
    }

    public enum RoomStatus {
        AVAILABLE, BOOKED, MAINTENANCE
    }

    private String roomNumber;
    private RoomType roomType;
    private double pricePerNight;
    private RoomStatus status;
    private String description;
    private int floor; // Added missing field
    private int capacity; // Added missing field

    // Original constructor (keep for backward compatibility)
    public Room(String roomNumber, RoomType roomType, double pricePerNight,
            RoomStatus status, String description) {
        this(roomNumber, roomType, pricePerNight, status, description, 1, 2);
    }

    // Full constructor with floor and capacity
    public Room(String roomNumber, RoomType roomType, double pricePerNight,
            RoomStatus status, String description, int floor, int capacity) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.status = status;
        this.description = description;
        this.floor = floor;
        this.capacity = capacity;
    }

    public String toFileString() {
        return roomNumber + "|" + roomType.name() + "|" + pricePerNight
                + "|" + status.name() + "|" + description + "|" + floor + "|" + capacity;
    }

    public static Room fromFileString(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 5)
            throw new IllegalArgumentException("Invalid room data: " + line);
        if (p.length >= 7) {
            return new Room(p[0].trim(), RoomType.valueOf(p[1].trim()),
                    Double.parseDouble(p[2].trim()), RoomStatus.valueOf(p[3].trim()),
                    p[4].trim(), Integer.parseInt(p[5].trim()), Integer.parseInt(p[6].trim()));
        } else {
            return new Room(p[0].trim(), RoomType.valueOf(p[1].trim()),
                    Double.parseDouble(p[2].trim()), RoomStatus.valueOf(p[3].trim()), p[4].trim());
        }
    }

    // Getters
    public String getRoomNumber() {
        return roomNumber;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public int getFloor() {
        return floor;
    }

    public int getCapacity() {
        return capacity;
    }

    // Setters
    public void setRoomNumber(String n) {
        roomNumber = n;
    }

    public void setRoomType(RoomType t) {
        roomType = t;
    }

    public void setPricePerNight(double p) {
        pricePerNight = p;
    }

    public void setStatus(RoomStatus s) {
        status = s;
    }

    public void setDescription(String d) {
        description = d;
    }

    public void setFloor(int f) {
        floor = f;
    }

    public void setCapacity(int c) {
        capacity = c;
    }

    @Override
    public String toString() {
        return "Room[" + roomNumber + ", " + roomType + ", ₹" + pricePerNight + ", " + status + "]";
    }
}