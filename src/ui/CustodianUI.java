package ui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

import dao.Database;
import models.DataClasses;

public class CustodianUI {

   public static void menu(DataClasses.User user, Scanner sc) {
      boolean back = false;
      while (!back) {
         System.out.println("\n╔════════════════════════════════════════════════════════════════════╗");
         System.out.println("║                         CUSTODIAN MENU                              ║");
         System.out.println("║  Logged in as: " + padRight(user.getFullName(), 48) + "║");
         System.out.println("╠════════════════════════════════════════════════════════════════════╣");
         System.out.println("║  [1]  View Student Data (Currently Enrolled)                       ║");
         System.out.println("║  [2]  View CIS Staff and Faculty                                   ║");
         System.out.println("║  [3]  View Laboratory Classes (with students)                      ║");
         System.out.println("║  [4]  View Borrowed Items (by Class or Event)                      ║");
         System.out.println("║  [5]  View Borrow Status (by Class or Event)                       ║");
         System.out.println("║  [6]  View Borrowers with Unreturned Items / Returns with Issues   ║");
         System.out.println("║  [7]  View All Items / Equipment Status                            ║");
         System.out.println("║  [8]  View All Borrow Records                                      ║");
         System.out.println("║  [9]  View Pending Borrower Requests                               ║");
         System.out.println("║  [0]  Logout                                                       ║");
         System.out.println("╚════════════════════════════════════════════════════════════════════╝");
         System.out.print("  Choice: ");
         String choice = sc.nextLine().trim();
         switch (choice) {
            case "1" -> extractStudents();
            case "2" -> viewAllStaffFaculty();
            case "3" -> extractLabClasses(sc);
            case "4" -> getBorrowedItemsByClassOrEvent(sc);
            case "5" -> viewBorrowStatus(sc);
            case "6" -> viewUnreturnedAndIssues();
            case "7" -> viewAllItems();
            case "8" -> viewAllBorrowRecords();
            case "9" -> viewPendingRequests();
            case "0" -> back = true;
            default -> System.out.println("  Invalid choice.");
         }
      }
   }

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