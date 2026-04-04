package ui;
import java.sql.*;
import java.util.Scanner;

import dao.Database;
import models.DataClasses;

public class AdminUI {

    // ─── MENU ────────────────────────────────────────────────────────────────
    public static void menu(DataClasses.User user, Scanner sc) {
        boolean back = false;
        while (!back) {
            System.out.println("\n╔══════════════════════════════════════════════╗");
            System.out.println("║          ADMIN MENU                          ║");
            System.out.println("║  Logged in as: " + padRight(user.getFullName(), 30) + "║");
            System.out.println("╠══════════════════════════════════════════════╣");
            System.out.println("║  [1] View All User Accounts                  ║");
            System.out.println("║  [2] View Custodian Accounts                 ║");
            System.out.println("║  [3] View Equipment Status                   ║");
            System.out.println("║  [4] View All Borrow Records                 ║");
            System.out.println("║  [5] View All Activities / Requests          ║");
            System.out.println("║  [6] View Return Records                     ║");
            System.out.println("║  [0] Logout                                  ║");
            System.out.println("╚══════════════════════════════════════════════╝");
            System.out.print("  Choice: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> viewAllUsers();
                case "2" -> viewCustodianAccounts();
                case "3" -> viewEquipmentStatus();
                case "4" -> viewAllBorrowRecords();
                case "5" -> viewActivitiesAndRequests();
                case "6" -> viewAllReturnRecords();
                case "0" -> back = true;
                default  -> System.out.println("  Invalid choice.");
            }
        }
    }

    // ─── UI-A1: VIEW ALL USER ACCOUNTS ───────────────────────────────────────
    public static void viewAllUsers() {
        System.out.println("\n--- ALL USER ACCOUNTS ---");
        String sql = """
            SELECT user_id, first_name, last_name, email,
                   contact_number, user_type, department, account_status
            FROM USER
            ORDER BY user_type, last_name, first_name
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-5s %-22s %-30s %-15s %-12s %-20s %-10s%n",
                "ID", "Name", "Email", "Contact",
                "Type", "Department", "Status");
            printLine();

            int count = 0;
            while (rs.next()) {
                System.out.printf("%-5d %-22s %-30s %-15s %-12s %-20s %-10s%n",
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

    // ─── UI-A2: VIEW CUSTODIAN ACCOUNTS ──────────────────────────────────────
    public static void viewCustodianAccounts() {
        System.out.println("\n--- CUSTODIAN ACCOUNTS ---");
        String sql = """
            SELECT user_id, first_name, last_name, email,
                   contact_number, department, account_status
            FROM USER
            WHERE user_type = 'Custodian'
            ORDER BY last_name, first_name
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-5s %-22s %-30s %-15s %-20s %-10s%n",
                "ID", "Name", "Email", "Contact", "Department", "Status");
            printLine();

            int count = 0;
            while (rs.next()) {
                System.out.printf("%-5d %-22s %-30s %-15s %-20s %-10s%n",
                    rs.getInt("user_id"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("contact_number") == null ? "N/A" : rs.getString("contact_number"),
                    rs.getString("department") == null ? "N/A" : rs.getString("department"),
                    rs.getString("account_status"));
                count++;
            }
            printLine();
            System.out.println("  Total custodians: " + count);

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    // ─── UI-A3: VIEW EQUIPMENT STATUS ────────────────────────────────────────
    public static void viewEquipmentStatus() {
        System.out.println("\n--- EQUIPMENT STATUS SUMMARY ---");

        // Summary counts
        String summarySql = """
            SELECT condition_status, availability_status, COUNT(*) AS total
            FROM ITEM
            GROUP BY condition_status, availability_status
            ORDER BY condition_status, availability_status
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(summarySql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n  Summary:");
            printLine();
            System.out.printf("  %-20s %-15s %-10s%n",
                "Condition", "Availability", "Count");
            printLine();
            while (rs.next()) {
                System.out.printf("  %-20s %-15s %-10d%n",
                    rs.getString("condition_status"),
                    rs.getString("availability_status"),
                    rs.getInt("total"));
            }
            printLine();
        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }

        // Full list
        System.out.println("\n  Full Item List:");
        String fullSql = """
            SELECT item_id, barcode, item_name, item_type, model,
                   condition_status, availability_status, date_acquired
            FROM ITEM
            ORDER BY item_type, condition_status, item_name
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(fullSql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("  %-5s %-14s %-25s %-11s %-20s %-18s %-12s %-12s%n",
                "ID", "Barcode", "Item Name", "Type", "Model",
                "Condition", "Availability", "Date Acquired");
            printLine();

            int count = 0;
            while (rs.next()) {
                System.out.printf("  %-5d %-14s %-25s %-11s %-20s %-18s %-12s %-12s%n",
                    rs.getInt("item_id"),
                    rs.getString("barcode"),
                    rs.getString("item_name"),
                    rs.getString("item_type"),
                    rs.getString("model") == null ? "N/A" : rs.getString("model"),
                    rs.getString("condition_status"),
                    rs.getString("availability_status"),
                    rs.getString("date_acquired") == null ? "N/A" : rs.getString("date_acquired"));
                count++;
            }
            printLine();
            System.out.println("  Total items: " + count);

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    // ─── UI-A4: VIEW ALL BORROW RECORDS ──────────────────────────────────────
    public static void viewAllBorrowRecords() {
        System.out.println("\n--- ALL BORROW RECORDS ---");
        String sql = """
            SELECT br.borrow_id,
                   CONCAT(u.first_name, ' ', u.last_name)  AS borrower,
                   u.user_type                              AS borrower_type,
                   CONCAT(c.first_name, ' ', c.last_name)  AS custodian,
                   br.borrow_date, br.return_date,
                   br.purpose, br.status
            FROM BORROW_RECORD br
            JOIN USER u ON br.borrower_id  = u.user_id
            JOIN USER c ON br.custodian_id = c.user_id
            ORDER BY br.borrow_date DESC
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-5s %-22s %-12s %-22s %-20s %-20s %-8s %-30s%n",
                "ID", "Borrower", "Type", "Custodian",
                "Borrow Date", "Return Date", "Purpose", "Status");
            printLine();

            int count = 0;
            while (rs.next()) {
                System.out.printf("%-5d %-22s %-12s %-22s %-20s %-20s %-8s %-30s%n",
                    rs.getInt("borrow_id"),
                    rs.getString("borrower"),
                    rs.getString("borrower_type"),
                    rs.getString("custodian"),
                    rs.getString("borrow_date"),
                    rs.getString("return_date") == null ? "Not yet returned" : rs.getString("return_date"),
                    rs.getString("purpose"),
                    rs.getString("status"));
                count++;
            }
            printLine();
            System.out.println("  Total borrow records: " + count);

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    // ─── UI-A5: VIEW ACTIVITIES AND REQUESTS ─────────────────────────────────
    public static void viewActivitiesAndRequests() {
        System.out.println("\n--- ACTIVITIES & REQUESTS ---");
        String sql = """
            SELECT a.activity_id, a.activity_name, a.event_type,
                   a.event_date, a.event_time, a.location,
                   a.approval_status AS act_status,
                   CONCAT(u.first_name, ' ', u.last_name)  AS requester,
                   CONCAT(ap.first_name,' ', ap.last_name) AS approved_by,
                   ar.request_id, ar.request_date,
                   ar.status AS req_status, ar.remarks
            FROM ACTIVITY a
            JOIN USER u ON a.requester_id = u.user_id
            LEFT JOIN USER ap ON a.approved_by = ap.user_id
            LEFT JOIN ACTIVITY_REQUEST ar ON a.activity_id = ar.activity_id
            ORDER BY a.event_date DESC
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-5s %-28s %-14s %-12s %-22s %-12s %-12s %-12s%n",
                "ID", "Activity Name", "Event Type", "Event Date",
                "Requester", "Act Status", "Req Status", "Approved By");
            printLine();

            int count = 0;
            while (rs.next()) {
                System.out.printf("%-5d %-28s %-14s %-12s %-22s %-12s %-12s %-12s%n",
                    rs.getInt("activity_id"),
                    rs.getString("activity_name"),
                    rs.getString("event_type"),
                    rs.getString("event_date"),
                    rs.getString("requester"),
                    rs.getString("act_status"),
                    rs.getString("req_status") == null ? "N/A" : rs.getString("req_status"),
                    rs.getString("approved_by") == null ? "Pending" : rs.getString("approved_by"));
                count++;
            }
            printLine();
            System.out.println("  Total activities: " + count);

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    // ─── UI-A6: VIEW ALL RETURN RECORDS ──────────────────────────────────────
    public static void viewAllReturnRecords() {
        System.out.println("\n--- ALL RETURN RECORDS ---");
        String sql = """
            SELECT rr.return_id, rr.borrow_id,
                   CONCAT(u.first_name, ' ', u.last_name)  AS borrower,
                   CONCAT(c.first_name, ' ', c.last_name)  AS custodian,
                   rr.actual_return_date, rr.has_damage,
                   rr.condition_notes, rr.damage_description
            FROM RETURN_RECORD rr
            JOIN BORROW_RECORD br ON rr.borrow_id    = br.borrow_id
            JOIN USER u           ON br.borrower_id  = u.user_id
            JOIN USER c           ON rr.custodian_id = c.user_id
            ORDER BY rr.actual_return_date DESC
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-8s %-8s %-22s %-22s %-22s %-8s %-40s%n",
                "Ret ID", "Borr ID", "Borrower", "Custodian",
                "Return Date", "Damaged", "Notes / Damage Description");
            printLine();

            int count = 0;
            while (rs.next()) {
                String notes = rs.getString("damage_description") != null
                    ? rs.getString("damage_description")
                    : (rs.getString("condition_notes") != null
                        ? rs.getString("condition_notes") : "None");

                System.out.printf("%-8d %-8d %-22s %-22s %-22s %-8s %-40s%n",
                    rs.getInt("return_id"),
                    rs.getInt("borrow_id"),
                    rs.getString("borrower"),
                    rs.getString("custodian"),
                    rs.getString("actual_return_date"),
                    rs.getString("has_damage"),
                    notes);
                count++;
            }
            printLine();
            System.out.println("  Total return records: " + count);

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────
    private static void printLine() {
        System.out.println("  " + "─".repeat(140));
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
