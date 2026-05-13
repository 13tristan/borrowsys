package service;

import dao.Database;
import models.DataClasses;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Contains the borrower-side transaction features.
 *
 * Borrowers can view available items, create requests, cancel pending requests,
 * view their active borrowed items/history, and update their own account details.
 * Quantity is intentionally not used because each physical item is unique through
 * item_id and barcode.
 */
public class BorrowerService {

   /** Shows only items that are valid for requesting. The SQL view already filters unavailable/borrowed items. */
   public void viewAvailableItems() {
      // vw_available_items simplifies the SELECT by hiding the availability rules inside the database view.
      String sql = """
                SELECT item_id, barcode, item_name, item_type, condition_status, availability_status
                FROM vw_available_items
                ORDER BY item_type, item_name
                """;
      try (Connection conn = Database.getConnection();
           PreparedStatement ps = conn.prepareStatement(sql);
           ResultSet rs = ps.executeQuery()) {
         printItems(rs, "AVAILABLE ITEMS");
      } catch (Exception e) {
         System.out.println("  Unable to load available items: " + AuthService.cleanMessage(e));
      }
   }

   /**
    * Creates a new borrow request through a stored procedure.
    * The procedure validates item existence, availability, duplicate pending requests,
    * and the rule that a request must contain at least one item.
    */
   public void createBorrowRequest(DataClasses.User user, Scanner sc) {
      System.out.println("\n  Create Borrow Request");
      viewAvailableItems();

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

      // Multiple unique item IDs may be entered, but no quantity is needed because each item has its own barcode.
      System.out.print("  Enter item ID/s to request (comma-separated, no quantity needed): ");
      String itemIds = normalizeItemIds(sc.nextLine());
      if (itemIds == null) return;

      // Stored procedure inserts into borrow_request and request_item as one controlled transaction.
      String sql = "{CALL borrower_CreateBorrowRequest(?, ?, ?, ?, ?)}";
      try (Connection conn = Database.getConnection();
           // CallableStatement calls the MySQL procedure and receives the new request_id.
           CallableStatement cs = conn.prepareCall(sql)) {
         cs.setInt(1, user.userId);
         cs.setString(2, purpose);
         cs.setString(3, purposeRef.isBlank() ? null : purposeRef);
         cs.setString(4, itemIds);
         // OUT parameter returns the created request ID.
         cs.registerOutParameter(5, Types.INTEGER);
         cs.execute();
         System.out.println("  Borrow request created successfully. Request ID: " + cs.getInt(5));
      } catch (Exception e) {
         System.out.println("  Request was not created: " + AuthService.cleanMessage(e));
      }
   }

   /** Displays the logged-in borrower's request records only. */
   public void viewMyRequests(DataClasses.User user) {
      String sql = """
                SELECT request_id, request_date, purpose, purpose_ref, status,
                       processed_by_name, processed_date, remarks, items
                FROM vw_borrow_requests_detailed
                WHERE borrower_id = ?
                ORDER BY request_date DESC, request_id DESC
                """;
      try (Connection conn = Database.getConnection();
           PreparedStatement ps = conn.prepareStatement(sql)) {
         // Restricts results to the currently logged-in borrower.
         ps.setInt(1, user.userId);
         try (ResultSet rs = ps.executeQuery()) {
            System.out.println("\n  MY BORROW REQUESTS");
            boolean found = false;
            while (rs.next()) {
               found = true;
               System.out.printf("  Request #%d | %s | %s | Ref: %s | Status: %s%n",
                       rs.getInt("request_id"), rs.getString("request_date"), rs.getString("purpose"),
                       nvl(rs.getString("purpose_ref")), rs.getString("status"));
               System.out.println("    Items: " + nvl(rs.getString("items")));
               System.out.println("    Processed by: " + nvl(rs.getString("processed_by_name"))
                       + " | Processed date: " + nvl(rs.getString("processed_date"))
                       + " | Remarks: " + nvl(rs.getString("remarks")));
            }
            if (!found) System.out.println("  No requests found.");
         }
      } catch (Exception e) {
         System.out.println("  Unable to load requests: " + AuthService.cleanMessage(e));
      }
   }

   /** Cancels a request only when it belongs to the borrower and is still Pending. */
   public void cancelRequest(DataClasses.User user, Scanner sc) {
      System.out.print("  Enter pending request ID to cancel: ");
      Integer requestId = readInt(sc.nextLine());
      if (requestId == null) {
         System.out.println("  Invalid request ID.");
         return;
      }
      // Stored procedure enforces ownership and pending-status validation before cancelling.
      String sql = "{CALL borrower_CancelRequest(?, ?)}";
      try (Connection conn = Database.getConnection();
           // CallableStatement calls the MySQL procedure and receives the new request_id.
           CallableStatement cs = conn.prepareCall(sql)) {
         cs.setInt(1, requestId);
         cs.setInt(2, user.userId);
         cs.execute();
         System.out.println("  Request cancelled successfully.");
      } catch (Exception e) {
         System.out.println("  Request was not cancelled: " + AuthService.cleanMessage(e));
      }
   }

   /** Shows borrow records that are still Borrowed or Overdue. */
   public void viewActiveBorrowedItems(DataClasses.User user) {
      String sql = """
                SELECT borrow_id, borrow_date, purpose, status, custodian_name, items, item_count
                FROM vw_borrow_history_details
                WHERE borrower_id = ? AND status IN ('Borrowed', 'Overdue')
                ORDER BY borrow_date DESC
                """;
      printBorrowHistory(user, sql, "ACTIVE BORROWED ITEMS");
   }

   /** Shows the complete borrow history of the logged-in borrower. */
   public void viewBorrowHistory(DataClasses.User user) {
      String sql = """
                SELECT borrow_id, borrow_date, return_date, purpose, status, custodian_name, items, item_count
                FROM vw_borrow_history_details
                WHERE borrower_id = ?
                ORDER BY borrow_date DESC
                """;
      printBorrowHistory(user, sql, "BORROW HISTORY");
   }

   /** Updates editable account fields for the logged-in borrower. */
   public void updateAccountInfo(DataClasses.User user, Scanner sc) {
      System.out.println("\n  Update My Account Information");
      System.out.println("  Leave a field blank to keep the current value.");

      System.out.print("  First name [" + user.firstName + "]: ");
      String firstName = keepOrNew(sc.nextLine(), user.firstName, 50, "First name");
      if (firstName == null) return;

      System.out.print("  Last name [" + user.lastName + "]: ");
      String lastName = keepOrNew(sc.nextLine(), user.lastName, 50, "Last name");
      if (lastName == null) return;

      System.out.print("  Email [" + user.email + "]: ");
      String email = keepOrNew(sc.nextLine(), user.email, 100, "Email");
      if (email == null) return;

      System.out.print("  Contact number [" + nvl(user.contactNumber) + "]: ");
      String contact = keepOrNew(sc.nextLine(), user.contactNumber, 20, "Contact number");
      if (contact == null) return;

      System.out.print("  Department [" + nvl(user.department) + "]: ");
      String department = keepOrNew(sc.nextLine(), user.department, 100, "Department");
      if (department == null) return;

      System.out.print("  New password (blank to keep current): ");
      String password = sc.nextLine().trim();
      if (password.length() > 255) {
         System.out.println("  Password must not exceed 255 characters.");
         return;
      }

      // Procedure updates the user table and can optionally keep the current password.
      String sql = "{CALL borrower_UpdateAccountInfo(?, ?, ?, ?, ?, ?, ?)}";
      try (Connection conn = Database.getConnection();
           // CallableStatement calls the MySQL procedure and receives the new request_id.
           CallableStatement cs = conn.prepareCall(sql)) {
         cs.setInt(1, user.userId);
         cs.setString(2, firstName);
         cs.setString(3, lastName);
         cs.setString(4, email);
         cs.setString(5, contact);
         cs.setString(6, department);
         cs.setString(7, password.isBlank() ? null : password);
         cs.execute();

         user.firstName = firstName;
         user.lastName = lastName;
         user.email = email;
         user.contactNumber = contact;
         user.department = department;
         System.out.println("  Account information updated successfully.");
      } catch (Exception e) {
         System.out.println("  Account was not updated: " + AuthService.cleanMessage(e));
      }
   }

   // Shared display method used by active borrowed items and full history features.
   private void printBorrowHistory(DataClasses.User user, String sql, String heading) {
      try (Connection conn = Database.getConnection();
           PreparedStatement ps = conn.prepareStatement(sql)) {
         // Restricts results to the currently logged-in borrower.
         ps.setInt(1, user.userId);
         try (ResultSet rs = ps.executeQuery()) {
            System.out.println("\n  " + heading);
            boolean found = false;
            while (rs.next()) {
               found = true;
               System.out.printf("  Borrow #%d | %s | Purpose: %s | Status: %s | Items: %d%n",
                       rs.getInt("borrow_id"), rs.getString("borrow_date"), rs.getString("purpose"),
                       rs.getString("status"), rs.getInt("item_count"));
               System.out.println("    Custodian: " + nvl(rs.getString("custodian_name")));
               System.out.println("    Items: " + nvl(rs.getString("items")));
            }
            if (!found) System.out.println("  No records found.");
         }
      } catch (Exception e) {
         System.out.println("  Unable to load borrow records: " + AuthService.cleanMessage(e));
      }
   }

   /**
    * Validates and normalizes comma-separated item IDs.
    * LinkedHashSet removes duplicate IDs while preserving the order entered by the user.
    */
   static String normalizeItemIds(String input) {
      if (input == null || input.trim().isBlank()) {
         System.out.println("  At least one item must be selected.");
         return null;
      }
      String[] parts = input.split(",");
      Set<Integer> ids = new LinkedHashSet<>();
      for (String part : parts) {
         Integer id = readInt(part.trim());
         if (id == null || id <= 0) {
            System.out.println("  Invalid item ID: " + part);
            return null;
         }
         ids.add(id);
      }
      if (ids.isEmpty()) {
         System.out.println("  At least one item must be selected.");
         return null;
      }
      StringBuilder sb = new StringBuilder();
      for (Integer id : ids) {
         if (sb.length() > 0) sb.append(',');
         sb.append(id);
      }
      return sb.toString();
   }

   // Safely converts text input to Integer; returns null instead of crashing on invalid input.
   static Integer readInt(String value) {
      try {
         return Integer.parseInt(value.trim());
      } catch (Exception e) {
         return null;
      }
   }

   // Used by update forms: blank input means keep the current value.
   static String keepOrNew(String input, String current, int maxLength, String label) {
      String value = input.trim().isBlank() ? current : input.trim();
      if (value != null && value.length() > maxLength) {
         System.out.println("  " + label + " must not exceed " + maxLength + " characters.");
         return null;
      }
      return value;
   }

   // Shared item display helper used by Borrower, Custodian, and Admin views.
   static void printItems(ResultSet rs, String heading) throws Exception {
      System.out.println("\n  " + heading);
      boolean found = false;
      while (rs.next()) {
         found = true;
         System.out.printf("  [%d] %-25s | Barcode: %-14s | Type: %-10s | Condition: %-12s | Status: %s%n",
                 rs.getInt("item_id"), rs.getString("item_name"), rs.getString("barcode"),
                 rs.getString("item_type"), rs.getString("condition_status"), rs.getString("availability_status"));
      }
      if (!found) System.out.println("  No items found.");
   }

   // Converts null/blank database values into N/A for cleaner console output.
   static String nvl(String value) {
      return value == null || value.isBlank() ? "N/A" : value;
   }
}
