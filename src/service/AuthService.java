package service;

import dao.Database;
import models.DataClasses;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Scanner;

/**
 * Handles authentication-related features.
 *
 * In this final revision, every database access in the Java program is routed
 * through stored procedures or stored routines using CallableStatement. This
 * matches the requirement that the application should call routines instead of
 * writing direct SELECT/INSERT/UPDATE/DELETE statements inside the services.
 */
public class AuthService {

    /**
     * Calls auth_LoginUser to validate credentials and return the active user row.
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

        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL auth_LoginUser(?, ?)}")) {
            cs.setString(1, email);
            cs.setString(2, password);

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
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
        String userType = switch (sc.nextLine().trim()) {
            case "1" -> "Student";
            case "2" -> "Instructor";
            case "3" -> "Staff";
            default -> null;
        };

        if (userType == null) {
            System.out.println("  Invalid user type.");
            return;
        }

        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL auth_RegisterBorrower(?, ?, ?, ?, ?, ?, ?, ?)}")) {
            cs.setString(1, firstName);
            cs.setString(2, lastName);
            cs.setString(3, email);
            cs.setString(4, contact);
            cs.setString(5, userType);
            cs.setString(6, department);
            cs.setString(7, password);
            cs.registerOutParameter(8, Types.INTEGER);
            cs.execute();

            System.out.println("  Registration successful. New user ID: " + cs.getInt(8));
        } catch (Exception e) {
            System.out.println("  Registration failed: " + cleanMessage(e));
        }
    }

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
