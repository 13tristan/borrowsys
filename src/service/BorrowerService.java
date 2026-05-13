package service;

import dao.Database;
import models.DataClasses;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Scanner;

public class BorrowerService {

   // ─── VIEW AVAILABLE ITEMS ─────────────────────────────────────
   public void viewAvailableItems() {

      System.out.println("\n--- AVAILABLE ITEMS (Borrow by Item ID) ---");
      System.out.println("  Note: Each item has a unique ID. Borrow them individually.\n");

      String sql = "{CALL borrower_ViewAvailableItems()}";

      try (
            Connection conn = Database.getConnection();
            CallableStatement cs = conn.prepareCall(sql);
            ResultSet rs = cs.executeQuery()) {

         printLine();

         System.out.printf(
               "  %-6s %-14s %-30s %-15s %-15s %-10s %-15s%n",
               "Item ID", "Barcode", "Item Name",
               "Type", "Model", "Condition", "Status");

         printLine();

         int count = 0;

         while (rs.next()) {

            System.out.printf(
                  "  %-6d %-14s %-30s %-15s %-15s %-10s %-15s%n",
                  rs.getInt("item_id"),
                  rs.getString("barcode"),
                  truncate(rs.getString("item_name"), 30),
                  rs.getString("item_type"),
                  rs.getString("model") == null
                        ? "N/A"
                        : truncate(rs.getString("model"), 15),
                  rs.getString("condition_status"),
                  rs.getString("availability_status"));

            count++;
         }

         printLine();
         System.out.println("  Total available items: " + count);

         if (count == 0) {
            System.out.println("\n  No items available for borrowing.");
         }

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── VIEW BORROW HISTORY ──────────────────────────────────────
   public void viewBorrowHistory(DataClasses.User user) {

      System.out.println("\n--- MY BORROW HISTORY ---");

      String sql = "{CALL borrower_ViewBorrowHistory(?)}";

      try (Connection conn = Database.getConnection();
            CallableStatement cs = conn.prepareCall(sql)) {

         cs.setInt(1, user.userId);

         try (ResultSet rs = cs.executeQuery()) {

            printLine();

            System.out.printf(
                  "%-8s %-12s %-12s %-12s %-12s %-40s%n",
                  "Borr ID", "Borrow Date", "Return Date",
                  "Purpose", "Status", "Items Borrowed");

            printLine();

            int count = 0;

            while (rs.next()) {

               String returnDate = rs.getString("return_date");
               if (returnDate == null)
                  returnDate = "Not returned";

               String items = rs.getString("items");
               if (items == null)
                  items = "No items";

               String itemIds = rs.getString("item_ids");
               if (itemIds != null && !itemIds.isEmpty()) {
                  items += " (IDs: " + itemIds + ")";
               }

               System.out.printf(
                     "%-8d %-12s %-12s %-12s %-12s %-40s%n",
                     rs.getInt("borrow_id"),
                     rs.getString("borrow_date"),
                     returnDate,
                     truncate(rs.getString("purpose"), 12),
                     rs.getString("status"),
                     items.length() > 40
                           ? items.substring(0, 37) + "..."
                           : items);

               count++;
            }

            printLine();

            if (count == 0) {
               System.out.println("\n  You have no borrow history yet.");
            } else {
               System.out.println("  Total records: " + count);
            }
         }

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── VIEW PENDING REQUESTS ────────────────────────────────────
   public void viewPendingRequests(DataClasses.User user) {

      System.out.println("\n--- MY PENDING BORROW REQUESTS ---");

      String sql = "{CALL borrower_ViewPendingRequests(?)}";

      try (Connection conn = Database.getConnection();
            CallableStatement cs = conn.prepareCall(sql)) {

         cs.setInt(1, user.userId);

         try (ResultSet rs = cs.executeQuery()) {

            printLine();

            System.out.printf(
                  "%-8s %-12s %-12s %-20s %-50s%n",
                  "Req ID", "Request Date",
                  "Purpose", "Status", "Items Requested");

            printLine();

            int count = 0;

            while (rs.next()) {

               String items = rs.getString("items");
               if (items == null)
                  items = "No items";

               System.out.printf(
                     "%-8d %-12s %-12s %-20s %-50s%n",
                     rs.getInt("request_id"),
                     rs.getString("request_date"),
                     truncate(rs.getString("purpose"), 12),
                     rs.getString("status"),
                     items.length() > 50
                           ? items.substring(0, 47) + "..."
                           : items);

               count++;
            }

            printLine();

            if (count == 0) {
               System.out.println("\n  You have no pending requests.");
            } else {
               System.out.println("  Total pending requests: " + count);
            }
         }

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── CREATE BORROW REQUEST ────────────────────────────────────
   public void createBorrowRequest(DataClasses.User user, Scanner sc) {

      System.out.println("\n--- CREATE BORROW REQUEST ---");

      viewAvailableItems();

      System.out.print("\n  Enter Item IDs to borrow (comma-separated, e.g. 1,3,5): ");
      String itemIds = sc.nextLine().trim();

      if (itemIds.isEmpty()) {
         System.out.println("  [ERROR] No items entered. Request cancelled.");
         return;
      }

      System.out.print("  Enter quantities for each item (same order, e.g. 1,1,2): ");
      String quantities = sc.nextLine().trim();

      if (quantities.isEmpty()) {
         System.out.println("  [ERROR] No quantities entered. Request cancelled.");
         return;
      }

      System.out.print("  Purpose (Class / Event / Other): ");
      String purpose = sc.nextLine().trim();

      System.out.print("  Purpose reference/details (press Enter to skip): ");
      String purposeRef = sc.nextLine().trim();
      if (purposeRef.isEmpty())
         purposeRef = null;

      String sql = "{CALL borrower_CreateBorrowRequest(?, ?, ?, ?, ?, ?)}";

      try (Connection conn = Database.getConnection();
            CallableStatement cs = conn.prepareCall(sql)) {

         cs.setInt(1, user.userId);
         cs.setString(2, purpose);
         cs.setString(3, purposeRef);
         cs.setString(4, itemIds);
         cs.setString(5, quantities);
         cs.registerOutParameter(6, Types.INTEGER);

         cs.execute();

         int newRequestId = cs.getInt(6);
         System.out.println("\n  [SUCCESS] Borrow request submitted! Request ID: " + newRequestId);
         System.out.println("  Status: Pending — awaiting custodian approval.");

      } catch (SQLException e) {
         System.out.println("  [ERROR] " + e.getMessage());
      }
   }

   // ─── UPDATE BORROWED ITEM CONDITION ───────────────────────────
   public void updateBorrowedItem(Scanner sc) {

      System.out.println("\n--- UPDATE BORROWED ITEM CONDITION ---");

      System.out.print("  Enter Borrow Item ID to update: ");
      int borrowItemId;
      try {
         borrowItemId = Integer.parseInt(sc.nextLine().trim());
      } catch (NumberFormatException e) {
         System.out.println("  [ERROR] Invalid ID entered.");
         return;
      }

      System.out.println("  Condition options: Good | Damaged | Lost");
      System.out.print("  Enter new condition: ");
      String condition = sc.nextLine().trim();

      if (!condition.equals("Good") && !condition.equals("Damaged") && !condition.equals("Lost")) {
         System.out.println("  [ERROR] Invalid condition. Must be Good, Damaged, or Lost.");
         return;
      }

      String sql = "{CALL borrower_UpdateBorrowedItem(?, ?)}";

      try (Connection conn = Database.getConnection();
            CallableStatement cs = conn.prepareCall(sql)) {

         cs.setInt(1, borrowItemId);
         cs.setString(2, condition);

         cs.execute();

         System.out.println("  [SUCCESS] Item condition updated to: " + condition);

      } catch (SQLException e) {
         System.out.println("  [ERROR] " + e.getMessage());
      }
   }

   // ─── UPDATE ACCOUNT INFORMATION ───────────────────────────────
   public void updateAccountInfo(DataClasses.User user, Scanner sc) {

      System.out.println("\n--- UPDATE ACCOUNT INFORMATION ---");
      System.out.println("  Current info — Name: " + user.firstName + " " + user.lastName
            + " | Email: " + user.email);
      System.out.println("  (Press Enter to keep current value)\n");

      System.out.print("  First name [" + user.firstName + "]: ");
      String firstName = sc.nextLine().trim();
      if (firstName.isEmpty())
         firstName = user.firstName;

      System.out.print("  Last name [" + user.lastName + "]: ");
      String lastName = sc.nextLine().trim();
      if (lastName.isEmpty())
         lastName = user.lastName;

      System.out.print("  Email [" + user.email + "]: ");
      String email = sc.nextLine().trim();
      if (email.isEmpty())
         email = user.email;

      System.out.print("  Contact number [" + user.contactNumber + "]: ");
      String contactNumber = sc.nextLine().trim();
      if (contactNumber.isEmpty())
         contactNumber = user.contactNumber;

      System.out.print("  Department [" + user.department + "]: ");
      String department = sc.nextLine().trim();
      if (department.isEmpty())
         department = user.department;

      System.out.print("  New password (press Enter to keep current): ");
      String newPassword = sc.nextLine().trim();
      String passwordParam = newPassword.isEmpty() ? null : newPassword;

      String sql = "{CALL borrower_UpdateAccountInfo(?, ?, ?, ?, ?, ?, ?)}";

      try (Connection conn = Database.getConnection();
            CallableStatement cs = conn.prepareCall(sql)) {

         cs.setInt(1, user.userId);
         cs.setString(2, firstName);
         cs.setString(3, lastName);
         cs.setString(4, email);
         cs.setString(5, contactNumber);
         cs.setString(6, department);
         if (passwordParam == null) {
            cs.setNull(7, Types.VARCHAR);
         } else {
            cs.setString(7, passwordParam);
         }

         cs.execute();

         // Update the in-memory user object to reflect changes
         user.firstName = firstName;
         user.lastName = lastName;
         user.email = email;
         user.contactNumber = contactNumber;
         user.department = department;

         System.out.println("  [SUCCESS] Account information updated successfully.");

      } catch (SQLException e) {
         System.out.println("  [ERROR] " + e.getMessage());
      }
   }

   // ─── CANCEL REQUEST ───────────────────────────────────────────
   public void cancelRequest(DataClasses.User user, Scanner sc) {

      System.out.println("\n--- CANCEL BORROW REQUEST ---");

      viewPendingRequests(user);

      System.out.print("\n  Enter Request ID to cancel (0 to go back): ");
      int requestId;
      try {
         requestId = Integer.parseInt(sc.nextLine().trim());
      } catch (NumberFormatException e) {
         System.out.println("  [ERROR] Invalid ID entered.");
         return;
      }

      if (requestId == 0)
         return;

      System.out.print("  Confirm cancellation of Request #" + requestId + "? (y/n): ");
      String confirm = sc.nextLine().trim();
      if (!confirm.equalsIgnoreCase("y")) {
         System.out.println("  Cancellation aborted.");
         return;
      }

      String sql = "{CALL borrower_CancelRequest(?, ?)}";

      try (Connection conn = Database.getConnection();
            CallableStatement cs = conn.prepareCall(sql)) {

         cs.setInt(1, requestId);
         cs.setInt(2, user.userId);

         cs.execute();

         System.out.println("  [SUCCESS] Request #" + requestId + " has been cancelled.");

      } catch (SQLException e) {
         System.out.println("  [ERROR] " + e.getMessage());
      }
   }

   // ─── HELPERS ──────────────────────────────────────────────────
   private void printLine() {
      System.out.println("  " + "─".repeat(140));
   }

   private String truncate(String s, int maxLen) {

      if (s == null) {
         return "N/A";
      }

      if (s.length() <= maxLen) {
         return s;
      }

      return s.substring(0, maxLen - 3) + "...";
   }
}