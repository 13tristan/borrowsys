package service;

import dao.Database;
import models.DataClasses;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Scanner;

/**
 * Handles authentication-related features such as login and borrower registration.
 *
 * This class demonstrates both PreparedStatement and CallableStatement:
 * - PreparedStatement is used for login because it safely accepts email/password inputs.
 * - CallableStatement is used for registration because registration is handled by a stored procedure.
 */
public class AuthService {

    /**
     * Checks whether the entered email/password belongs to an active user.
     * Returns a populated User object when login succeeds; otherwise returns null.
     */
    public static DataClasses.User login(Scanner sc) {
        System.out.print("  Email: ");
        String email = sc.nextLine().trim();
        System.out.print("  Password: ");
        String password = sc.nextLine().trim();

        if (email.isBlank() || password.isBlank()) {
            System.out.println("  Email and password are required.");
            return null;
        }

        // Parameter placeholders (?) prevent SQL injection and keep user inputs separate from SQL code.
        String sql = """
                SELECT user_id, first_name, last_name, email, contact_number,
                       user_type, department, account_status
                FROM `user`
                WHERE email = ? AND password = ? AND account_status = 'Active'
                """;

        try (Connection conn = Database.getConnection();
             // PreparedStatement is used because the query needs user-provided email and password.
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Bind the actual input values to the placeholders in the WHERE clause.
            ps.setString(1, email);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Transfer the result set values into a User object for use by menus/services.
                    DataClasses.User user = new DataClasses.User();
                    user.userId = rs.getInt("user_id");
                    user.firstName = rs.getString("first_name");
                    user.lastName = rs.getString("last_name");
                    user.email = rs.getString("email");
                    user.contactNumber = rs.getString("contact_number");
                    user.userType = rs.getString("user_type");
                    user.department = rs.getString("department");
                    user.accountStatus = rs.getString("account_status");
                    System.out.println("  Login successful.");
                    return user;
                }
            }
            System.out.println("  Invalid credentials or inactive account.");
        } catch (Exception e) {
            System.out.println("  Login failed: " + cleanMessage(e));
        }
        return null;
    }

    /**
     * Registers only borrower-type accounts: Student, Instructor, or Staff.
     * Custodian accounts are created by Admin to keep account control restricted.
     */
    public static void register(Scanner sc) {
        System.out.println("\n  Borrower Registration");
        String firstName = promptRequired(sc, "First name", 50);
        String lastName = promptRequired(sc, "Last name", 50);
        String email = promptRequired(sc, "Email", 100);
        String contact = promptOptional(sc, "Contact number", 20);
        String department = promptOptional(sc, "Department", 100);
        String password = promptRequired(sc, "Password", 255);

        if (firstName == null || lastName == null || email == null || password == null) return;

        System.out.println("  User type: [1] Student  [2] Instructor  [3] Staff");
        System.out.print("  Choice: ");
        String typeChoice = sc.nextLine().trim();
        String userType = switch (typeChoice) {
            case "1" -> "Student";
            case "2" -> "Instructor";
            case "3" -> "Staff";
            default -> null;
        };

        if (userType == null) {
            System.out.println("  Invalid user type.");
            return;
        }

        // Stored procedure contains database-side checks such as duplicate email validation.
        String sql = "{CALL auth_RegisterBorrower(?, ?, ?, ?, ?, ?, ?, ?)}";
        try (Connection conn = Database.getConnection();
             // CallableStatement is required when Java calls a MySQL stored procedure.
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, firstName);
            cs.setString(2, lastName);
            cs.setString(3, email);
            cs.setString(4, contact);
            cs.setString(5, userType);
            cs.setString(6, department);
            cs.setString(7, password);
            // OUT parameter returns the newly created user_id from the stored procedure.
            cs.registerOutParameter(8, Types.INTEGER);
            cs.execute();

            System.out.println("  Registration successful. New user ID: " + cs.getInt(8));
        } catch (Exception e) {
            System.out.println("  Registration failed: " + cleanMessage(e));
        }
    }

    // Reusable input helper for required fields with length validation.
    private static String promptRequired(Scanner sc, String label, int maxLength) {
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

    // Reusable input helper for optional fields with length validation.
    private static String promptOptional(Scanner sc, String label, int maxLength) {
        System.out.print("  " + label + ": ");
        String value = sc.nextLine().trim();
        if (value.length() > maxLength) {
            System.out.println("  " + label + " must not exceed " + maxLength + " characters. It will be left blank.");
            return "";
        }
        return value;
    }

    /**
     * Cleans MySQL SIGNAL messages so users see the meaningful part only.
     */
    static String cleanMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return "Unknown error";
        int marker = msg.indexOf("MESSAGE_TEXT = ");
        return marker >= 0 ? msg.substring(marker + 15).trim() : msg;
    }
}
