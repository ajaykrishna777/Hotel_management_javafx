package model;

public class Bill {

    public enum PaymentStatus { PAID, PENDING, PARTIAL }

    private String        billId;
    private String        bookingId;
    private double        amount;
    private PaymentStatus paymentStatus;

    public Bill(String billId, String bookingId, double amount, PaymentStatus paymentStatus) {
        this.billId        = billId;
        this.bookingId     = bookingId;
        this.amount        = amount;
        this.paymentStatus = paymentStatus;
    }

    // Getters
    public String        getBillId()        { return billId; }
    public String        getBookingId()     { return bookingId; }
    public double        getAmount()        { return amount; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }

    // Setters
    public void setBillId(String billId)               { this.billId        = billId; }
    public void setBookingId(String bookingId)         { this.bookingId     = bookingId; }
    public void setAmount(double amount)               { this.amount        = amount; }
    public void setPaymentStatus(PaymentStatus status) { this.paymentStatus = status; }

    @Override
    public String toString() {
        return "Bill[" + billId + ", Booking:" + bookingId + ", ₹" + amount + ", " + paymentStatus + "]";
    }
}
