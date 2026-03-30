package models;

public class User {
    private int    userId;
    private String firstName;
    private String lastName;
    private String email;
    private String contactNumber;
    private String type;
    private String department;

    public User(int userId, String firstName, String lastName,
                String email, String contactNumber, String type, String department) {
        this.userId        = userId;
        this.firstName     = firstName;
        this.lastName      = lastName;
        this.email         = email;
        this.contactNumber = contactNumber;
        this.type          = type;
        this.department    = department;
    }

    public int    getUserId()        { return userId; }
    public String getFirstName()     { return firstName; }
    public String getLastName()      { return lastName; }
    public String getFullName()      { return firstName + " " + lastName; }
    public String getEmail()         { return email; }
    public String getContactNumber() { return contactNumber; }
    public String getType()          { return type; }
    public String getDepartment()    { return department; }

    @Override
    public String toString() {
        return String.format("  [%d] %-25s %-12s %-30s %s",
            userId, getFullName(), type, email, department);
    }
}