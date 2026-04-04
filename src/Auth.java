import java.sql.*;
import java.util.Scanner;

import dao.Database;
import models.DataClasses;

public class Auth {


    public static DataClasses.User login(Scanner sc) {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║              LOGIN                   ║");
        System.out.println("╚══════════════════════════════════════╝");

        System.out.print("  Email   : ");
        String email = sc.nextLine().trim();
        System.out.print("  Password: ");
        String password = sc.nextLine().trim();

        String sql = """
            SELECT user_id, first_name, last_name, email,
                   contact_number, user_type, department, account_status
            FROM USER
            WHERE email = ?
              AND password = ?
              AND account_status = 'Active'
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DataClasses.User user = new DataClasses.User();
                    user.userId        = rs.getInt("user_id");
                    user.firstName     = rs.getString("first_name");
                    user.lastName      = rs.getString("last_name");
                    user.email         = rs.getString("email");
                    user.contactNumber = rs.getString("contact_number");
                    user.userType      = rs.getString("user_type");
                    user.department    = rs.getString("department");
                    user.accountStatus = rs.getString("account_status");
                    return user;
                } else {
                    System.out.println("\n  ✘ Invalid email or password, or account is inactive.");
                    return null;
                }
            }

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
            return null;
        }
    }

    // ─── REGISTER ────────────────────────────────────────────────────────────
    public static void register(Scanner sc) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║             REGISTER                 ║");
        System.out.println("╚══════════════════════════════════════╝");

        System.out.print("  First Name      : ");
        String firstName = sc.nextLine().trim();

        System.out.print("  Last Name       : ");
        String lastName = sc.nextLine().trim();

        System.out.print("  Email           : ");
        String email = sc.nextLine().trim();

        System.out.print("  Contact Number  : ");
        String contact = sc.nextLine().trim();

        System.out.print("  Department      : ");
        String dept = sc.nextLine().trim();

        System.out.println("  User Type:");
        System.out.println("    [1] Student");
        System.out.println("    [2] Instructor");
        System.out.println("    [3] Staff");
        System.out.print("  Choice: ");
        String typeChoice = sc.nextLine().trim();
        String userType;
        switch (typeChoice) {
            case "1" -> userType = "Student";
            case "2" -> userType = "Instructor";
            default  -> { System.out.println("  Invalid choice. Registration cancelled."); return; }
        }

        System.out.print("  Password        : ");
        String password = sc.nextLine().trim();
        System.out.print("  Confirm Password: ");
        String confirm = sc.nextLine().trim();

        if (!password.equals(confirm)) {
            System.out.println("  ✘ Passwords do not match. Registration cancelled.");
            return;
        }

        // Check if email already exists
        String checkSql = "SELECT COUNT(*) FROM USER WHERE email = ?";
        String insertSql = """
            INSERT INTO USER (first_name, last_name, email, contact_number,
                              user_type, department, password, account_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'Active')
            """;

        try (Connection conn = Database.getConnection()) {

            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setString(1, email);
                ResultSet rs = check.executeQuery();
                rs.next();
                if (rs.getInt(1) > 0) {
                    System.out.println("  ✘ Email is already registered.");
                    return;
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, firstName);
                ps.setString(2, lastName);
                ps.setString(3, email);
                ps.setString(4, contact.isEmpty() ? null : contact);
                ps.setString(5, userType);
                ps.setString(6, dept.isEmpty() ? null : dept);
                ps.setString(7, password);
                ps.executeUpdate();
                System.out.println("  ✔ Registration successful! You may now log in.");
            }

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }
}