package models;

public class DataClasses {

    // ─── USER ────────────────────────────────────────────────────────────────
    public static class User {
        public int    userId;
        public String firstName;
        public String lastName;
        public String email;
        public String contactNumber;
        public String userType;
        public String department;
        public String accountStatus;

        public String getFullName() {
            return firstName + " " + lastName;
        }

        @Override
        public String toString() {
            return String.format("[%d] %s %s | %s | %s | %s",
                userId, firstName, lastName, userType, department, accountStatus);
        }
    }

    // ─── ITEM ─────────────────────────────────────────────────────────────────
    public static class Item {
        public int    itemId;
        public String barcode;
        public String itemName;
        public String itemType;
        public String description;
        public String model;
        public String tag;
        public String conditionStatus;
        public String availabilityStatus;
        public String dateAcquired;

        @Override
        public String toString() {
            return String.format("[%d] %-25s | %-10s | %-10s | Cond: %-18s | Avail: %s",
                itemId, itemName, itemType, model == null ? "N/A" : model,
                conditionStatus, availabilityStatus);
        }
    }

    // ─── BORROW RECORD ───────────────────────────────────────────────────────
    public static class BorrowRecord {
        public int    borrowId;
        public int    borrowerId;
        public String borrowerName;
        public int    custodianId;
        public String custodianName;
        public String borrowDate;
        public String returnDate;
        public String purpose;
        public String status;
        public String remarks;

        @Override
        public String toString() {
            return String.format("[%d] Borrower: %-20s | Purpose: %-8s | Status: %-30s | Borrowed: %s | Return: %s",
                borrowId, borrowerName, purpose, status,
                borrowDate, returnDate == null ? "Not yet returned" : returnDate);
        }
    }

    // ─── BORROW ITEM ─────────────────────────────────────────────────────────
    public static class BorrowItem {
        public int    borrowItemId;
        public int    borrowId;
        public int    itemId;
        public String itemName;
        public String barcode;
        public int    quantity;
        public String itemConditionOnReturn;

        @Override
        public String toString() {
            return String.format("  - %-25s | Barcode: %-12s | Qty: %d | Return Condition: %s",
                itemName, barcode, quantity,
                itemConditionOnReturn == null ? "N/A" : itemConditionOnReturn);
        }
    }

    // ─── LABORATORY CLASS ────────────────────────────────────────────────────
    public static class LaboratoryClass {
        public int    classId;
        public String classCode;
        public String className;
        public int    instructorId;
        public String instructorName;
        public String room;
        public String scheduleDay;
        public String scheduleTime;
        public String semester;
        public String academicYear;

        @Override
        public String toString() {
            return String.format("[%d] %-12s | %-40s | Instructor: %-20s | %s %s | Room: %s | %s SY%s",
                classId, classCode, className, instructorName,
                scheduleDay, scheduleTime, room, semester, academicYear);
        }
    }

    // ─── ACTIVITY ─────────────────────────────────────────────────────────────
    public static class Activity {
        public int    activityId;
        public String activityName;
        public String eventType;
        public String eventDate;
        public String eventTime;
        public String location;
        public String requesterName;
        public String approvedByName;
        public String approvalStatus;

        @Override
        public String toString() {
            return String.format("[%d] %-30s | %-15s | %s %s | Status: %-10s | Requestor: %s",
                activityId, activityName, eventType, eventDate,
                eventTime == null ? "" : eventTime, approvalStatus, requesterName);
        }
    }

    // ─── RETURN RECORD ───────────────────────────────────────────────────────
    public static class ReturnRecord {
        public int    returnId;
        public int    borrowId;
        public String custodianName;
        public String actualReturnDate;
        public String conditionNotes;
        public String hasDamage;
        public String damageDescription;

        @Override
        public String toString() {
            return String.format("[Return #%d] Borrow ID: %d | Returned: %s | Damage: %s | Notes: %s",
                returnId, borrowId, actualReturnDate, hasDamage,
                conditionNotes == null ? "None" : conditionNotes);
        }
    }
}