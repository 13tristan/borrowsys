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
 * Every database call is routed through stored procedures using CallableStatement.
 * Read-only reports call procedures that return result sets, keeping SQL logic
 * inside the database routines.
 */
public class AdminService {

   public void viewDashboardSummary() {
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall("{CALL admin_ViewDashboardSummary()}")) {
         try (ResultSet rs = cs.executeQuery()) {
            ConsoleTable.print(rs, "SYSTEM DASHBOARD SUMMARY");
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
      callTable("{CALL admin_ViewAllUsers()}", "ALL USERS", "  Unable to load users: ");
   }

   public void viewInventoryStatus() {
      callTable("{CALL admin_ViewInventoryStatus()}", "EQUIPMENT / INVENTORY STATUS", "  Unable to load inventory status: ");
   }

   public void viewBorrowRequests() {
      callTable("{CALL admin_ViewBorrowRequests()}", "BORROW REQUESTS", "  Unable to load borrow requests: ");
   }

   public void viewBorrowRecords() {
      callTable("{CALL admin_ViewBorrowRecords()}", "BORROW RECORDS", "  Unable to load borrow records: ");
   }

   public void viewReturnRecords() {
      callTable("{CALL admin_ViewReturnRecords()}", "RETURN RECORDS / ISSUES", "  Unable to load return records: ");
   }

   private void viewCustodianAccounts() {
      callTable("{CALL admin_ViewCustodianAccounts()}", "CUSTODIAN ACCOUNTS", "  Unable to load custodians: ");
   }

   private void callTable(String procedureCall, String heading, String errorPrefix) {
      try (Connection conn = Database.getConnection();
           CallableStatement cs = conn.prepareCall(procedureCall)) {
         try (ResultSet rs = cs.executeQuery()) {
            ConsoleTable.print(rs, heading);
         }
      } catch (Exception e) {
         System.out.println(errorPrefix + AuthService.cleanMessage(e));
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
