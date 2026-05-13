package service;

import dao.Database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.Scanner;

/**
 * Contains administrator-side features.
 *
 * Admin focuses on account control and monitoring reports. Most destructive
 * operations are restricted through stored procedures so records with history
 * are protected from accidental deletion.
 */
public class AdminService {

   /** Dashboard summary uses a view and Statement because it has no user input. */
   public void viewDashboardSummary() {
      // vw_system_dashboard summarizes counts from several tables for quick admin monitoring.
      String sql = "SELECT * FROM vw_system_dashboard";
      try (Connection conn = Database.getConnection();
           // Statement is used only for fixed SQL without user input.
           Statement st = conn.createStatement();
           ResultSet rs = st.executeQuery(sql)) {
         System.out.println("\n  SYSTEM DASHBOARD SUMMARY");
         if (rs.next()) {
            System.out.println("  Total Users: " + rs.getInt("total_users"));
            System.out.println("  Active Custodians: " + rs.getInt("active_custodians"));
            System.out.println("  Total Items: " + rs.getInt("total_items"));
            System.out.println("  Available Items: " + rs.getInt("available_items"));
            System.out.println("  Borrowed Items: " + rs.getInt("borrowed_items"));
            System.out.println("  Pending Requests: " + rs.getInt("pending_requests"));
            System.out.println("  Active Borrow Records: " + rs.getInt("active_borrow_records"));
            System.out.println("  Return Records with Damage: " + rs.getInt("damaged_returns"));
            System.out.println("  JDBC interface shown here: Statement");
         }
      } catch (Exception e) {
         System.out.println("  Unable to load dashboard: " + AuthService.cleanMessage(e));
      }
   }

   /** Creates a custodian account through a stored procedure. */
   public void addCustodian(Scanner sc) {
      System.out.println("\n  Add Custodian Account");
      String firstName = promptRequired(sc, "First name", 50);
      String lastName = promptRequired(sc, "Last name", 50);
      String email = promptRequired(sc, "Email", 100);
      String contact = promptOptional(sc, "Contact number", 20);
      String department = promptOptional(sc, "Department", 100);
      String password = promptRequired(sc, "Password", 255);
      if (firstName == null || lastName == null || email == null || password == null) return;

      // Procedure centralizes validation such as duplicate email and correct user_type.
      String sql = "{CALL admin_AddCustodian(?, ?, ?, ?, ?, ?, ?)}";
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall(sql)) {
         cs.setString(1, firstName);
         cs.setString(2, lastName);
         cs.setString(3, email);
         cs.setString(4, contact);
         cs.setString(5, department);
         cs.setString(6, password);
         cs.registerOutParameter(7, Types.INTEGER);
         cs.execute();
         System.out.println("  Custodian account created. User ID: " + cs.getInt(7));
      } catch (Exception e) {
         System.out.println("  Custodian was not created: " + AuthService.cleanMessage(e));
      }
   }

   /** Activates/deactivates custodian accounts instead of deleting history. */
   public void setCustodianStatus(Scanner sc, int adminId) {
      viewCustodianAccounts();
      System.out.print("  Enter custodian user ID: ");
      Integer custodianId = BorrowerService.readInt(sc.nextLine());
      if (custodianId == null) {
         System.out.println("  Invalid user ID.");
         return;
      }
      System.out.print("  New status [Active/Inactive]: ");
      String status = sc.nextLine().trim();
      if (!status.equals("Active") && !status.equals("Inactive")) {
         System.out.println("  Status must be Active or Inactive.");
         return;
      }

      // Status change is handled by a procedure to verify admin authority and target user.
      String sql = "{CALL admin_SetCustodianStatus(?, ?, ?)}";
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall(sql)) {
         cs.setInt(1, adminId);
         cs.setInt(2, custodianId);
         cs.setString(3, status);
         cs.execute();
         System.out.println("  Custodian account status updated.");
      } catch (Exception e) {
         System.out.println("  Status was not updated: " + AuthService.cleanMessage(e));
      }
   }


   /**
    * Heavily restricted delete for user accounts.
    * The procedure blocks deletion when the user has any related history,
    * and the UI warns admins to deactivate instead.
    */
   public void deleteUserAccount(Scanner sc, int adminId) {
      viewAllUsers();
      System.out.print("  Enter user ID to delete: ");
      Integer targetUserId = BorrowerService.readInt(sc.nextLine());
      if (targetUserId == null) {
         System.out.println("  Invalid user ID.");
         return;
      }

      System.out.println("  This is a permanent delete and is heavily restricted.");
      System.out.println("  The account can only be deleted if it has no class, request, borrow, return, or approval history.");
      System.out.println("  Tip: If the user already has records, deactivate the account instead so history remains intact.");
      System.out.print("  Type DELETE to confirm: ");
      String confirm = sc.nextLine().trim();
      if (!confirm.equals("DELETE")) {
         System.out.println("  Delete cancelled.");
         return;
      }

      // Stored procedure enforces all delete restrictions at the database level.
      String sql = "{CALL admin_DeleteUserAccount(?, ?)}";
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall(sql)) {
         cs.setInt(1, adminId);
         cs.setInt(2, targetUserId);
         cs.execute();
         System.out.println("  User account deleted successfully.");
      } catch (Exception e) {
         System.out.println("  User account was not deleted: " + AuthService.cleanMessage(e));
         System.out.println("  Tip: Deactivate the account instead when it already has related records or transaction history.");
      }
   }

   /** Displays all users for admin monitoring. */
   public void viewAllUsers() {
      String sql = """
                SELECT user_id, first_name, last_name, email, user_type, department, account_status
                FROM `user`
                ORDER BY user_type, last_name, first_name
                """;
      try (Connection conn = Database.getConnection();
           PreparedStatement ps = conn.prepareStatement(sql);
           ResultSet rs = ps.executeQuery()) {
         System.out.println("\n  ALL USERS");
         boolean found = false;
         while (rs.next()) {
            found = true;
            System.out.printf("  [%d] %-20s | %-10s | %-25s | %-15s | %s%n",
                    rs.getInt("user_id"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("user_type"), rs.getString("email"),
                    BorrowerService.nvl(rs.getString("department")), rs.getString("account_status"));
         }
         if (!found) System.out.println("  No users found.");
      } catch (Exception e) {
         System.out.println("  Unable to load users: " + AuthService.cleanMessage(e));
      }
   }

   /** Displays inventory status using the inventory view. */
   public void viewInventoryStatus() {
      String sql = "SELECT * FROM vw_item_inventory_status ORDER BY item_type, item_name";
      try (Connection conn = Database.getConnection();
           // Statement is used only for fixed SQL without user input.
           Statement st = conn.createStatement();
           ResultSet rs = st.executeQuery(sql)) {
         BorrowerService.printItems(rs, "EQUIPMENT / INVENTORY STATUS");
      } catch (Exception e) {
         System.out.println("  Unable to load inventory status: " + AuthService.cleanMessage(e));
      }
   }

   /** Displays all borrow requests, including requester, processor, status, and items. */
   public void viewBorrowRequests() {
      String sql = """
                SELECT request_id, borrower_name, request_date, purpose, purpose_ref, status,
                       processed_by_name, processed_date, items
                FROM vw_borrow_requests_detailed
                ORDER BY request_date DESC, request_id DESC
                """;
      try (Connection conn = Database.getConnection();
           PreparedStatement ps = conn.prepareStatement(sql);
           ResultSet rs = ps.executeQuery()) {
         System.out.println("\n  BORROW REQUESTS");
         boolean found = false;
         while (rs.next()) {
            found = true;
            System.out.printf("  Request #%d | Borrower: %-20s | %s | %s | Status: %s%n",
                    rs.getInt("request_id"), rs.getString("borrower_name"),
                    rs.getString("request_date"), rs.getString("purpose"), rs.getString("status"));
            System.out.println("    Ref: " + BorrowerService.nvl(rs.getString("purpose_ref")));
            System.out.println("    Items: " + BorrowerService.nvl(rs.getString("items")));
            System.out.println("    Processed by: " + BorrowerService.nvl(rs.getString("processed_by_name"))
                    + " | Date: " + BorrowerService.nvl(rs.getString("processed_date")));
         }
         if (!found) System.out.println("  No borrow requests found.");
      } catch (Exception e) {
         System.out.println("  Unable to load borrow requests: " + AuthService.cleanMessage(e));
      }
   }

   /** Displays all approved/actual borrow records. */
   public void viewBorrowRecords() {
      String sql = """
                SELECT borrow_id, borrower_name, custodian_name, borrow_date, return_date,
                       purpose, status, item_count, items
                FROM vw_borrow_history_details
                ORDER BY borrow_date DESC, borrow_id DESC
                """;
      printBorrowRecords(sql);
   }

   /** Displays return records and damage notes for monitoring issues. */
   public void viewReturnRecords() {
      String sql = """
                SELECT return_id, borrow_id, borrower_name, custodian_name, actual_return_date,
                       has_damage, condition_notes, damage_description, items
                FROM vw_return_records_detailed
                ORDER BY actual_return_date DESC, return_id DESC
                """;
      try (Connection conn = Database.getConnection();
           PreparedStatement ps = conn.prepareStatement(sql);
           ResultSet rs = ps.executeQuery()) {
         System.out.println("\n  RETURN RECORDS / ISSUES");
         boolean found = false;
         while (rs.next()) {
            found = true;
            System.out.printf("  Return #%d | Borrow #%d | Borrower: %-20s | Damage: %s | Date: %s%n",
                    rs.getInt("return_id"), rs.getInt("borrow_id"), rs.getString("borrower_name"),
                    rs.getString("has_damage"), rs.getString("actual_return_date"));
            System.out.println("    Items: " + BorrowerService.nvl(rs.getString("items")));
            System.out.println("    Notes: " + BorrowerService.nvl(rs.getString("condition_notes"))
                    + " | Damage details: " + BorrowerService.nvl(rs.getString("damage_description")));
         }
         if (!found) System.out.println("  No return records found.");
      } catch (Exception e) {
         System.out.println("  Unable to load return records: " + AuthService.cleanMessage(e));
      }
   }

   // Helper used before changing custodian status so the admin can see valid IDs.
   private void viewCustodianAccounts() {
      String sql = """
                SELECT user_id, first_name, last_name, email, department, account_status
                FROM `user`
                WHERE user_type = 'Custodian'
                ORDER BY account_status, last_name, first_name
                """;
      try (Connection conn = Database.getConnection();
           PreparedStatement ps = conn.prepareStatement(sql);
           ResultSet rs = ps.executeQuery()) {
         System.out.println("\n  CUSTODIAN ACCOUNTS");
         boolean found = false;
         while (rs.next()) {
            found = true;
            System.out.printf("  [%d] %-20s | %-25s | %-15s | %s%n",
                    rs.getInt("user_id"), rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("email"), BorrowerService.nvl(rs.getString("department")),
                    rs.getString("account_status"));
         }
         if (!found) System.out.println("  No custodians found.");
      } catch (Exception e) {
         System.out.println("  Unable to load custodians: " + AuthService.cleanMessage(e));
      }
   }

   // Shared print method for borrow record result sets.
   private void printBorrowRecords(String sql) {
      try (Connection conn = Database.getConnection();
           PreparedStatement ps = conn.prepareStatement(sql);
           ResultSet rs = ps.executeQuery()) {
         System.out.println("\n  BORROW RECORDS");
         boolean found = false;
         while (rs.next()) {
            found = true;
            System.out.printf("  Borrow #%d | Borrower: %-20s | Custodian: %-20s | Status: %-22s | Items: %d%n",
                    rs.getInt("borrow_id"), rs.getString("borrower_name"), rs.getString("custodian_name"),
                    rs.getString("status"), rs.getInt("item_count"));
            System.out.println("    Borrowed: " + rs.getString("borrow_date")
                    + " | Returned: " + BorrowerService.nvl(rs.getString("return_date"))
                    + " | Purpose: " + rs.getString("purpose"));
            System.out.println("    Items: " + BorrowerService.nvl(rs.getString("items")));
         }
         if (!found) System.out.println("  No borrow records found.");
      } catch (Exception e) {
         System.out.println("  Unable to load borrow records: " + AuthService.cleanMessage(e));
      }
   }

   // Reusable validation helper for required admin input fields.
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

   // Reusable validation helper for optional admin input fields.
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
