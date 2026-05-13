package service;

import dao.Database;
import models.DataClasses;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.Scanner;

/**
 * Contains custodian-side functions.
 *
 * Custodians handle the main transaction cycle of the system:
 * 1. View pending requests.
 * 2. Create walk-in requests when needed.
 * 3. Approve/reject and check out items.
 * 4. Log returned items and damage information.
 * 5. Maintain equipment records and monitor unreturned items.
 */
public class CustodianService {

    /** Lists all pending borrow requests waiting for custodian action. */
    public void viewPendingRequests() {
        // View joins request, borrower, and item details so the custodian can decide quickly.
        String sql = """
                SELECT request_id, borrower_name, request_date, purpose, purpose_ref, items
                FROM vw_pending_borrow_requests
                ORDER BY request_date ASC, request_id ASC
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
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
        } catch (Exception e) {
            System.out.println("  Unable to load pending requests: " + AuthService.cleanMessage(e));
        }
    }

    /**
     * Allows a custodian to encode a borrower's request at the counter.
     * It still uses the same borrower_CreateBorrowRequest procedure so validation stays consistent.
     */
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

        // Reuses the borrower request procedure instead of duplicating request-insert logic.
        String sql = "{CALL borrower_CreateBorrowRequest(?, ?, ?, ?, ?)}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, borrowerId);
            cs.setString(2, purpose);
            cs.setString(3, purposeRef.isBlank() ? null : purposeRef);
            cs.setString(4, itemIds);
            // OUT parameter returns the new borrow_id when the request is approved.
            cs.registerOutParameter(5, Types.INTEGER);
            cs.execute();
            System.out.println("  Walk-in borrow request created by custodian " + custodian.getFullName()
                    + ". Request ID: " + cs.getInt(5));
        } catch (Exception e) {
            System.out.println("  Walk-in request was not created: " + AuthService.cleanMessage(e));
        }
    }

    /**
     * Approves or rejects a pending request.
     * If approved, the stored procedure creates borrow_record/borrow_item rows
     * and marks the requested items as Borrowed.
     */
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

        // Procedure performs the checkout transaction and protects against empty/unavailable requests.
        String sql = "{CALL custodian_ProcessBorrowRequest(?, ?, ?, ?, ?)}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, requestId);
            cs.setInt(2, custodian.userId);
            cs.setString(3, decision);
            cs.setString(4, remarks.isBlank() ? null : remarks);
            // OUT parameter returns the new borrow_id when the request is approved.
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

    /**
     * Logs the return of an active borrow record.
     * The procedure updates borrow_record, borrow_item, return_record, and item statuses.
     */
    public void logReturnedItems(DataClasses.User custodian, Scanner sc) {
        viewUnreturnedAndIssues();
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

        // Stored procedure keeps return logging atomic so records do not become inconsistent.
        String sql = "{CALL custodian_LogReturn(?, ?, ?, ?, ?)}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
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

    /** Adds a new physical item to inventory. Newly added items start as Good and Available. */
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

        // INSERT creates the inventory record; generated keys returns the new item_id.
        String sql = """
                INSERT INTO item (barcode, item_name, item_type, description, model, tag,
                                  condition_status, availability_status, date_acquired)
                VALUES (?, ?, ?, ?, ?, ?, 'Good', 'Available', CURDATE())
                """;
        try (Connection conn = Database.getConnection();
             // RETURN_GENERATED_KEYS lets Java display the auto-generated item_id after insert.
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, barcode);
            ps.setString(2, name);
            ps.setString(3, itemType);
            ps.setString(4, description);
            ps.setString(5, model);
            ps.setString(6, tag);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) System.out.println("  Item added successfully. Item ID: " + keys.getInt(1));
                else System.out.println("  Item added successfully.");
            }
        } catch (Exception e) {
            System.out.println("  Item was not added: " + AuthService.cleanMessage(e));
        }
    }

    /**
     * Updates item condition/status only if the item is not currently borrowed.
     * This protects the borrowing cycle from changing active borrowed items incorrectly.
     */
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
        System.out.print("  New availability [Available/Borrowed/Unavailable]: ");
        String availability = sc.nextLine().trim();
        if (!availability.equals("Available") && !availability.equals("Borrowed") && !availability.equals("Unavailable")) {
            System.out.println("  Invalid availability status.");
            return;
        }

        // NOT EXISTS prevents updates to items that are part of active borrow records.
        String sql = """
                UPDATE item
                SET condition_status = ?, availability_status = ?
                WHERE item_id = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM borrow_item bi
                      JOIN borrow_record br ON br.borrow_id = bi.borrow_id
                      WHERE bi.item_id = item.item_id
                        AND br.status IN ('Borrowed', 'Overdue')
                  )
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, condition);
            ps.setString(2, availability);
            ps.setInt(3, itemId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                System.out.println("  Item was not updated. It may not exist or may currently be borrowed.");
            } else {
                System.out.println("  Item condition/status updated successfully.");
            }
        } catch (Exception e) {
            System.out.println("  Item was not updated: " + AuthService.cleanMessage(e));
        }
    }

    /** Demonstrates Statement by reading an inventory view with no user input parameters. */
    public void viewEquipmentStatus() {
        String sql = "SELECT * FROM vw_item_inventory_status ORDER BY item_type, item_name";
        try (Connection conn = Database.getConnection();
             // Statement is acceptable here because the query has no external/user input.
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            BorrowerService.printItems(rs, "EQUIPMENT / INVENTORY STATUS");
            System.out.println("  JDBC interface shown here: Statement");
        } catch (Exception e) {
            System.out.println("  Unable to load equipment status: " + AuthService.cleanMessage(e));
        }
    }

    /** Displays all borrow records for monitoring and reporting. */
    public void viewBorrowRecords() {
        String sql = """
                SELECT borrow_id, borrower_name, custodian_name, borrow_date, return_date,
                       purpose, status, item_count, items
                FROM vw_borrow_history_details
                ORDER BY borrow_date DESC, borrow_id DESC
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
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
        } catch (Exception e) {
            System.out.println("  Unable to load borrow records: " + AuthService.cleanMessage(e));
        }
    }

    /** Shows active, overdue, or damaged return records that need custodian attention. */
    public void viewUnreturnedAndIssues() {
        String sql = """
                SELECT borrow_id, borrower_name, custodian_name, borrow_date, purpose, status, item_count, items
                FROM vw_borrow_history_details
                WHERE status IN ('Borrowed', 'Overdue', 'Returned with Damage/s')
                ORDER BY FIELD(status, 'Overdue', 'Borrowed', 'Returned with Damage/s'), borrow_date
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
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
        } catch (Exception e) {
            System.out.println("  Unable to load records: " + AuthService.cleanMessage(e));
        }
    }

    // Reusable validation helper for required text fields.
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

    // Reusable validation helper for optional text fields.
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
