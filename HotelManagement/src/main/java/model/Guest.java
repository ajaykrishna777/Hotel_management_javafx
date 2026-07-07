package model;

public class Guest {

    private String guestId;
    private String name;
    private String phone;
    private String email;

    public Guest(String guestId, String name, String phone, String email) {
        this.guestId = guestId;
        this.name    = name;
        this.phone   = phone;
        this.email   = email;
    }

    // Getters
    public String getGuestId() { return guestId; }
    public String getName()    { return name; }
    public String getPhone()   { return phone; }
    public String getEmail()   { return email; }

    // Setters
    public void setGuestId(String guestId) { this.guestId = guestId; }
    public void setName(String name)       { this.name    = name; }
    public void setPhone(String phone)     { this.phone   = phone; }
    public void setEmail(String email)     { this.email   = email; }

    @Override
    public String toString() {
        return "Guest[" + guestId + ", " + name + "]";
    }
}
