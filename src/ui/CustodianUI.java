package ui;

import java.sql.*;
import java.util.Scanner;

import dao.Database;
import models.DataClasses;

public class CustodianUI {

    // ─── MENU ────────────────────────────────────────────────────────────────
    public static void menu(DataClasses.User user, Scanner sc) {
        boolean back = false;
        while (!back) {
            System.out.println("\n╔══════════════════════════════════════════════╗");
            System.out.println("║          CUSTODIAN MENU                      ║");
            System.out.println("║  Logged in as: " + padRight(user.getFullName(), 30) + "║");
            System.out.println("╠══════════════════════════════════════════════╣");
            System.out.println("║  [1] View All Items / Equipment Status       ║");
            System.out.println("║  [2] View Laboratory Classes                 ║");
            System.out.println("║  [3] View Borrow Status (by Class or Event)  ║");
            System.out.println("║  [4] View Borrowers with Unreturned Items     ║");
            System.out.println("║  [5] View All Borrow Records                 ║");
            System.out.println("║  [6] View Return Records with Damage         ║");
            System.out.println("║  [0] Logout                                  ║");
            System.out.println("╚══════════════════════════════════════════════╝");
            System.out.print("  Choice: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> viewAllItems();
                case "2" -> viewLaboratoryClasses(sc);
                case "3" -> viewBorrowStatus(sc);
                case "4" -> viewUnreturnedBorrowers();
                case "5" -> viewAllBorrowRecords();
                case "6" -> viewReturnRecordsWithDamage();
                case "0" -> back = true;
                default  -> System.out.println("  Invalid choice.");
            }
        }
    }

    // ─── UI-C1: VIEW ALL ITEMS / EQUIPMENT STATUS ────────────────────────────
    public static void viewAllItems() {
        System.out.println("\n--- EQUIPMENT / ITEM STATUS ---");
        String sql = """
            SELECT item_id, barcode, item_name, item_type, model, tag,
                   condition_status, availability_status, date_acquired
            FROM ITEM
            ORDER BY item_type, item_name
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-5s %-14s %-25s %-11s %-20s %-10s %-18s %-12s%n",
                "ID", "Barcode", "Item Name", "Type", "Model", "Tag",
                "Condition", "Availability");
            printLine();

            int count = 0;
            while (rs.next()) {
                System.out.printf("%-5d %-14s %-25s %-11s %-20s %-10s %-18s %-12s%n",
                    rs.getInt("item_id"),
                    rs.getString("barcode"),
                    rs.getString("item_name"),
                    rs.getString("item_type"),
                    rs.getString("model") == null ? "N/A" : rs.getString("model"),
                    rs.getString("tag")   == null ? "N/A" : rs.getString("tag"),
                    rs.getString("condition_status"),
                    rs.getString("availability_status"));
                count++;
            }
            printLine();
            System.out.println("  Total items: " + count);

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    // ─── UI-C2: VIEW LABORATORY CLASSES ──────────────────────────────────────
    public static void viewLaboratoryClasses(Scanner sc) {
        System.out.println("\n--- LABORATORY CLASSES ---");
        String sql = """
            SELECT lc.class_id, lc.class_code, lc.class_name,
                   CONCAT(u.first_name, ' ', u.last_name) AS instructor,
                   lc.room, lc.schedule_day, lc.schedule_time,
                   lc.semester, lc.academic_year
            FROM LABORATORY_CLASS lc
            JOIN USER u ON lc.instructor_id = u.user_id
            ORDER BY lc.academic_year DESC, lc.semester, lc.class_code
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-5s %-12s %-38s %-22s %-8s %-10s %-8s %-6s %-10s%n",
                "ID", "Code", "Class Name", "Instructor",
                "Room", "Day", "Time", "Sem", "Acad Year");
            printLine();

            int count = 0;
            while (rs.next()) {
                System.out.printf("%-5d %-12s %-38s %-22s %-8s %-10s %-8s %-6s %-10s%n",
                    rs.getInt("class_id"),
                    rs.getString("class_code"),
                    rs.getString("class_name"),
                    rs.getString("instructor"),
                    rs.getString("room"),
                    rs.getString("schedule_day"),
                    rs.getString("schedule_time"),
                    rs.getString("semester"),
                    rs.getString("academic_year"));
                count++;
            }
            printLine();
            System.out.println("  Total classes: " + count);

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }

        // Option to view students in a class
        System.out.print("\n  View students in a class? Enter class ID (or 0 to skip): ");
        String input = sc.nextLine().trim();
        if (!input.equals("0") && input.matches("\\d+")) {
            viewStudentsInClass(Integer.parseInt(input));
        }
    }

    private static void viewStudentsInClass(int classId) {
        String sql = """
            SELECT u.user_id, u.first_name, u.last_name,
                   u.email, cs.enrollment_status
            FROM CLASS_STUDENT cs
            JOIN USER u ON cs.student_id = u.user_id
            WHERE cs.class_id = ?
            ORDER BY u.last_name, u.first_name
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n  Students in Class ID " + classId + ":");
                printLine();
                System.out.printf("  %-5s %-25s %-30s %-12s%n",
                    "ID", "Name", "Email", "Status");
                printLine();
                int count = 0;
                while (rs.next()) {
                    System.out.printf("  %-5d %-25s %-30s %-12s%n",
                        rs.getInt("user_id"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("enrollment_status"));
                    count++;
                }
                printLine();
                System.out.println("  Total students: " + count);
            }
        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    // ─── UI-C3: VIEW BORROW STATUS BY CLASS OR EVENT ─────────────────────────
    public static void viewBorrowStatus(Scanner sc) {
        System.out.println("\n--- VIEW BORROW STATUS ---");
        System.out.println("  Filter by:");
        System.out.println("    [1] Class (purpose = Class)");
        System.out.println("    [2] Event (purpose = Event)");
        System.out.println("    [3] All");
        System.out.print("  Choice: ");
        String choice = sc.nextLine().trim();

        String purposeFilter = switch (choice) {
            case "1" -> "Class";
            case "2" -> "Event";
            default  -> null;
        };

        String sql = """
            SELECT br.borrow_id,
                   CONCAT(u.first_name, ' ', u.last_name) AS borrower,
                   CONCAT(c.first_name, ' ', c.last_name) AS custodian,
                   br.borrow_date, br.return_date,
                   br.purpose, br.status, br.remarks
            FROM BORROW_RECORD br
            JOIN USER u ON br.borrower_id = u.user_id
            JOIN USER c ON br.custodian_id = c.user_id
            """ + (purposeFilter != null ? "WHERE br.purpose = ? " : "") + """
            ORDER BY br.borrow_date DESC
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (purposeFilter != null) ps.setString(1, purposeFilter);

            try (ResultSet rs = ps.executeQuery()) {
                printLine();
                System.out.printf("%-5s %-22s %-22s %-20s %-20s %-8s %-30s%n",
                    "ID", "Borrower", "Custodian",
                    "Borrow Date", "Return Date", "Purpose", "Status");
                printLine();

                int count = 0;
                while (rs.next()) {
                    System.out.printf("%-5d %-22s %-22s %-20s %-20s %-8s %-30s%n",
                        rs.getInt("borrow_id"),
                        rs.getString("borrower"),
                        rs.getString("custodian"),
                        rs.getString("borrow_date"),
                        rs.getString("return_date") == null ? "Not yet returned" : rs.getString("return_date"),
                        rs.getString("purpose"),
                        rs.getString("status"));
                    count++;
                }
                printLine();
                System.out.println("  Total records: " + count);
            }

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }

        // Option to view items per borrow record
        System.out.print("\n  View items for a borrow record? Enter borrow ID (or 0 to skip): ");
        String input = sc.nextLine().trim();
        if (!input.equals("0") && input.matches("\\d+")) {
            viewItemsInBorrowRecord(Integer.parseInt(input));
        }
    }

    private static void viewItemsInBorrowRecord(int borrowId) {
        String sql = """
            SELECT bi.borrow_item_id, i.item_name, i.barcode,
                   i.item_type, bi.quantity, bi.item_condition_on_return
            FROM BORROW_ITEM bi
            JOIN ITEM i ON bi.item_id = i.item_id
            WHERE bi.borrow_id = ?
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, borrowId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n  Items in Borrow Record ID " + borrowId + ":");
                printLine();
                System.out.printf("  %-5s %-25s %-14s %-12s %-5s %-20s%n",
                    "ID", "Item Name", "Barcode", "Type", "Qty", "Return Condition");
                printLine();
                while (rs.next()) {
                    System.out.printf("  %-5d %-25s %-14s %-12s %-5d %-20s%n",
                        rs.getInt("borrow_item_id"),
                        rs.getString("item_name"),
                        rs.getString("barcode"),
                        rs.getString("item_type"),
                        rs.getInt("quantity"),
                        rs.getString("item_condition_on_return") == null
                            ? "Not yet returned" : rs.getString("item_condition_on_return"));
                }
                printLine();
            }
        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    // ─── UI-C4: VIEW BORROWERS WITH UNRETURNED / OVERDUE ITEMS ───────────────
    public static void viewUnreturnedBorrowers() {
        System.out.println("\n--- BORROWERS WITH UNRETURNED / OVERDUE ITEMS ---");
        String sql = """
            SELECT br.borrow_id,
                   CONCAT(u.first_name, ' ', u.last_name) AS borrower,
                   u.email, u.contact_number,
                   br.borrow_date, br.purpose, br.status,
                   COUNT(bi.borrow_item_id) AS item_count
            FROM BORROW_RECORD br
            JOIN USER u ON br.borrower_id = u.user_id
            JOIN BORROW_ITEM bi ON br.borrow_id = bi.borrow_id
            WHERE br.status IN ('Borrowed', 'Overdue')
            GROUP BY br.borrow_id, u.first_name, u.last_name,
                     u.email, u.contact_number,
                     br.borrow_date, br.purpose, br.status
            ORDER BY br.status DESC, br.borrow_date ASC
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-5s %-22s %-30s %-15s %-20s %-8s %-10s %-5s%n",
                "ID", "Borrower", "Email", "Contact",
                "Borrow Date", "Purpose", "Status", "Items");
            printLine();

            int count = 0;
            while (rs.next()) {
                System.out.printf("%-5d %-22s %-30s %-15s %-20s %-8s %-10s %-5d%n",
                    rs.getInt("borrow_id"),
                    rs.getString("borrower"),
                    rs.getString("email"),
                    rs.getString("contact_number") == null ? "N/A" : rs.getString("contact_number"),
                    rs.getString("borrow_date"),
                    rs.getString("purpose"),
                    rs.getString("status"),
                    rs.getInt("item_count"));
                count++;
            }
            printLine();
            System.out.println("  Total unreturned/overdue records: " + count);

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    // ─── UI-C5: VIEW ALL BORROW RECORDS ──────────────────────────────────────
    public static void viewAllBorrowRecords() {
        System.out.println("\n--- ALL BORROW RECORDS ---");
        String sql = """
            SELECT br.borrow_id,
                   CONCAT(u.first_name, ' ', u.last_name)  AS borrower,
                   CONCAT(c.first_name, ' ', c.last_name)  AS custodian,
                   br.borrow_date, br.return_date,
                   br.purpose, br.status, br.remarks
            FROM BORROW_RECORD br
            JOIN USER u ON br.borrower_id  = u.user_id
            JOIN USER c ON br.custodian_id = c.user_id
            ORDER BY br.borrow_date DESC
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-5s %-22s %-22s %-20s %-20s %-8s %-30s%n",
                "ID", "Borrower", "Custodian",
                "Borrow Date", "Return Date", "Purpose", "Status");
            printLine();

            int count = 0;
            while (rs.next()) {
                System.out.printf("%-5d %-22s %-22s %-20s %-20s %-8s %-30s%n",
                    rs.getInt("borrow_id"),
                    rs.getString("borrower"),
                    rs.getString("custodian"),
                    rs.getString("borrow_date"),
                    rs.getString("return_date") == null ? "Not yet returned" : rs.getString("return_date"),
                    rs.getString("purpose"),
                    rs.getString("status"));
                count++;
            }
            printLine();
            System.out.println("  Total records: " + count);

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    // ─── UI-C6: VIEW RETURN RECORDS WITH DAMAGE ───────────────────────────────
    public static void viewReturnRecordsWithDamage() {
        System.out.println("\n--- RETURN RECORDS WITH DAMAGE / ISSUES ---");
        String sql = """
            SELECT rr.return_id, rr.borrow_id,
                   CONCAT(u.first_name, ' ', u.last_name) AS borrower,
                   CONCAT(c.first_name, ' ', c.last_name) AS custodian,
                   rr.actual_return_date, rr.has_damage,
                   rr.condition_notes, rr.damage_description
            FROM RETURN_RECORD rr
            JOIN BORROW_RECORD br ON rr.borrow_id = br.borrow_id
            JOIN USER u ON br.borrower_id  = u.user_id
            JOIN USER c ON rr.custodian_id = c.user_id
            WHERE rr.has_damage = 'Yes'
            ORDER BY rr.actual_return_date DESC
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-8s %-8s %-22s %-22s %-22s %-50s%n",
                "Ret ID", "Borr ID", "Borrower", "Custodian",
                "Return Date", "Damage Description");
            printLine();

            int count = 0;
            while (rs.next()) {
                System.out.printf("%-8d %-8d %-22s %-22s %-22s %-50s%n",
                    rs.getInt("return_id"),
                    rs.getInt("borrow_id"),
                    rs.getString("borrower"),
                    rs.getString("custodian"),
                    rs.getString("actual_return_date"),
                    rs.getString("damage_description") == null
                        ? "N/A" : rs.getString("damage_description"));
                count++;
            }
            printLine();
            System.out.println("  Total damaged returns: " + count);

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