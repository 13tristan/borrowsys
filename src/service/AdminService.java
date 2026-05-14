package service;

import dao.Database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Scanner;

/**
 * Contains administrator-side features.
 *
 * Every database call is now routed through stored procedures using
 * CallableStatement. Even read-only reports call procedures that return result
 * sets, keeping SQL logic inside the database routines.
 */
public class AdminService {

   public void viewDashboardSummary() {
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL admin_ViewDashboardSummary()}")) {
         try (ResultSet rs = cs.executeQuery()) {
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
            }
         }
      } catch (Exception e) {
         System.out.println("  Unable to load dashboard: " + AuthService.cleanMessage(e));
      }
   }

   public void addCustodian(Scanner sc) {
      System.out.println("\n  Add Custodian Account");
      String firstName = promptRequired(sc, "First name", 50);
      String lastName = promptRequired(sc, "Last name", 50);
      String email = promptRequired(sc, "Email", 100);
      String contact = promptOptional(sc, "Contact number", 20);
      String department = promptOptional(sc, "Department", 100);
      String password = promptRequired(sc, "Password", 255);
      if (firstName == null || lastName == null || email == null || password == null) return;

      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL admin_AddCustodian(?, ?, ?, ?, ?, ?, ?)}")) {
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

      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL admin_SetCustodianStatus(?, ?, ?)}")) {
         cs.setInt(1, adminId);
         cs.setInt(2, custodianId);
         cs.setString(3, status);
         cs.execute();
         System.out.println("  Custodian account status updated.");
      } catch (Exception e) {
         System.out.println("  Status was not updated: " + AuthService.cleanMessage(e));
      }
   }

   public void deleteUserAccount(Scanner sc, int adminId) {
      viewAllUsers();
      System.out.print("  Enter user ID to delete: ");
      Integer targetUserId = BorrowerService.readInt(sc.nextLine());
      if (targetUserId == null) {
         System.out.println("  Invalid user ID.");
         return;
      }

      System.out.println("  This is a permanent delete and is heavily restricted.");
      System.out.println("  Tip: If the user already has records, deactivate the account instead so history remains intact.");
      System.out.print("  Type DELETE to confirm: ");
      if (!sc.nextLine().trim().equals("DELETE")) {
         System.out.println("  Delete cancelled.");
         return;
      }

      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL admin_DeleteUserAccount(?, ?)}")) {
         cs.setInt(1, adminId);
         cs.setInt(2, targetUserId);
         cs.execute();
         System.out.println("  User account deleted successfully.");
      } catch (Exception e) {
         System.out.println("  User account was not deleted: " + AuthService.cleanMessage(e));
         System.out.println("  Tip: Deactivate the account instead when it already has related records or transaction history.");
      }
   }

   public void viewAllUsers() {
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL admin_ViewAllUsers()}")) {
         try (ResultSet rs = cs.executeQuery()) {
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
         }
      } catch (Exception e) {
         System.out.println("  Unable to load users: " + AuthService.cleanMessage(e));
      }
   }

   public void viewInventoryStatus() {
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL admin_ViewInventoryStatus()}")) {
         try (ResultSet rs = cs.executeQuery()) {
            BorrowerService.printItems(rs, "EQUIPMENT / INVENTORY STATUS");
         }
      } catch (Exception e) {
         System.out.println("  Unable to load inventory status: " + AuthService.cleanMessage(e));
      }
   }

   public void viewBorrowRequests() {
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL admin_ViewBorrowRequests()}")) {
         try (ResultSet rs = cs.executeQuery()) {
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
         }
      } catch (Exception e) {
         System.out.println("  Unable to load borrow requests: " + AuthService.cleanMessage(e));
      }
   }

   public void viewBorrowRecords() {
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL admin_ViewBorrowRecords()}")) {
         try (ResultSet rs = cs.executeQuery()) {
            printBorrowRecords(rs);
         }
      } catch (Exception e) {
         System.out.println("  Unable to load borrow records: " + AuthService.cleanMessage(e));
      }
   }

   public void viewReturnRecords() {
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL admin_ViewReturnRecords()}")) {
         try (ResultSet rs = cs.executeQuery()) {
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
         }
      } catch (Exception e) {
         System.out.println("  Unable to load return records: " + AuthService.cleanMessage(e));
      }
   }

   private void viewCustodianAccounts() {
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL admin_ViewCustodianAccounts()}")) {
         try (ResultSet rs = cs.executeQuery()) {
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
         }
      } catch (Exception e) {
         System.out.println("  Unable to load custodians: " + AuthService.cleanMessage(e));
      }
   }

   private void printBorrowRecords(ResultSet rs) throws Exception {
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
