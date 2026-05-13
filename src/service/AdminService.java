package service;

import dao.Database;
import models.DataClasses;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class AdminService {

   // ─── ADD USER ─────────────────────────────────────────────────
   public void addUser(Scanner sc) {

      System.out.println("\n--- ADD USER ---");

      System.out.print("  First Name: ");
      String firstName = sc.nextLine();

      System.out.print("  Last Name: ");
      String lastName = sc.nextLine();

      System.out.print("  Email: ");
      String email = sc.nextLine();

      System.out.print("  Contact Number: ");
      String contact = sc.nextLine();

      System.out.print("  User Type (Student/Instructor/Staff/Custodian/Admin): ");
      String type = sc.nextLine();

      System.out.print("  Department: ");
      String dept = sc.nextLine();

      System.out.print("  Password: ");
      String password = sc.nextLine();

      String sql = "{CALL admin_AddUser(?, ?, ?, ?, ?, ?, ?)}";

      try (
            Connection conn = Database.getConnection();
            CallableStatement cs = conn.prepareCall(sql)) {

         cs.setString(1, firstName);
         cs.setString(2, lastName);
         cs.setString(3, email);
         cs.setString(4, contact);
         cs.setString(5, type);
         cs.setString(6, dept);
         cs.setString(7, password);

         cs.executeUpdate();

         System.out.println("  [SUCCESS] User added successfully.");

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── VIEW ALL USERS ───────────────────────────────────────────
   public void viewAllUsers() {

      System.out.println("\n--- ALL USERS ---");

      String sql = "{CALL admin_GetAllUsers()}";

      try (
            Connection conn = Database.getConnection();
            CallableStatement cs = conn.prepareCall(sql);
            ResultSet rs = cs.executeQuery()) {

         printLine();

         System.out.printf(
               "  %-5s %-22s %-30s %-15s %-12s %-20s %-10s%n",
               "ID", "Name", "Email",
               "Contact", "Type", "Department", "Status");

         printLine();

         int count = 0;

         while (rs.next()) {

            System.out.printf(
                  "  %-5d %-22s %-30s %-15s %-12s %-20s %-10s%n",
                  rs.getInt("user_id"),
                  rs.getString("first_name") + " " + rs.getString("last_name"),
                  rs.getString("email"),
                  rs.getString("contact_number") == null ? "N/A" : rs.getString("contact_number"),
                  rs.getString("user_type"),
                  rs.getString("department") == null ? "N/A" : rs.getString("department"),
                  rs.getString("account_status"));

            count++;
         }

         printLine();
         System.out.println("  Total users: " + count);

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── UPDATE USER ──────────────────────────────────────────────
   public void updateUser(Scanner sc) {

      System.out.println("\n--- UPDATE USER ---");

      System.out.print("  Enter User ID: ");
      int userId;
      try {
         userId = Integer.parseInt(sc.nextLine().trim());
      } catch (NumberFormatException e) {
         System.out.println("  [ERROR] Invalid ID entered.");
         return;
      }

      DataClasses.User u = getUserById(userId);

      if (u == null) {
         System.out.println("  User not found.");
         return;
      }

      if ("Admin".equalsIgnoreCase(u.userType)) {
         System.out.println("  Access denied: Cannot modify Admin accounts.");
         return;
      }

      System.out.println("\n  Press ENTER to keep current value\n");

      String firstName = prompt("First Name", u.firstName, sc);
      String lastName = prompt("Last Name", u.lastName, sc);
      String email = prompt("Email", u.email, sc);
      String contact = prompt("Contact", u.contactNumber, sc);
      String dept = prompt("Department", u.department, sc);

      String sql = "{CALL admin_UpdateUser(?, ?, ?, ?, ?, ?)}";

      try (
            Connection conn = Database.getConnection();
            CallableStatement cs = conn.prepareCall(sql)) {

         cs.setInt(1, userId);
         cs.setString(2, firstName);
         cs.setString(3, lastName);
         cs.setString(4, email);
         cs.setString(5, contact);
         cs.setString(6, dept);

         int rows = cs.executeUpdate();

         if (rows > 0) {
            System.out.println("  [SUCCESS] User updated successfully.");
         } else {
            System.out.println("  User not found.");
         }

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── DELETE USER ──────────────────────────────────────────────
   public void deleteUser(Scanner sc) {

      System.out.println("\n--- DELETE USER ---");

      System.out.print("  Enter User ID: ");
      int userId;
      try {
         userId = Integer.parseInt(sc.nextLine().trim());
      } catch (NumberFormatException e) {
         System.out.println("  [ERROR] Invalid ID entered.");
         return;
      }

      System.out.print("  Are you sure you want to delete User #" + userId + "? (y/n): ");
      String confirm = sc.nextLine().trim();

      if (!confirm.equalsIgnoreCase("y")) {
         System.out.println("  Cancelled.");
         return;
      }

      String sql = "{CALL admin_DeleteUser(?)}";

      try (
            Connection conn = Database.getConnection();
            CallableStatement cs = conn.prepareCall(sql)) {

         cs.setInt(1, userId);

         int rows = cs.executeUpdate();

         if (rows > 0) {
            System.out.println("  [SUCCESS] User deleted successfully.");
         } else {
            System.out.println("  User not found or cannot delete Admin accounts.");
         }

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── GET USER BY ID (internal helper) ────────────────────────
   public DataClasses.User getUserById(int userId) {

      String sql = "{CALL admin_FindUserById(?)}";

      try (
            Connection conn = Database.getConnection();
            CallableStatement cs = conn.prepareCall(sql)) {

         cs.setInt(1, userId);

         try (ResultSet rs = cs.executeQuery()) {

            if (rs.next()) {

               DataClasses.User user = new DataClasses.User();

               user.userId = rs.getInt("user_id");
               user.firstName = rs.getString("first_name");
               user.lastName = rs.getString("last_name");
               user.email = rs.getString("email");
               user.userType = rs.getString("user_type");
               user.contactNumber = rs.getString("contact_number");
               user.department = rs.getString("department");

               return user;
            }
         }

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }

      return null;
   }

   // ─── HELPERS ──────────────────────────────────────────────────
   private String prompt(String label, String current, Scanner sc) {

      System.out.println("  " + label + " [" + current + "]");
      System.out.print("  New: ");

      String input = sc.nextLine();

      return input.isBlank() ? current : input;
   }

   private void printLine() {
      System.out.println("  " + "─".repeat(120));
   }
}
