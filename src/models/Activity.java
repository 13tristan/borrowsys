package models;

import java.sql.Date;

public class Activity {
    private int    activityId;
    private String activityName;
    private String facilityName;
    private String eventType;
    private Date   eventDate;
    private String requesterName;
    private String approvedByName;
    private String approvalStatus;

    public Activity(int activityId, String activityName, String facilityName,
                    String eventType, Date eventDate, String requesterName,
                    String approvedByName, String approvalStatus) {
        this.activityId     = activityId;
        this.activityName   = activityName;
        this.facilityName   = facilityName;
        this.eventType      = eventType;
        this.eventDate      = eventDate;
        this.requesterName  = requesterName;
        this.approvedByName = approvedByName;
        this.approvalStatus = approvalStatus;
    }

    public int    getActivityId()     { return activityId; }
    public String getActivityName()   { return activityName; }
    public String getFacilityName()   { return facilityName; }
    public String getEventType()      { return eventType; }
    public Date   getEventDate()      { return eventDate; }
    public String getRequesterName()  { return requesterName; }
    public String getApprovedByName() { return approvedByName; }
    public String getApprovalStatus() { return approvalStatus; }

    @Override
    public String toString() {
        return String.format("  [%d] %-25s %-18s %-10s %-12s %s",
            activityId, activityName, eventType, eventDate, approvalStatus, facilityName);
    }
}