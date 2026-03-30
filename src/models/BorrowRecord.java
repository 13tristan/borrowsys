package models;

import java.sql.Date;

public class BorrowRecord {
    private int    borrowId;
    private String borrowerName;
    private String custodianName;
    private Date   borrowedDate;
    private String purpose;
    private Date   returnDate;
    private String status;
    private String remarks;

    public BorrowRecord(int borrowId, String borrowerName, String custodianName,
                        Date borrowedDate, String purpose, Date returnDate,
                        String status, String remarks) {
        this.borrowId      = borrowId;
        this.borrowerName  = borrowerName;
        this.custodianName = custodianName;
        this.borrowedDate  = borrowedDate;
        this.purpose       = purpose;
        this.returnDate    = returnDate;
        this.status        = status;
        this.remarks       = remarks;
    }

    public int    getBorrowId()      { return borrowId; }
    public String getBorrowerName()  { return borrowerName; }
    public String getCustodianName() { return custodianName; }
    public Date   getBorrowedDate()  { return borrowedDate; }
    public String getPurpose()       { return purpose; }
    public Date   getReturnDate()    { return returnDate; }
    public String getStatus()        { return status; }
    public String getRemarks()       { return remarks; }

    @Override
    public String toString() {
        return String.format("  [%d] %-22s %-10s %-12s %-28s %s",
            borrowId, borrowerName, borrowedDate, purpose, status,
            (remarks != null ? remarks : "—"));
    }
}