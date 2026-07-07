package model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Booking {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private String bookingId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String roomNumber;
    private int numberOfDays;
    private double totalBill;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private boolean checkedOut;
    private String paymentMethod;
    private String specialRequests;

    // Full constructor with all fields
    public Booking(String bookingId, String customerName, String customerPhone,
            String customerEmail, String roomNumber, int numberOfDays,
            LocalDate checkInDate, double totalBill, String paymentMethod,
            String specialRequests) {
        this.bookingId = bookingId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerEmail = customerEmail != null ? customerEmail : "";
        this.roomNumber = roomNumber;
        this.numberOfDays = numberOfDays;
        this.totalBill = totalBill;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkInDate.plusDays(numberOfDays);
        this.checkedOut = false;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH";
        this.specialRequests = specialRequests != null ? specialRequests : "";
    }

    // Simplified constructor (for backward compatibility)
    public Booking(String bookingId, String customerName, String customerPhone,
            String roomNumber, int numberOfDays, double totalBill,
            LocalDate checkInDate, boolean checkedOut) {
        this(bookingId, customerName, customerPhone, "", roomNumber,
                numberOfDays, checkInDate, totalBill, "CASH", "");
        this.checkedOut = checkedOut;
    }

    public String toFileString() {
        return bookingId + "|" + customerName + "|" + customerPhone + "|"
                + customerEmail + "|" + roomNumber + "|" + numberOfDays + "|"
                + totalBill + "|" + checkInDate.format(FMT) + "|" + checkedOut
                + "|" + paymentMethod + "|" + specialRequests;
    }

    public static Booking fromFileString(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 8)
            throw new IllegalArgumentException("Invalid booking data: " + line);

        if (p.length >= 11) {
            return new Booking(p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(),
                    p[4].trim(), Integer.parseInt(p[5].trim()),
                    LocalDate.parse(p[6].trim(), FMT),
                    Double.parseDouble(p[7].trim()), p[9].trim(), p[10].trim());
        } else {
            Booking b = new Booking(p[0].trim(), p[1].trim(), p[2].trim(), p[4].trim(),
                    Integer.parseInt(p[5].trim()), Double.parseDouble(p[6].trim()),
                    LocalDate.parse(p[7].trim(), FMT), Boolean.parseBoolean(p[8].trim()));
            return b;
        }
    }

    // Getters
    public String getBookingId() {
        return bookingId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public int getNumberOfDays() {
        return numberOfDays;
    }

    public double getTotalBill() {
        return totalBill;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public boolean isCheckedOut() {
        return checkedOut;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    // Setters
    public void setCheckedOut(boolean b) {
        this.checkedOut = b;
    }

    public void setCheckOutDate(LocalDate date) {
        this.checkOutDate = date;
    }

    public void setPaymentMethod(String pm) {
        this.paymentMethod = pm;
    }
}