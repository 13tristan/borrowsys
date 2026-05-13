package service;

import dao.Database;

import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Scanner;

public class CustodianService {

    public static void extractStudents() {
        System.out.println("\n--- CURRENTLY ENROLLED STUDENTS ---");
        String sql = """
            SELECT user_id, first_name, last_name, email, contact_number, department
            FROM USER
            WHERE user_type = 'Student' AND account_status = 'Active'
            ORDER BY last_name, first_name
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-6s %-25s %-35s %-15s %-20s%n",
                    "ID", "Name", "Email", "Contact", "Department");
            printLine();

            int count = 0;
            while (rs.next()) {
                System.out.printf("%-6d %-25s %-35s %-15s %-20s%n",
                        rs.getInt("user_id"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("contact_number") == null ? "N/A" : rs.getString("contact_number"),
                        rs.getString("department") == null ? "N/A" : rs.getString("department"));
                count++;
            }
            printLine();
            System.out.println("  Total enrolled students: " + count);

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void viewAllStaffFaculty() {
        System.out.println("\n--- ALL CIS STAFF AND FACULTY ---");
        String sql = """
            SELECT user_id, first_name, last_name, email, contact_number, user_type, department
            FROM USER
            WHERE user_type IN ('Staff', 'Instructor') AND department = 'CIS' AND account_status = 'Active'
            ORDER BY user_type, last_name, first_name
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-6s %-25s %-35s %-15s %-12s %-20s%n",
                    "ID", "Name", "Email", "Contact", "Type", "Department");
            printLine();

            int count = 0;
            while (rs.next()) {
                System.out.printf("%-6d %-25s %-35s %-15s %-12s %-20s%n",
                        rs.getInt("user_id"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("contact_number") == null ? "N/A" : rs.getString("contact_number"),
                        rs.getString("user_type"),
                        rs.getString("department") == null ? "CIS" : rs.getString("department"));
                count++;
            }
            printLine();
            System.out.println("  Total CIS Staff & Faculty: " + count);

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void extractLabClasses(Scanner sc) {
        System.out.println("\n--- LABORATORY CLASSES (CIS) ---");
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
            System.out.printf("%-6s %-12s %-35s %-22s %-8s %-10s %-8s %-6s %-10s%n",
                    "ID", "Code", "Class Name", "Instructor",
                    "Room", "Day", "Time", "Sem", "Acad Year");
            printLine();

            while (rs.next()) {
                System.out.printf("%-6d %-12s %-35s %-22s %-8s %-10s %-8s %-6s %-10s%n",
                        rs.getInt("class_id"),
                        rs.getString("class_code"),
                        truncate(rs.getString("class_name"), 35),
                        truncate(rs.getString("instructor"), 22),
                        rs.getString("room"),
                        rs.getString("schedule_day"),
                        rs.getString("schedule_time"),
                        rs.getString("semester"),
                        rs.getString("academic_year"));
            }
            printLine();

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }

        System.out.print("\n  View students in a class? Enter class ID (or 0 to skip): ");
        String input = sc.nextLine().trim();
        if (input.equals("0")) {
            return;
        }

        if (!input.matches("\\d+")) {
            System.out.println("  [INPUT ERROR] Please enter a valid Class ID.");
            return;
        }

        int classId = Integer.parseInt(input);
        viewStudentsInClass(classId);
    }

    private static void viewStudentsInClass(int classId) {
        String sql = """
               SELECT u.user_id, u.first_name, u.last_name, u.email, cs.enrollment_status
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
                System.out.printf("  %-6s %-30s %-35s %-12s%n",
                        "ID", "Name", "Email", "Status");
                printLine();
                int count = 0;
                while (rs.next()) {
                    System.out.printf("  %-6d %-30s %-35s %-12s%n",
                            rs.getInt("user_id"),
                            rs.getString("first_name") + " " + rs.getString("last_name"),
                            rs.getString("email"),
                            rs.getString("enrollment_status") == null ? "N/A" : rs.getString("enrollment_status"));
                    count++;
                }
                printLine();
                if (count == 0) {
                    System.out.println("  No students found for Class ID " + classId + ".");
                }
                System.out.println("  Total students: " + count);
            }
        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void getBorrowedItemsByClassOrEvent(Scanner sc) {
        System.out.println("\n--- VIEW BORROWED ITEMS ---");
        System.out.println("  Filter by:");
        System.out.println("    [1] Class");
        System.out.println("    [2] Event/Activity");
        System.out.print("  Choice: ");
        String choice = sc.nextLine().trim();

        if (choice.equals("1")) {
            getBorrowedItemsByClass(sc);
        } else if (choice.equals("2")) {
            getBorrowedItemsByEvent(sc);
        } else {
            System.out.println("  Invalid choice.");
        }
    }

    private static void getBorrowedItemsByClass(Scanner sc) {
        System.out.println("\n--- BORROWED ITEMS BY CLASS ---");

        showLaboratoryClasses();

        System.out.print("  Enter Class ID: ");
        int classId;
        try {
            classId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  [INPUT ERROR] Please enter a valid Class ID.");
            return;
        }

        String sql = """
            SELECT
                br.borrow_id,
                br.borrow_date,
                br.return_date,
                br.status,
                CONCAT(u.first_name, ' ', u.last_name) AS borrower,
                u.contact_number,
                i.item_name,
                i.item_type,
                i.tag,
                i.condition_status,
                bi.quantity,
                bi.item_condition_on_return
            FROM BORROW_RECORD br
            JOIN USER u         ON u.user_id    = br.borrower_id
            JOIN BORROW_ITEM bi ON bi.borrow_id = br.borrow_id
            JOIN ITEM i         ON i.item_id    = bi.item_id
            WHERE br.remarks = ?
            ORDER BY br.borrow_date DESC, br.borrow_id
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "Class ID: " + classId);
            ResultSet rs = ps.executeQuery();

            boolean hasResults = false;
            int currentBorrowId = -1;
            String className = getClassName(classId);

            while (rs.next()) {
                if (!hasResults) {
                    System.out.println("\n  Class: " + className + "  (ID: " + classId + ")");
                    System.out.println("  " + "═".repeat(80));
                    hasResults = true;
                }

                int borrowId = rs.getInt("borrow_id");

                if (borrowId != currentBorrowId) {
                    currentBorrowId = borrowId;
                    System.out.println();
                    System.out.println("  Borrow ID   : " + borrowId);
                    System.out.println("  Borrower    : " + rs.getString("borrower")
                            + "  (" + (rs.getString("contact_number") == null
                            ? "N/A"
                            : rs.getString("contact_number"))
                            + ")");
                    System.out.println("  Borrow Date : " + rs.getString("borrow_date"));
                    System.out.println("  Return Date : " + (rs.getString("return_date") == null
                            ? "Not yet returned"
                            : rs.getString("return_date")));
                    System.out.println("  Status      : " + rs.getString("status"));
                    System.out.println("  Items:");
                    System.out.printf("    %-28s %-14s %-10s %-15s %-5s %-15s%n",
                            "Item Name", "Type", "Tag", "Condition", "Qty", "Return Condition");
                    System.out.println("    " + "─".repeat(90));
                }

                System.out.printf("    %-28s %-14s %-10s %-15s %-5d %-15s%n",
                        truncate(rs.getString("item_name"), 28),
                        rs.getString("item_type"),
                        rs.getString("tag") == null ? "N/A" : rs.getString("tag"),
                        rs.getString("condition_status"),
                        rs.getInt("quantity"),
                        rs.getString("item_condition_on_return") == null
                                ? "Not returned"
                                : rs.getString("item_condition_on_return"));
            }

            if (!hasResults) {
                System.out.println("  No borrowed items found for Class ID: " + classId);
            } else {
                System.out.println("\n  " + "═".repeat(80));
            }

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    private static void getBorrowedItemsByEvent(Scanner sc) {
        System.out.println("\n--- BORROWED ITEMS BY EVENT/ACTIVITY ---");

        showAllActivities();

        System.out.print("  Enter Activity ID: ");
        int activityId;
        try {
            activityId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  [INPUT ERROR] Please enter a valid Activity ID.");
            return;
        }

        String sql = """
            SELECT
                br.borrow_id,
                br.borrow_date,
                br.return_date,
                br.status,
                CONCAT(u.first_name, ' ', u.last_name) AS borrower,
                u.contact_number,
                i.item_name,
                i.item_type,
                i.tag,
                i.condition_status,
                bi.quantity,
                bi.item_condition_on_return
            FROM BORROW_RECORD br
            JOIN USER u         ON u.user_id    = br.borrower_id
            JOIN BORROW_ITEM bi ON bi.borrow_id = br.borrow_id
            JOIN ITEM i         ON i.item_id    = bi.item_id
            WHERE br.remarks = ?
            ORDER BY br.borrow_date DESC, br.borrow_id
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "Linked to activity ID: " + activityId);
            ResultSet rs = ps.executeQuery();

            boolean hasResults = false;
            int currentBorrowId = -1;
            String activityName = getActivityInfo(activityId);

            while (rs.next()) {
                if (!hasResults) {
                    System.out.println("\n  Activity: " + activityName + "  (ID: " + activityId + ")");
                    System.out.println("  " + "═".repeat(80));
                    hasResults = true;
                }

                int borrowId = rs.getInt("borrow_id");

                if (borrowId != currentBorrowId) {
                    currentBorrowId = borrowId;
                    System.out.println();
                    System.out.println("  Borrow ID   : " + borrowId);
                    System.out.println("  Borrower    : " + rs.getString("borrower")
                            + "  (" + (rs.getString("contact_number") == null
                            ? "N/A"
                            : rs.getString("contact_number"))
                            + ")");
                    System.out.println("  Borrow Date : " + rs.getString("borrow_date"));
                    System.out.println("  Return Date : " + (rs.getString("return_date") == null
                            ? "Not yet returned"
                            : rs.getString("return_date")));
                    System.out.println("  Status      : " + rs.getString("status"));
                    System.out.println("  Items:");
                    System.out.printf("    %-28s %-14s %-10s %-15s %-5s %-15s%n",
                            "Item Name", "Type", "Tag", "Condition", "Qty", "Return Condition");
                    System.out.println("    " + "─".repeat(90));
                }

                System.out.printf("    %-28s %-14s %-10s %-15s %-5d %-15s%n",
                        truncate(rs.getString("item_name"), 28),
                        rs.getString("item_type"),
                        rs.getString("tag") == null ? "N/A" : rs.getString("tag"),
                        rs.getString("condition_status"),
                        rs.getInt("quantity"),
                        rs.getString("item_condition_on_return") == null
                                ? "Not returned"
                                : rs.getString("item_condition_on_return"));
            }

            if (!hasResults) {
                System.out.println("  No borrowed items found for Activity ID: " + activityId);
            } else {
                System.out.println("\n  " + "═".repeat(80));
            }

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void viewBorrowStatus(Scanner sc) {
        System.out.println("\n--- VIEW BORROW STATUS ---");
        System.out.println("  Filter by:");
        System.out.println("    [1] Class");
        System.out.println("    [2] Event");
        System.out.println("    [3] All");
        System.out.print("  Choice: ");
        String choice = sc.nextLine().trim();

        String filter = switch (choice) {
            case "1" -> "Class";
            case "2" -> "Event";
            default -> null;
        };

        String sql = """
            SELECT br.borrow_id,
                   CONCAT(u.first_name, ' ', u.last_name) AS borrower,
                   CONCAT(c.first_name, ' ', c.last_name) AS custodian,
                    DATE_FORMAT(br.borrow_date, '%Y-%m-%d') AS borrow_date,
                    COALESCE(DATE_FORMAT(br.return_date, '%Y-%m-%d'), 'Not returned') AS return_date,
                   br.purpose, br.status, br.remarks
            FROM BORROW_RECORD br
            JOIN USER u ON br.borrower_id  = u.user_id
            JOIN USER c ON br.custodian_id = c.user_id
            """ + (filter != null ? "WHERE br.purpose LIKE ? " : "") + """
            ORDER BY br.borrow_date DESC
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (filter != null)
                ps.setString(1, filter + "%");

            try (ResultSet rs = ps.executeQuery()) {
                printLine();
                System.out.printf("%-6s %-22s %-22s %-12s %-13s %-15s %-20s%n",
                        "BorrID", "Borrower", "Custodian", "Borrow Date", "Return Date", "Purpose", "Status");
                printLine();

                boolean hasRows = false;
                while (rs.next()) {
                    hasRows = true;
                    System.out.printf("%-6d %-22s %-22s %-12s %-13s %-15s %-20s%n",
                            rs.getInt("borrow_id"),
                            truncate(rs.getString("borrower"), 22),
                            truncate(rs.getString("custodian"), 22),
                            rs.getString("borrow_date"),
                            truncate(rs.getString("return_date"), 13),
                            truncate(rs.getString("purpose"), 15),
                            truncate(rs.getString("status"), 20));
                }
                printLine();
                if (!hasRows)
                    System.out.println("  No records found.");
            }
        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void viewUnreturnedAndIssues() {
        System.out.println("\n--- BORROWERS WITH UNRETURNED ITEMS / RETURNS WITH ISSUES ---");

        System.out.println("\n  [A] UNRETURNED ITEMS (Still Borrowed/Overdue):");

        String unreturnedSql = """
            SELECT br.borrow_id,
                   CONCAT(COALESCE(u.first_name,''), ' ', COALESCE(u.last_name,'')) AS borrower,
                   COALESCE(u.email, 'N/A') AS email,
                   COALESCE(u.contact_number, 'N/A') AS contact_number,
                   COALESCE(DATE_FORMAT(br.borrow_date,'%Y-%m-%d'), 'N/A') AS borrow_date,
                   COALESCE(br.purpose, 'N/A') AS purpose,
                   COALESCE(br.status, 'N/A') AS status,
                   COUNT(bi.borrow_item_id) AS item_count
            FROM BORROW_RECORD br
            LEFT JOIN USER u ON br.borrower_id = u.user_id
            LEFT JOIN BORROW_ITEM bi ON br.borrow_id = bi.borrow_id
            WHERE br.status IN ('Borrowed', 'Overdue')
            GROUP BY br.borrow_id, u.first_name, u.last_name, u.email,
                     u.contact_number, br.borrow_date, br.purpose, br.status
            ORDER BY br.borrow_date ASC
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(unreturnedSql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("  %-8s %-28s %-35s %-15s %-20s %-10s %-5s%n",
                    "BorrID", "Borrower", "Email", "Contact", "Purpose", "Status", "Items");
            printLine();

            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                System.out.printf("  %-8d %-28s %-35s %-15s %-20s %-10s %-5d%n",
                        rs.getInt("borrow_id"),
                        truncate(rs.getString("borrower"), 28),
                        truncate(rs.getString("email"), 35),
                        rs.getString("contact_number"),
                        truncate(rs.getString("purpose"), 20),
                        rs.getString("status"),
                        rs.getInt("item_count"));
            }
            printLine();
            if (!hasRows)
                System.out.println("  No unreturned items found.");

        } catch (SQLException e) {
            System.out.println("  [DB ERROR - Unreturned] " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n  [B] RETURNS WITH DAMAGE/ISSUES:");

        String damagedSql = """
            SELECT rr.return_id, rr.borrow_id,
                   CONCAT(COALESCE(u.first_name,''), ' ', COALESCE(u.last_name,'')) AS borrower,
                   COALESCE(u.contact_number, 'N/A') AS contact,
                   COALESCE(DATE_FORMAT(rr.actual_return_date,'%Y-%m-%d'), 'N/A') AS return_date,
                   COALESCE(rr.damage_description, 'No description') AS damage_desc,
                   COALESCE(rr.condition_notes, 'N/A') AS condition_notes
            FROM RETURN_RECORD rr
            LEFT JOIN BORROW_RECORD br ON rr.borrow_id = br.borrow_id
            LEFT JOIN USER u ON br.borrower_id = u.user_id
            WHERE rr.has_damage = 'Yes'
            ORDER BY rr.actual_return_date DESC
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(damagedSql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("  %-8s %-8s %-28s %-15s %-12s %-35s%n",
                    "RetID", "BorrID", "Borrower", "Contact", "Return Date", "Damage");
            printLine();

            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                System.out.printf("  %-8d %-8d %-28s %-15s %-12s %-35s%n",
                        rs.getInt("return_id"),
                        rs.getInt("borrow_id"),
                        truncate(rs.getString("borrower"), 28),
                        rs.getString("contact"),
                        rs.getString("return_date"),
                        truncate(rs.getString("damage_desc"), 35));
            }
            printLine();
            if (!hasRows)
                System.out.println("  No returns with damage found.");

        } catch (SQLException e) {
            System.out.println("  [DB ERROR - Damaged] " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void viewAllItems() {
        System.out.println("\n--- ALL ITEMS / EQUIPMENT STATUS ---");
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
            System.out.printf("%-5s %-14s %-30s %-15s %-15s %-10s %-15s %-12s%n",
                    "ID", "Barcode", "Item Name", "Type", "Model", "Tag", "Condition", "Availability");
            printLine();

            while (rs.next()) {
                System.out.printf("%-5d %-14s %-30s %-15s %-15s %-10s %-15s %-12s%n",
                        rs.getInt("item_id"),
                        rs.getString("barcode"),
                        truncate(rs.getString("item_name"), 30),
                        rs.getString("item_type"),
                        rs.getString("model") == null ? "N/A" : truncate(rs.getString("model"), 15),
                        rs.getString("tag") == null ? "N/A" : truncate(rs.getString("tag"), 10),
                        rs.getString("condition_status"),
                        rs.getString("availability_status"));
            }
            printLine();

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void viewAllBorrowRecords() {
        System.out.println("\n--- ALL BORROW RECORDS ---");
        String sql = """
            SELECT br.borrow_id,
                   CONCAT(u.first_name, ' ', u.last_name) AS borrower,
                   CONCAT(c.first_name, ' ', c.last_name) AS custodian,
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
            System.out.printf("%-6s %-24s %-24s %-12s %-14s %-30s %-15s%n",
                    "ID", "Borrower", "Custodian", "Borrow Date", "Return Date", "Purpose", "Status");
            printLine();

            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                String borrowDate = rs.getString("borrow_date");
                String returnDate = rs.getString("return_date");

                System.out.printf("%-6d %-24s %-24s %-12s %-14s %-30s %-15s%n",
                        rs.getInt("borrow_id"),
                        truncate(rs.getString("borrower"), 24),
                        truncate(rs.getString("custodian"), 24),
                        borrowDate == null ? "N/A" : truncate(borrowDate, 12),
                        returnDate == null ? "Not returned" : truncate(returnDate, 14),
                        rs.getString("purpose") == null ? "N/A" : truncate(rs.getString("purpose"), 30),
                        rs.getString("status"));
            }
            printLine();
            if (!hasRows)
                System.out.println("  No borrow records found.");

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void viewPendingRequests() {
        System.out.println("\n--- PENDING BORROWER REQUESTS ---");

        String sql = """
            SELECT br.request_id, br.request_date, br.purpose, br.purpose_ref,
                   CONCAT(u.first_name, ' ', u.last_name) AS borrower_name,
                   u.email, u.contact_number,
                   GROUP_CONCAT(CONCAT(i.item_name, ' (x', ri.quantity, ')')
                                SEPARATOR ', ') AS requested_items
            FROM BORROW_REQUEST br
            JOIN USER u ON br.borrower_id = u.user_id
            LEFT JOIN REQUEST_ITEM ri ON br.request_id = ri.request_id
            LEFT JOIN ITEM i ON ri.item_id = i.item_id
            WHERE br.status = 'Pending'
            GROUP BY br.request_id, br.request_date, br.purpose, br.purpose_ref,
                     u.first_name, u.last_name, u.email, u.contact_number
            ORDER BY br.request_date ASC
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-8s %-12s %-25s %-15s %-20s %-40s%n",
                    "Req ID", "Date", "Borrower", "Contact", "Purpose", "Requested Items");
            printLine();

            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                System.out.printf("%-8d %-12s %-25s %-15s %-20s %-40s%n",
                        rs.getInt("request_id"),
                        rs.getString("request_date"),
                        truncate(rs.getString("borrower_name"), 25),
                        rs.getString("contact_number") == null ? "N/A" : rs.getString("contact_number"),
                        truncate(rs.getString("purpose"), 20),
                        truncate(rs.getString("requested_items"), 40));
            }
            printLine();
            if (!hasRows)
                System.out.println("  No pending requests found.");

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    private static void showLaboratoryClasses() {
        System.out.println("\n  --- LABORATORY CLASSES ---");
        String sql = """
            SELECT class_id, class_code, class_name, semester, academic_year
            FROM LABORATORY_CLASS
            ORDER BY academic_year DESC, semester
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("  %-6s %-12s %-35s %-10s %-10s%n",
                    "ID", "Code", "Class Name", "Semester", "Year");
            System.out.println("  " + "─".repeat(75));

            while (rs.next()) {
                System.out.printf("  %-6d %-12s %-35s %-10s %-10s%n",
                        rs.getInt("class_id"),
                        rs.getString("class_code"),
                        truncate(rs.getString("class_name"), 35),
                        rs.getString("semester"),
                        rs.getString("academic_year"));
            }
            System.out.println("  " + "─".repeat(75));

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    private static void showAllActivities() {
        System.out.println("\n  --- ACTIVITIES ---");
        String sql = """
            SELECT a.activity_id, a.activity_name, a.event_type, a.event_date,
                   a.approval_status,
                   CONCAT(u.first_name, ' ', u.last_name) AS requester
            FROM ACTIVITY a
            JOIN USER u ON a.requester_id = u.user_id
            ORDER BY a.event_date DESC
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("  %-6s %-35s %-15s %-12s %-10s %-25s%n",
                    "Act ID", "Activity Name", "Type", "Date", "Status", "Requester");
            System.out.println("  " + "─".repeat(105));

            while (rs.next()) {
                System.out.printf("  %-6d %-35s %-15s %-12s %-10s %-25s%n",
                        rs.getInt("activity_id"),
                        truncate(rs.getString("activity_name"), 35),
                        rs.getString("event_type"),
                        rs.getString("event_date"),
                        rs.getString("approval_status"),
                        truncate(rs.getString("requester"), 25));
            }
            System.out.println("  " + "─".repeat(105));

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    private static String getClassName(int classId) {
        String sql = "SELECT class_name FROM LABORATORY_CLASS WHERE class_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("class_name");
        } catch (SQLException e) {
            return "Class ID: " + classId;
        }
        return "Class ID: " + classId;
    }

    private static String getActivityInfo(int activityId) {
        String sql = "SELECT activity_name FROM ACTIVITY WHERE activity_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("activity_name");
        } catch (SQLException e) {
            return "Activity ID: " + activityId;
        }
        return "Activity ID: " + activityId;
    }


    public static void processBorrowRequest(models.DataClasses.User user, Scanner sc) {
        System.out.println("\n--- PROCESS BORROW REQUEST ---");
        viewPendingRequests();

        System.out.print("\n  Enter Request ID to process (or 0 to cancel): ");
        int requestId;
        try {
            requestId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  [INPUT ERROR] Please enter a valid Request ID.");
            return;
        }

        if (requestId == 0) {
            System.out.println("  Processing cancelled.");
            return;
        }

        System.out.print("  Decision [A = Approve / R = Reject]: ");
        String input = sc.nextLine().trim().toUpperCase();
        String decision;
        if (input.equals("A")) {
            decision = "Approved";
        } else if (input.equals("R")) {
            decision = "Rejected";
        } else {
            System.out.println("  [INPUT ERROR] Decision must be A or R only.");
            return;
        }

        System.out.print("  Remarks (press Enter to skip): ");
        String remarks = sc.nextLine().trim();
        if (remarks.isBlank()) {
            remarks = decision.equals("Approved") ? "Approved and checked out by custodian." : "Rejected by custodian.";
        }

        String sql = "{CALL custodian_ProcessBorrowRequest(?, ?, ?, ?, ?)}";

        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, requestId);
            cs.setInt(2, user.userId);
            cs.setString(3, decision);
            cs.setString(4, remarks);
            cs.registerOutParameter(5, Types.INTEGER);

            cs.execute();

            int borrowId = cs.getInt(5);
            if (decision.equals("Approved")) {
                System.out.println("  [SUCCESS] Request approved and checked out.");
                System.out.println("  Created Borrow Record ID: " + borrowId);
            } else {
                System.out.println("  [SUCCESS] Request rejected successfully.");
            }

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void logReturnedItems(models.DataClasses.User user, Scanner sc) {
        System.out.println("\n--- LOG RETURNED ITEMS ---");
        viewAllBorrowRecords();

        System.out.print("\n  Enter Borrow ID to log return (or 0 to cancel): ");
        int borrowId;
        try {
            borrowId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  [INPUT ERROR] Please enter a valid Borrow ID.");
            return;
        }

        if (borrowId == 0) {
            System.out.println("  Return logging cancelled.");
            return;
        }

        System.out.print("  Were there damages? (Y/N): ");
        String damageInput = sc.nextLine().trim();
        String hasDamage;
        if (damageInput.equalsIgnoreCase("Y")) {
            hasDamage = "Yes";
        } else if (damageInput.equalsIgnoreCase("N")) {
            hasDamage = "No";
        } else {
            System.out.println("  [INPUT ERROR] Please enter Y or N only.");
            return;
        }

        System.out.print("  Condition notes: ");
        String conditionNotes = sc.nextLine().trim();
        if (conditionNotes.isBlank()) {
            conditionNotes = hasDamage.equals("Yes") ? "Returned with damage/s." : "Returned in good condition.";
        }

        String damageDescription = null;
        if (hasDamage.equals("Yes")) {
            System.out.print("  Damage description: ");
            damageDescription = sc.nextLine().trim();
            if (damageDescription.isBlank()) {
                damageDescription = "Damage reported by custodian.";
            }
        }

        String sql = "{CALL custodian_LogReturn(?, ?, ?, ?, ?)}";

        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, borrowId);
            cs.setInt(2, user.userId);
            cs.setString(3, hasDamage);
            cs.setString(4, conditionNotes);
            cs.setString(5, damageDescription);

            cs.executeUpdate();

            System.out.println("  [SUCCESS] Return logged successfully.");
            System.out.println("  Borrow record and item availability were updated.");

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void viewItemAvailabilitySummary() {
        System.out.println("\n--- ITEM AVAILABILITY SUMMARY ---");
        String sql = """
            SELECT availability_status, condition_status, COUNT(*) AS item_count
            FROM item
            GROUP BY availability_status, condition_status
            ORDER BY availability_status, condition_status
            """;

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            printLine();
            System.out.printf("%-20s %-22s %-10s%n", "Availability", "Condition", "Count");
            printLine();

            int total = 0;
            while (rs.next()) {
                int count = rs.getInt("item_count");
                System.out.printf("%-20s %-22s %-10d%n",
                        rs.getString("availability_status"),
                        rs.getString("condition_status"),
                        count);
                total += count;
            }
            printLine();
            System.out.println("  Total items counted: " + total);
            System.out.println("  JDBC interface demonstrated here: Statement");

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void countItemsInBorrowRecord(Scanner sc) {
        System.out.println("\n--- COUNT ITEMS IN BORROW RECORD ---");
        System.out.print("  Enter Borrow ID: ");

        int borrowId;
        try {
            borrowId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  [INPUT ERROR] Please enter a valid Borrow ID.");
            return;
        }

        String sql = "{? = CALL fn_BorrowItemCount(?)}";

        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2, borrowId);
            cs.execute();

            int itemCount = cs.getInt(1);
            System.out.println("  Borrow ID " + borrowId + " has " + itemCount + " borrowed item record/s.");
            System.out.println("  Stored function used: fn_BorrowItemCount");

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }


    public static void addNewItem(Scanner sc) {
        System.out.println("\n--- ADD EQUIPMENT / ACCESSORY / PERIPHERAL ---");

        System.out.print("  Barcode: ");
        String barcode = sc.nextLine().trim();
        System.out.print("  Item Name: ");
        String itemName = sc.nextLine().trim();
        System.out.print("  Item Type [Equipment/Peripheral/Accessory]: ");
        String itemType = normalizeChoice(sc.nextLine().trim(), "Equipment", "Peripheral", "Accessory");
        System.out.print("  Description: ");
        String description = emptyToNull(sc.nextLine().trim());
        System.out.print("  Model: ");
        String model = emptyToNull(sc.nextLine().trim());
        System.out.print("  Tag: ");
        String tag = emptyToNull(sc.nextLine().trim());
        System.out.print("  Condition [Good/Damaged/Under Maintenance]: ");
        String condition = normalizeChoice(sc.nextLine().trim(), "Good", "Damaged", "Under Maintenance");
        System.out.print("  Date Acquired [YYYY-MM-DD, blank if unknown]: ");
        String dateAcquired = emptyToNull(sc.nextLine().trim());

        if (barcode.isBlank() || itemName.isBlank()) {
            System.out.println("  [INPUT ERROR] Barcode and item name are required.");
            return;
        }
        if (itemType == null || condition == null) {
            System.out.println("  [INPUT ERROR] Invalid item type or condition.");
            return;
        }
        if (dateAcquired != null && !dateAcquired.matches("\\d{4}-\\d{2}-\\d{2}")) {
            System.out.println("  [INPUT ERROR] Date must follow YYYY-MM-DD.");
            return;
        }

        String existsSql = "SELECT COUNT(*) FROM item WHERE barcode = ?";
        String insertSql = """
            INSERT INTO item
                (barcode, item_name, item_type, description, model, tag, condition_status, availability_status, date_acquired)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'Available', ?)
            """;

        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement check = conn.prepareStatement(existsSql)) {
                check.setString(1, barcode);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        System.out.println("  [VALIDATION ERROR] Barcode already exists.");
                        return;
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, barcode);
                ps.setString(2, itemName);
                ps.setString(3, itemType);
                ps.setString(4, description);
                ps.setString(5, model);
                ps.setString(6, tag);
                ps.setString(7, condition);
                if (dateAcquired == null) {
                    ps.setNull(8, Types.DATE);
                } else {
                    ps.setString(8, dateAcquired);
                }

                int affected = ps.executeUpdate();
                if (affected > 0) {
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            System.out.println("  Item added successfully. New Item ID: " + keys.getInt(1));
                        } else {
                            System.out.println("  Item added successfully.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void updateItemDetails(Scanner sc) {
        System.out.println("\n--- UPDATE ITEM DETAILS / STATUS ---");
        viewAllItems();
        System.out.print("\n  Enter Item ID to update: ");
        Integer itemId = readInt(sc.nextLine().trim());
        if (itemId == null) {
            System.out.println("  [INPUT ERROR] Please enter a valid Item ID.");
            return;
        }

        String selectSql = """
            SELECT barcode, item_name, item_type, description, model, tag,
                   condition_status, availability_status, date_acquired
            FROM item
            WHERE item_id = ?
            """;
        String updateSql = """
            UPDATE item
            SET barcode = ?, item_name = ?, item_type = ?, description = ?, model = ?, tag = ?,
                condition_status = ?, availability_status = ?, date_acquired = ?
            WHERE item_id = ?
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement select = conn.prepareStatement(selectSql)) {

            select.setInt(1, itemId);
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("  [VALIDATION ERROR] Item ID not found.");
                    return;
                }

                String oldBarcode = rs.getString("barcode");
                String oldName = rs.getString("item_name");
                String oldType = rs.getString("item_type");
                String oldDescription = rs.getString("description");
                String oldModel = rs.getString("model");
                String oldTag = rs.getString("tag");
                String oldCondition = rs.getString("condition_status");
                String oldAvailability = rs.getString("availability_status");
                String oldDate = rs.getString("date_acquired");

                System.out.println("  Leave input blank to keep the current value.");
                System.out.print("  Barcode [" + oldBarcode + "]: ");
                String barcode = keepIfBlank(sc.nextLine().trim(), oldBarcode);
                System.out.print("  Item Name [" + oldName + "]: ");
                String itemName = keepIfBlank(sc.nextLine().trim(), oldName);
                System.out.print("  Item Type [" + oldType + "] Equipment/Peripheral/Accessory: ");
                String itemTypeInput = sc.nextLine().trim();
                String itemType = itemTypeInput.isBlank() ? oldType : normalizeChoice(itemTypeInput, "Equipment", "Peripheral", "Accessory");
                System.out.print("  Description [" + nullToEmpty(oldDescription) + "]: ");
                String description = keepNullable(sc.nextLine().trim(), oldDescription);
                System.out.print("  Model [" + nullToEmpty(oldModel) + "]: ");
                String model = keepNullable(sc.nextLine().trim(), oldModel);
                System.out.print("  Tag [" + nullToEmpty(oldTag) + "]: ");
                String tag = keepNullable(sc.nextLine().trim(), oldTag);
                System.out.print("  Condition [" + oldCondition + "] Good/Damaged/Under Maintenance: ");
                String conditionInput = sc.nextLine().trim();
                String condition = conditionInput.isBlank() ? oldCondition : normalizeChoice(conditionInput, "Good", "Damaged", "Under Maintenance");
                System.out.print("  Availability [" + oldAvailability + "] Available/Borrowed: ");
                String availabilityInput = sc.nextLine().trim();
                String availability = availabilityInput.isBlank() ? oldAvailability : normalizeChoice(availabilityInput, "Available", "Borrowed");
                System.out.print("  Date Acquired [" + nullToEmpty(oldDate) + "] YYYY-MM-DD: ");
                String dateInput = sc.nextLine().trim();
                String dateAcquired = dateInput.isBlank() ? oldDate : dateInput;

                if (barcode.isBlank() || itemName.isBlank() || itemType == null || condition == null || availability == null) {
                    System.out.println("  [INPUT ERROR] Invalid update values.");
                    return;
                }
                if (dateAcquired != null && !dateAcquired.isBlank() && !dateAcquired.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    System.out.println("  [INPUT ERROR] Date must follow YYYY-MM-DD.");
                    return;
                }

                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    update.setString(1, barcode);
                    update.setString(2, itemName);
                    update.setString(3, itemType);
                    update.setString(4, description);
                    update.setString(5, model);
                    update.setString(6, tag);
                    update.setString(7, condition);
                    update.setString(8, availability);
                    if (dateAcquired == null || dateAcquired.isBlank()) {
                        update.setNull(9, Types.DATE);
                    } else {
                        update.setString(9, dateAcquired);
                    }
                    update.setInt(10, itemId);

                    int affected = update.executeUpdate();
                    System.out.println("  Item update completed. Affected row/s: " + affected);
                }
            }
        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void deleteItemFromInventory(Scanner sc) {
        System.out.println("\n--- DELETE ITEM FROM INVENTORY ---");
        viewAllItems();
        System.out.print("\n  Enter Item ID to delete: ");
        Integer itemId = readInt(sc.nextLine().trim());
        if (itemId == null) {
            System.out.println("  [INPUT ERROR] Please enter a valid Item ID.");
            return;
        }

        System.out.print("  Type DELETE to confirm: ");
        String confirm = sc.nextLine().trim();
        if (!confirm.equals("DELETE")) {
            System.out.println("  Delete cancelled.");
            return;
        }

        String activeBorrowSql = """
            SELECT COUNT(*)
            FROM borrow_item bi
            JOIN borrow_record br ON bi.borrow_id = br.borrow_id
            WHERE bi.item_id = ? AND br.status IN ('Borrowed', 'Overdue')
            """;
        String referenceSql = """
            SELECT
                (SELECT COUNT(*) FROM borrow_item WHERE item_id = ?) +
                (SELECT COUNT(*) FROM request_item WHERE item_id = ?) AS total_refs
            """;
        String deleteSql = "DELETE FROM item WHERE item_id = ?";

        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(activeBorrowSql)) {
                ps.setInt(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        System.out.println("  [VALIDATION ERROR] This item is currently borrowed/overdue and cannot be deleted.");
                        return;
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(referenceSql)) {
                ps.setInt(1, itemId);
                ps.setInt(2, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt("total_refs") > 0) {
                        System.out.println("  [VALIDATION ERROR] This item already appears in request/borrow history.");
                        System.out.println("  To preserve transaction history, update its condition/status instead of deleting it.");
                        return;
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setInt(1, itemId);
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    System.out.println("  [VALIDATION ERROR] Item ID not found.");
                } else {
                    System.out.println("  Item deleted successfully. Affected row/s: " + affected);
                }
            }
        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void createActivityEvent(models.DataClasses.User user, Scanner sc) {
        System.out.println("\n--- CREATE ACTIVITY / EVENT REQUEST RECORD ---");
        System.out.print("  Activity Name: ");
        String activityName = sc.nextLine().trim();
        System.out.print("  Event Type [Meeting/Training/Recruitment/Certification/Other]: ");
        String eventType = emptyToNull(sc.nextLine().trim());
        System.out.print("  Event Date [YYYY-MM-DD]: ");
        String eventDate = sc.nextLine().trim();
        System.out.print("  Event Time [HH:MM, blank if unknown]: ");
        String eventTime = emptyToNull(sc.nextLine().trim());
        if (eventTime != null && eventTime.matches("\\d{2}:\\d{2}")) {
            eventTime += ":00";
        }
        System.out.print("  Location: ");
        String location = emptyToNull(sc.nextLine().trim());
        System.out.print("  Facility ID [blank if none/TBA]: ");
        String facilityInput = sc.nextLine().trim();
        Integer facilityId = facilityInput.isBlank() ? null : readInt(facilityInput);
        System.out.print("  Approval Status [Pending/Approved/Rejected]: ");
        String status = normalizeChoice(sc.nextLine().trim(), "Pending", "Approved", "Rejected");

        if (activityName.isBlank() || !eventDate.matches("\\d{4}-\\d{2}-\\d{2}") || status == null) {
            System.out.println("  [INPUT ERROR] Activity name, valid date, and valid approval status are required.");
            return;
        }
        if (!facilityInput.isBlank() && facilityId == null) {
            System.out.println("  [INPUT ERROR] Facility ID must be numeric.");
            return;
        }
        if (eventTime != null && !eventTime.matches("\\d{2}:\\d{2}:\\d{2}")) {
            System.out.println("  [INPUT ERROR] Time must follow HH:MM.");
            return;
        }

        String facilityCheck = "SELECT COUNT(*) FROM facility WHERE facility_id = ?";
        String sql = """
            INSERT INTO activity
                (facility_id, activity_name, event_type, event_date, event_time, location,
                 requester_id, approved_by, approval_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnection()) {
            if (facilityId != null) {
                try (PreparedStatement check = conn.prepareStatement(facilityCheck)) {
                    check.setInt(1, facilityId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            System.out.println("  [VALIDATION ERROR] Facility ID not found.");
                            return;
                        }
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                if (facilityId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, facilityId);
                ps.setString(2, activityName);
                ps.setString(3, eventType);
                ps.setString(4, eventDate);
                ps.setString(5, eventTime);
                ps.setString(6, location);
                ps.setInt(7, user.userId);
                if (status.equals("Approved")) ps.setInt(8, user.userId); else ps.setNull(8, Types.INTEGER);
                ps.setString(9, status);

                int affected = ps.executeUpdate();
                if (affected > 0) {
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            System.out.println("  Activity/event record created. New Activity ID: " + keys.getInt(1));
                        } else {
                            System.out.println("  Activity/event record created.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void markBorrowRecordOverdue(Scanner sc) {
        System.out.println("\n--- MARK BORROW RECORD AS OVERDUE ---");
        System.out.print("  Enter Borrow ID: ");
        Integer borrowId = readInt(sc.nextLine().trim());
        if (borrowId == null) {
            System.out.println("  [INPUT ERROR] Please enter a valid Borrow ID.");
            return;
        }

        String checkSql = "SELECT status FROM borrow_record WHERE borrow_id = ?";
        String updateSql = """
            UPDATE borrow_record
            SET status = 'Overdue', remarks = COALESCE(remarks, 'Marked overdue by custodian.')
            WHERE borrow_id = ? AND status = 'Borrowed'
            """;

        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setInt(1, borrowId);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("  [VALIDATION ERROR] Borrow record not found.");
                        return;
                    }
                    String status = rs.getString("status");
                    if (!"Borrowed".equals(status)) {
                        System.out.println("  [VALIDATION ERROR] Only records with status 'Borrowed' can be marked as overdue. Current status: " + status);
                        return;
                    }
                }
            }

            try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                update.setInt(1, borrowId);
                int affected = update.executeUpdate();
                System.out.println("  Borrow record marked overdue. Affected row/s: " + affected);
            }
        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    public static void syncItemAvailability() {
        System.out.println("\n--- SYNC ITEM AVAILABILITY WITH ACTIVE BORROW RECORDS ---");
        String markBorrowed = """
            UPDATE item
            SET availability_status = 'Borrowed'
            WHERE item_id IN (
                SELECT borrowed_items.item_id
                FROM (
                    SELECT DISTINCT bi.item_id
                    FROM borrow_item bi
                    JOIN borrow_record br ON bi.borrow_id = br.borrow_id
                    WHERE br.status IN ('Borrowed', 'Overdue')
                ) borrowed_items
            )
            """;
        String markAvailable = """
            UPDATE item
            SET availability_status = 'Available'
            WHERE item_id NOT IN (
                SELECT borrowed_items.item_id
                FROM (
                    SELECT DISTINCT bi.item_id
                    FROM borrow_item bi
                    JOIN borrow_record br ON bi.borrow_id = br.borrow_id
                    WHERE br.status IN ('Borrowed', 'Overdue')
                ) borrowed_items
            )
            AND availability_status = 'Borrowed'
            """;

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            int borrowedRows = stmt.executeUpdate(markBorrowed);
            int availableRows = stmt.executeUpdate(markAvailable);
            System.out.println("  Sync completed using Statement.");
            System.out.println("  Rows marked Borrowed: " + borrowedRows);
            System.out.println("  Rows marked Available: " + availableRows);
        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    private static Integer readInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeChoice(String input, String... allowedValues) {
        for (String value : allowedValues) {
            if (value.equalsIgnoreCase(input)) {
                return value;
            }
        }
        return null;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String keepIfBlank(String input, String oldValue) {
        return input.isBlank() ? oldValue : input;
    }

    private static String keepNullable(String input, String oldValue) {
        return input.isBlank() ? oldValue : input;
    }

    private static void printLine() {
        System.out.println("  " + "─".repeat(140));
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null)
            return "N/A";
        if (s.length() <= maxLen)
            return s;
        return s.substring(0, maxLen - 3) + "...";
    }
}
