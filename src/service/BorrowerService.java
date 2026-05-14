package service;

import dao.Database;
import models.DataClasses;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Contains borrower-side transaction features.
 *
 * Every database operation in this class uses CallableStatement. The actual SQL
 * SELECT/INSERT/UPDATE logic is stored inside MySQL routines so the Java code
 * acts as the application interface while the database controls the rules.
 */
public class BorrowerService {

   /** Shows only items that are valid for requesting. */
   public void viewAvailableItems() {
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL borrower_ViewAvailableItems()}")) {
         try (ResultSet rs = cs.executeQuery()) {
            ConsoleTable.print(rs, "AVAILABLE ITEMS");
         }
      } catch (Exception e) {
         System.out.println("  Unable to load available items: " + AuthService.cleanMessage(e));
      }
   }

   /**
    * Creates a borrow request. Quantity is not used because every physical item
    * is already represented by one unique item_id and barcode.
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

      System.out.print("  Enter item ID/s to request (comma-separated): ");
      String itemIds = normalizeItemIds(sc.nextLine());
      if (itemIds == null) return;

      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL borrower_CreateBorrowRequest(?, ?, ?, ?, ?)}")) {
         cs.setInt(1, user.userId);
         cs.setString(2, purpose);
         cs.setString(3, purposeRef.isBlank() ? null : purposeRef);
         cs.setString(4, itemIds);
         cs.registerOutParameter(5, Types.INTEGER);
         cs.execute();
         System.out.println("  Borrow request created successfully. Request ID: " + cs.getInt(5));
      } catch (Exception e) {
         System.out.println("  Request was not created: " + AuthService.cleanMessage(e));
      }
   }

   /** Displays the logged-in borrower's request records only. */
   public void viewMyRequests(DataClasses.User user) {
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL borrower_ViewMyRequests(?)}")) {
         cs.setInt(1, user.userId);
         try (ResultSet rs = cs.executeQuery()) {
            ConsoleTable.print(rs, "MY BORROW REQUESTS");
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
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL borrower_CancelRequest(?, ?)}")) {
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
      printBorrowHistory(user, "{CALL borrower_ViewActiveBorrowedItems(?)}", "ACTIVE BORROWED ITEMS");
   }

   /** Shows the complete borrow history of the logged-in borrower. */
   public void viewBorrowHistory(DataClasses.User user) {
      printBorrowHistory(user, "{CALL borrower_ViewBorrowHistory(?)}", "BORROW HISTORY");
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

      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL borrower_UpdateAccountInfo(?, ?, ?, ?, ?, ?, ?)}")) {
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

   private void printBorrowHistory(DataClasses.User user, String procedureCall, String heading) {
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall(procedureCall)) {
         cs.setInt(1, user.userId);
         try (ResultSet rs = cs.executeQuery()) {
            ConsoleTable.print(rs, heading);
         }
      } catch (Exception e) {
         System.out.println("  Unable to load borrow records: " + AuthService.cleanMessage(e));
      }
   }

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
         if (!sb.isEmpty()) sb.append(',');
         sb.append(id);
      }
      return sb.toString();
   }

   static Integer readInt(String value) {
      try {
         return Integer.parseInt(value.trim());
      } catch (Exception e) {
         return null;
      }
   }

   static String keepOrNew(String input, String current, int maxLength, String label) {
      String value = input.trim().isBlank() ? current : input.trim();
      if (value != null && value.length() > maxLength) {
         System.out.println("  " + label + " must not exceed " + maxLength + " characters.");
         return null;
      }
      return value;
   }

   static void printItems(ResultSet rs, String heading) throws Exception {
      ConsoleTable.print(rs, heading);
   }

   static String nvl(String value) {
      return value == null || value.isBlank() ? "N/A" : value;
   }
}
