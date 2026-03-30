package models;

public class Facility {
    private int    facilityId;
    private String facilityName;
    private String roomNumber;

    public Facility(int facilityId, String facilityName, String roomNumber) {
        this.facilityId   = facilityId;
        this.facilityName = facilityName;
        this.roomNumber   = roomNumber;
    }

    public int    getFacilityId()   { return facilityId; }
    public String getFacilityName() { return facilityName; }
    public String getRoomNumber()   { return roomNumber; }

    @Override
    public String toString() {
        return String.format("  [%d] %-25s Room: %s",
            facilityId, facilityName, roomNumber);
    }
}