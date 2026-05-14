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
 * All methods now use CallableStatement to interact with the database via stored procedures.
 */
public class CustodianService {

    /** CASE [1]: Lists all pending borrow requests using custodian_ViewPendingRequests. */
    public void viewPendingRequests() {
        String sql = "{CALL custodian_ViewPendingRequests()}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {

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

    /** CASE [2]: Creates walk-in request via borrower_CreateBorrowRequest. */
    public void createWalkInBorrowRequest(DataClasses.User custodian, Scanner sc) {
        System.out.println("\n  Create Walk-in Borrow Request");
        System.out.print("  Borrower user ID: ");
        Integer borrowerId = BorrowerService.readInt(sc.nextLine());
        if (borrowerId == null) return;

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

        System.out.print("  Purpose reference: ");
        String purposeRef = sc.nextLine().trim();

        System.out.print("  Enter item ID/s (comma-separated): ");
        String itemIds = BorrowerService.normalizeItemIds(sc.nextLine());
        if (itemIds == null) return;

        String sql = "{CALL borrower_CreateBorrowRequest(?, ?, ?, ?, ?)}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, borrowerId);
            cs.setString(2, purpose);
            cs.setString(3, purposeRef.isBlank() ? null : purposeRef);
            cs.setString(4, itemIds);
            cs.registerOutParameter(5, Types.INTEGER);
            cs.execute();
            System.out.println("  Request created. ID: " + cs.getInt(5));
        } catch (Exception e) {
            System.out.println("  Error: " + AuthService.cleanMessage(e));
        }
    }

    /** CASE [3]: Approves/Rejects request via custodian_ProcessBorrowRequest. */
    public void processBorrowRequest(DataClasses.User custodian, Scanner sc) {
        viewPendingRequests();
        System.out.print("\n  Request ID to process: ");
        Integer requestId = BorrowerService.readInt(sc.nextLine());
        if (requestId == null) return;

        System.out.print("  Decision [Approve/Reject]: ");
        String decision = sc.nextLine().trim().equalsIgnoreCase("Approve") ? "Approved" : "Rejected";

        System.out.print("  Remarks: ");
        String remarks = sc.nextLine().trim();

        String sql = "{CALL custodian_ProcessBorrowRequest(?, ?, ?, ?, ?)}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, requestId);
            cs.setInt(2, custodian.userId);
            cs.setString(3, decision);
            cs.setString(4, remarks.isBlank() ? null : remarks);
            cs.registerOutParameter(5, Types.INTEGER);
            cs.execute();

            if (decision.equals("Approved")) {
                System.out.println("  Approved. New Borrow ID: " + cs.getInt(5));
            } else {
                System.out.println("  Request rejected.");
            }
        } catch (Exception e) {
            System.out.println("  Error: " + AuthService.cleanMessage(e));
        }
    }

    /** CASE [4]: Logs return via custodian_LogReturn. */
    public void logReturnedItems(DataClasses.User custodian, Scanner sc) {
        viewUnreturnedAndIssues();
        System.out.print("\n  Borrow ID to return: ");
        Integer borrowId = BorrowerService.readInt(sc.nextLine());
        if (borrowId == null) return;

        System.out.print("  Was there damage? [Yes/No]: ");
        String hasDamage = sc.nextLine().trim().equalsIgnoreCase("Yes") ? "Yes" : "No";

        System.out.print("  Condition notes: ");
        String notes = sc.nextLine().trim();

        String damageDescription = null;
        if (hasDamage.equals("Yes")) {
            System.out.print("  Damage description: ");
            damageDescription = sc.nextLine().trim();
        }

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
            System.out.println("  Error: " + AuthService.cleanMessage(e));
        }
    }

    /** CASE [5]: Adds new item via custodian_AddItem. */
    public void addNewItem(Scanner sc) {
        System.out.println("\n  Add New Equipment / Accessory / Peripheral");

        // Barcode Input
        System.out.print("  Barcode: ");
        String barcode = sc.nextLine().trim();
        if (barcode.isEmpty()) {
            System.out.println("  Error: Barcode is required.");
            return;
        }
        if (barcode.length() > 50) {
            System.out.println("  Error: Barcode must not exceed 50 characters.");
            return;
        }

        // Item Name Input
        System.out.print("  Item name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("  Error: Item name is required.");
            return;
        }
        if (name.length() > 100) {
            System.out.println("  Error: Item name must not exceed 100 characters.");
            return;
        }

        // Item Type Input
        System.out.print("  Item type [Equipment/Accessory/Peripheral]: ");
        String type = sc.nextLine().trim();
        if (!type.equalsIgnoreCase("Equipment") &&
                !type.equalsIgnoreCase("Accessory") &&
                !type.equalsIgnoreCase("Peripheral")) {
            System.out.println("  Error: Item type must be Equipment, Accessory, or Peripheral.");
            return;
        }

        // Optional Fields
        System.out.print("  Description (optional): ");
        String desc = sc.nextLine().trim();

        System.out.print("  Model (optional): ");
        String model = sc.nextLine().trim();

        System.out.print("  Tag (optional): ");
        String tag = sc.nextLine().trim();

        // Call Procedure
        String sql = "{CALL custodian_AddItem(?, ?, ?, ?, ?, ?)}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, barcode);
            cs.setString(2, name);
            cs.setString(3, type);
            // Set optional fields to null if they are blank
            cs.setString(4, desc.isBlank() ? null : desc);
            cs.setString(5, model.isBlank() ? null : model);
            cs.setString(6, tag.isBlank() ? null : tag);

            cs.execute();
            System.out.println("  Item added successfully.");

        } catch (Exception e) {
            System.out.println("  Error: " + AuthService.cleanMessage(e));
        }
    }

    /** CASE [6]: Updates condition via custodian_UpdateItemStatus. */
    public void updateItemConditionStatus(Scanner sc) {
        viewEquipmentStatus();
        System.out.print("\n  Item ID to update: ");
        Integer itemId = BorrowerService.readInt(sc.nextLine());
        if (itemId == null) return;

        System.out.print("  New condition [Good/Needs Repair/Damaged/Lost]: ");
        String cond = sc.nextLine().trim();
        System.out.print("  New availability [Available/Unavailable]: ");
        String avail = sc.nextLine().trim();

        String sql = "{CALL custodian_UpdateItemStatus(?, ?, ?)}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, itemId);
            cs.setString(2, cond);
            cs.setString(3, avail);
            cs.execute();
            System.out.println("  Item updated (if not currently borrowed).");
        } catch (Exception e) {
            System.out.println("  Error: " + AuthService.cleanMessage(e));
        }
    }

    /** CASE [7]: View Inventory via custodian_ViewInventory. */
    public void viewEquipmentStatus() {
        String sql = "{CALL custodian_ViewInventory()}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            BorrowerService.printItems(rs, "EQUIPMENT / INVENTORY STATUS");
        } catch (Exception e) {
            System.out.println("  Error: " + AuthService.cleanMessage(e));
        }
    }

    /** CASE [8]: View History via custodian_ViewBorrowHistory. */
    public void viewBorrowRecords() {
        String sql = "{CALL custodian_ViewBorrowHistory()}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            System.out.println("\n  BORROW RECORDS");
            while (rs.next()) {
                System.out.printf("  Borrow #%d | Borrower: %-20s | Status: %s%n",
                        rs.getInt("borrow_id"), rs.getString("borrower_name"), rs.getString("status"));
                System.out.println("    Items: " + BorrowerService.nvl(rs.getString("items")));
            }
        } catch (Exception e) {
            System.out.println("  Error: " + AuthService.cleanMessage(e));
        }
    }

    /** CASE [9]: View Issues via custodian_ViewIssues. */
    public void viewUnreturnedAndIssues() {
        String sql = "{CALL custodian_ViewIssues()}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            System.out.println("\n  UNRETURNED ITEMS / RETURNS WITH ISSUES");
            while (rs.next()) {
                System.out.printf("  Borrow #%d | Borrower: %-20s | Status: %-22s%n",
                        rs.getInt("borrow_id"), rs.getString("borrower_name"), rs.getString("status"));
                System.out.println("    Items: " + BorrowerService.nvl(rs.getString("items")));
            }
        } catch (Exception e) {
            System.out.println("  Error: " + AuthService.cleanMessage(e));
        }
    }

    // Helper validation methods (promptRequired, promptOptional) remain same as original...
}