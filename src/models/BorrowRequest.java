package models;

import java.sql.Date;

public class BorrowRequest {
    private int    requestId;
    private String requesterName;
    private String activityName;
    private Date   requestDate;
    private String status;
    private String approvedByName;
    private String remarks;

    public BorrowRequest(int requestId, String requesterName, String activityName,
                         Date requestDate, String status, String approvedByName,
                         String remarks) {
        this.requestId      = requestId;
        this.requesterName  = requesterName;
        this.activityName   = activityName;
        this.requestDate    = requestDate;
        this.status         = status;
        this.approvedByName = approvedByName;
        this.remarks        = remarks;
    }

    public int    getRequestId()      { return requestId; }
    public String getRequesterName()  { return requesterName; }
    public String getActivityName()   { return activityName; }
    public Date   getRequestDate()    { return requestDate; }
    public String getStatus()         { return status; }
    public String getApprovedByName() { return approvedByName; }
    public String getRemarks()        { return remarks; }

    @Override
    public String toString() {
        return String.format("  [%d] %-22s %-25s %-10s %-10s %s",
            requestId, requesterName, activityName, requestDate, status,
            (remarks != null ? remarks : "—"));
    }
}