package service;

import dao.Database;
import models.DataClasses;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Scanner;

/**
 * Contains custodian-side functions.
 *
 * This final version calls stored procedures for every database operation.
 * Read-only screens call procedures that return ResultSets, while transaction
 * features call procedures that perform validation and updates inside MySQL.
 */
public class CustodianService {

    public void viewPendingRequests() {
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL custodian_ViewPendingRequests()}")) {
            try (ResultSet rs = cs.executeQuery()) {
                System.out.println("\n  PENDING BORROW REQUESTS");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("  Request #%d | Borrower: %-20s | Date: %s | Purpose: %s%n",
                            rs.getInt("request_id"), rs.getString("borrower_name"),
                            rs.getString("request_date"), rs.getString("purpose"));
                    System.out.println("    Ref: " + BorrowerService.nvl(rs.getString("purpose_ref")));
                    System.out.println("    Items: " + BorrowerService.nvl(rs.getString("items")));
                }
                if (!found) System.out.println("  No pending requests.");
            }
        } catch (Exception e) {
            System.out.println("  Unable to load pending requests: " + AuthService.cleanMessage(e));
        }
    }

    public void createWalkInBorrowRequest(DataClasses.User custodian, Scanner sc) {
        System.out.println("\n  Create Walk-in Borrow Request");
        System.out.print("  Borrower user ID: ");
        Integer borrowerId = BorrowerService.readInt(sc.nextLine());
        if (borrowerId == null) {
            System.out.println("  Invalid borrower ID.");
            return;
        }

        viewEquipmentStatus();
        System.out.println("\n  Purpose: [1] Class  [2] Activity  [3] Other");
        System.out.print("  Choice: ");
        String purpose = switch (sc.nextLine().trim()) {
            case "1" -> "Class";
            case "2" -> "Activity";
            case "3" -> "Other";
            default -> null;
        };
        if (purpose == null) {
            System.out.println("  Invalid purpose.");
            return;
        }

        System.out.print("  Purpose reference (class code, event name, or short reason): ");
        String purposeRef = sc.nextLine().trim();
        if (purposeRef.length() > 200) {
            System.out.println("  Purpose reference must not exceed 200 characters.");
            return;
        }

        System.out.print("  Enter item ID/s to request (comma-separated, no quantity needed): ");
        String itemIds = BorrowerService.normalizeItemIds(sc.nextLine());
        if (itemIds == null) return;

        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL borrower_CreateBorrowRequest(?, ?, ?, ?, ?)}")) {
            cs.setInt(1, borrowerId);
            cs.setString(2, purpose);
            cs.setString(3, purposeRef.isBlank() ? null : purposeRef);
            cs.setString(4, itemIds);
            cs.registerOutParameter(5, Types.INTEGER);
            cs.execute();
            System.out.println("  Walk-in borrow request created by custodian " + custodian.getFullName()
                    + ". Request ID: " + cs.getInt(5));
        } catch (Exception e) {
            System.out.println("  Walk-in request was not created: " + AuthService.cleanMessage(e));
        }
    }

    public void processBorrowRequest(DataClasses.User custodian, Scanner sc) {
        viewPendingRequests();
        System.out.print("\n  Request ID to process: ");
        Integer requestId = BorrowerService.readInt(sc.nextLine());
        if (requestId == null) {
            System.out.println("  Invalid request ID.");
            return;
        }
        System.out.print("  Decision [Approve/Reject]: ");
        String decisionInput = sc.nextLine().trim();
        String decision;
        if (decisionInput.equalsIgnoreCase("Approve") || decisionInput.equalsIgnoreCase("Approved")) {
            decision = "Approved";
        } else if (decisionInput.equalsIgnoreCase("Reject") || decisionInput.equalsIgnoreCase("Rejected")) {
            decision = "Rejected";
        } else {
            System.out.println("  Decision must be Approve or Reject.");
            return;
        }
        System.out.print("  Remarks: ");
        String remarks = sc.nextLine().trim();
        if (remarks.length() > 500) {
            System.out.println("  Remarks must not exceed 500 characters.");
            return;
        }

        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL custodian_ProcessBorrowRequest(?, ?, ?, ?, ?)}")) {
            cs.setInt(1, requestId);
            cs.setInt(2, custodian.userId);
            cs.setString(3, decision);
            cs.setString(4, remarks.isBlank() ? null : remarks);
            cs.registerOutParameter(5, Types.INTEGER);
            cs.execute();
            int borrowId = cs.getInt(5);
            if (decision.equals("Approved")) {
                System.out.println("  Request approved and checked out. Borrow ID: " + borrowId);
            } else {
                System.out.println("  Request rejected.");
            }
        } catch (Exception e) {
            System.out.println("  Request was not processed: " + AuthService.cleanMessage(e));
        }
    }

    public void logReturnedItems(DataClasses.User custodian, Scanner sc) {
        viewReturnEligibleRecords();
        System.out.print("\n  Borrow ID to return: ");
        Integer borrowId = BorrowerService.readInt(sc.nextLine());
        if (borrowId == null) {
            System.out.println("  Invalid borrow ID.");
            return;
        }
        System.out.print("  Was there any damage? [Yes/No]: ");
        String hasDamage = sc.nextLine().trim();
        if (!hasDamage.equalsIgnoreCase("Yes") && !hasDamage.equalsIgnoreCase("No")) {
            System.out.println("  Answer must be Yes or No.");
            return;
        }
        hasDamage = hasDamage.equalsIgnoreCase("Yes") ? "Yes" : "No";

        System.out.print("  Condition notes: ");
        String notes = sc.nextLine().trim();
        if (notes.length() > 500) {
            System.out.println("  Condition notes must not exceed 500 characters.");
            return;
        }

        String damageDescription = null;
        if (hasDamage.equals("Yes")) {
            System.out.print("  Damage description: ");
            damageDescription = sc.nextLine().trim();
            if (damageDescription.isBlank()) {
                System.out.println("  Damage description is required when damage is marked Yes.");
                return;
            }
            if (damageDescription.length() > 500) {
                System.out.println("  Damage description must not exceed 500 characters.");
                return;
            }
        }

        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL custodian_LogReturn(?, ?, ?, ?, ?)}")) {
            cs.setInt(1, borrowId);
            cs.setInt(2, custodian.userId);
            cs.setString(3, hasDamage);
            cs.setString(4, notes.isBlank() ? null : notes);
            cs.setString(5, damageDescription);
            cs.execute();
            System.out.println("  Return logged successfully.");
        } catch (Exception e) {
            System.out.println("  Return was not logged: " + AuthService.cleanMessage(e));
        }
    }

    public void addNewItem(Scanner sc) {
        System.out.println("\n  Add Equipment / Accessory / Peripheral");
        String barcode = promptRequired(sc, "Barcode", 50);
        String name = promptRequired(sc, "Item name", 100);
        System.out.print("  Item type [Equipment/Accessory/Peripheral]: ");
        String itemType = sc.nextLine().trim();
        if (!itemType.equals("Equipment") && !itemType.equals("Accessory") && !itemType.equals("Peripheral")) {
            System.out.println("  Item type must be Equipment, Accessory, or Peripheral.");
            return;
        }
        String description = promptOptional(sc, "Description", 200);
        String model = promptOptional(sc, "Model", 100);
        String tag = promptOptional(sc, "Tag", 50);
        if (barcode == null || name == null) return;

        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL custodian_AddItem(?, ?, ?, ?, ?, ?, ?)}")) {
            cs.setString(1, barcode);
            cs.setString(2, name);
            cs.setString(3, itemType);
            cs.setString(4, description.isBlank() ? null : description);
            cs.setString(5, model.isBlank() ? null : model);
            cs.setString(6, tag.isBlank() ? null : tag);
            cs.registerOutParameter(7, Types.INTEGER);
            cs.execute();
            System.out.println("  Item added successfully. Item ID: " + cs.getInt(7));
        } catch (Exception e) {
            System.out.println("  Item was not added: " + AuthService.cleanMessage(e));
        }
    }

    public void updateItemConditionStatus(Scanner sc) {
        viewEquipmentStatus();
        System.out.print("\n  Item ID to update: ");
        Integer itemId = BorrowerService.readInt(sc.nextLine());
        if (itemId == null) {
            System.out.println("  Invalid item ID.");
            return;
        }
        System.out.print("  New condition [Good/Needs Repair/Damaged/Lost]: ");
        String condition = sc.nextLine().trim();
        if (!condition.equals("Good") && !condition.equals("Needs Repair") && !condition.equals("Damaged") && !condition.equals("Lost")) {
            System.out.println("  Invalid condition.");
            return;
        }
        System.out.print("  New availability [Available/Unavailable]: ");
        String availability = sc.nextLine().trim();
        if (!availability.equals("Available") && !availability.equals("Unavailable")) {
            System.out.println("  Availability can only be Available or Unavailable here. Borrowed is set only through checkout.");
            return;
        }

        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL custodian_UpdateItemStatus(?, ?, ?)}")) {
            cs.setInt(1, itemId);
            cs.setString(2, condition);
            cs.setString(3, availability);
            cs.execute();
            System.out.println("  Item condition/status updated successfully.");
        } catch (Exception e) {
            System.out.println("  Item was not updated: " + AuthService.cleanMessage(e));
        }
    }

    public void viewEquipmentStatus() {
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL custodian_ViewEquipmentStatus()}")) {
            try (ResultSet rs = cs.executeQuery()) {
                BorrowerService.printItems(rs, "EQUIPMENT / INVENTORY STATUS");
            }
        } catch (Exception e) {
            System.out.println("  Unable to load equipment status: " + AuthService.cleanMessage(e));
        }
    }

    public void viewBorrowRecords() {
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL custodian_ViewBorrowRecords()}")) {
            try (ResultSet rs = cs.executeQuery()) {
                System.out.println("\n  BORROW RECORDS");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("  Borrow #%d | Borrower: %-20s | Status: %-22s | Items: %d%n",
                            rs.getInt("borrow_id"), rs.getString("borrower_name"), rs.getString("status"),
                            rs.getInt("item_count"));
                    System.out.println("    Custodian: " + BorrowerService.nvl(rs.getString("custodian_name"))
                            + " | Borrowed: " + rs.getString("borrow_date")
                            + " | Returned: " + BorrowerService.nvl(rs.getString("return_date")));
                    System.out.println("    Items: " + BorrowerService.nvl(rs.getString("items")));
                }
                if (!found) System.out.println("  No borrow records found.");
            }
        } catch (Exception e) {
            System.out.println("  Unable to load borrow records: " + AuthService.cleanMessage(e));
        }
    }

    private void viewReturnEligibleRecords() {
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL custodian_ViewReturnEligibleRecords()}")) {
            try (ResultSet rs = cs.executeQuery()) {
                System.out.println("\n  BORROW RECORDS ELIGIBLE FOR RETURN");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("  Borrow #%d | Borrower: %-20s | Status: %-10s | Items: %d%n",
                            rs.getInt("borrow_id"), rs.getString("borrower_name"), rs.getString("status"),
                            rs.getInt("item_count"));
                    System.out.println("    Borrowed: " + rs.getString("borrow_date")
                            + " | Purpose: " + rs.getString("purpose"));
                    System.out.println("    Items: " + BorrowerService.nvl(rs.getString("items")));
                }
                if (!found) System.out.println("  No borrowed or overdue records are eligible for return.");
            }
        } catch (Exception e) {
            System.out.println("  Unable to load return-eligible records: " + AuthService.cleanMessage(e));
        }
    }

    public void viewUnreturnedAndIssues() {
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL custodian_ViewUnreturnedAndIssues()}")) {
            try (ResultSet rs = cs.executeQuery()) {
                System.out.println("\n  UNRETURNED ITEMS / RETURNS WITH ISSUES");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("  Borrow #%d | Borrower: %-20s | Status: %-22s | Items: %d%n",
                            rs.getInt("borrow_id"), rs.getString("borrower_name"), rs.getString("status"),
                            rs.getInt("item_count"));
                    System.out.println("    Borrowed: " + rs.getString("borrow_date")
                            + " | Purpose: " + rs.getString("purpose"));
                    System.out.println("    Items: " + BorrowerService.nvl(rs.getString("items")));
                }
                if (!found) System.out.println("  No unreturned items or issue records found.");
            }
        } catch (Exception e) {
            System.out.println("  Unable to load records: " + AuthService.cleanMessage(e));
        }
    }

    private String promptRequired(Scanner sc, String label, int maxLength) {
        System.out.print("  " + label + ": ");
        String value = sc.nextLine().trim();
        if (value.isBlank()) {
            System.out.println("  " + label + " is required.");
            return null;
        }
        if (value.length() > maxLength) {
            System.out.println("  " + label + " must not exceed " + maxLength + " characters.");
            return null;
        }
        return value;
    }

    private String promptOptional(Scanner sc, String label, int maxLength) {
        System.out.print("  " + label + ": ");
        String value = sc.nextLine().trim();
        if (value.length() > maxLength) {
            System.out.println("  " + label + " must not exceed " + maxLength + " characters.");
            return "";
        }
        return value;
    }
}
