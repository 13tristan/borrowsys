package ui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import dao.Database;
import models.DataClasses;

public class CustodianUI {

   // ─── MENU ────────────────────────────────────────────────────────────────
   public static void menu(DataClasses.User user, Scanner sc) {
      boolean back = false;
      while (!back) {
         System.out.println("\n╔════════════════════════════════════════════════════════════════════╗");
         System.out.println("║                         CUSTODIAN MENU                              ║");
         System.out.println("║  Logged in as: " + padRight(user.getFullName(), 48) + "║");
         System.out.println("╠════════════════════════════════════════════════════════════════════╣");
         System.out.println("║  [1]  Input Equipment / Accessory / Peripheral Data                ║");
         System.out.println("║  [2]  Extract Student Data (Currently Enrolled)                    ║");
         System.out.println("║  [3]  Extract / Input CIS Staff and Faculty                        ║");
         System.out.println("║  [4]  Extract Laboratory Classes (with students)                   ║");
         System.out.println("║  [5]  Log Borrowed Items (Class or Event)                          ║");
         System.out.println("║  [6]  Log Returned Items (with damage/issues)                      ║");
         System.out.println("║  [7]  View Borrow Status (by Class or Event)                       ║");
         System.out.println("║  [8]  View Borrowers with Unreturned Items / Returns with Issues   ║");
         System.out.println("║  [9] View All Items / Equipment Status                            ║");
         System.out.println("║  [10] View All Borrow Records                                      ║");
         System.out.println("║  [11] Approve/Process Borrower Requests                            ║");
         System.out.println("║  [0]  Logout                                                       ║");
         System.out.println("╚════════════════════════════════════════════════════════════════════╝");
         System.out.print("  Choice: ");
         String choice = sc.nextLine().trim();
         switch (choice) {
            case "1" -> inputEquipment(sc);
            case "2" -> extractStudents();
            case "3" -> manageStaffFaculty(sc);
            case "4" -> extractLabClasses(sc);
            case "5" -> logBorrowedItems(sc);
            case "6" -> logReturnedItems(sc);
            case "7" -> viewBorrowStatus(sc);
            case "8" -> viewUnreturnedAndIssues();
            case "9" -> viewAllItems();
            case "10" -> viewAllBorrowRecords();
            case "11" -> approveBorrowRequests(sc, user);
            case "0" -> back = true;
            default -> System.out.println("  Invalid choice.");
         }
      }
   }

   // ─── REQUIREMENT 1: INPUT EQUIPMENT / ACCESSORY / PERIPHERAL DATA ────────
   public static void inputEquipment(Scanner sc) {
      System.out.println("\n--- INPUT NEW EQUIPMENT / ACCESSORY / PERIPHERAL ---");

      System.out.print("  Barcode: ");
      String barcode = sc.nextLine().trim();

      System.out.print("  Item Name: ");
      String itemName = sc.nextLine().trim();

      System.out.print("  Item Type (Equipment/Accessory/Peripheral): ");
      String itemType = sc.nextLine().trim();

      System.out.print("  Description: ");
      String description = sc.nextLine().trim();

      System.out.print("  Model: ");
      String model = sc.nextLine().trim();

      System.out.print("  Tag/Asset Tag: ");
      String tag = sc.nextLine().trim();

      System.out.print("  Condition Status (Good/Damaged/Under Repair): ");
      String conditionStatus = sc.nextLine().trim();

      System.out.print("  Availability Status (Available/Borrowed/Reserved): ");
      String availabilityStatus = sc.nextLine().trim();

      System.out.print("  Date Acquired (YYYY-MM-DD): ");
      String dateAcquired = sc.nextLine().trim();

      String sql = """
            INSERT INTO ITEM (barcode, item_name, item_type, description,
                              model, tag, condition_status, availability_status, date_acquired)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

         ps.setString(1, barcode);
         ps.setString(2, itemName);
         ps.setString(3, itemType);
         ps.setString(4, description);
         ps.setString(5, model.isEmpty() ? null : model);
         ps.setString(6, tag.isEmpty() ? null : tag);
         ps.setString(7, conditionStatus);
         ps.setString(8, availabilityStatus);
         ps.setString(9, dateAcquired);

         int rows = ps.executeUpdate();
         if (rows > 0) {
            System.out.println("  ✓ Equipment added successfully!");
         }
      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── REQUIREMENT 2: EXTRACT STUDENT DATA (CURRENTLY ENROLLED) ─────────────
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

   // ─── REQUIREMENT 3: EXTRACT / INPUT CIS STAFF AND FACULTY ─────────────────
   public static void manageStaffFaculty(Scanner sc) {
      System.out.println("\n--- MANAGE CIS STAFF & FACULTY ---");
      System.out.println("  [1] View All CIS Staff and Faculty");
      System.out.println("  [2] Add New Staff/Faculty");
      System.out.println("  [3] Back");
      System.out.print("  Choice: ");
      String choice = sc.nextLine().trim();

      switch (choice) {
         case "1" -> viewAllStaffFaculty();
         case "2" -> addStaffFaculty(sc);
         default -> {
         }
      }
   }

   private static void viewAllStaffFaculty() {
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

   private static void addStaffFaculty(Scanner sc) {
      System.out.println("\n--- ADD NEW STAFF OR FACULTY ---");

      System.out.print("  Type (Staff/Instructor): ");
      String userType = sc.nextLine().trim();

      if (!userType.equalsIgnoreCase("Staff") && !userType.equalsIgnoreCase("Instructor")) {
         System.out.println("  Invalid type. Must be 'Staff' or 'Instructor'.");
         return;
      }

      System.out.print("  First Name: ");
      String firstName = sc.nextLine().trim();

      System.out.print("  Last Name: ");
      String lastName = sc.nextLine().trim();

      System.out.print("  Email: ");
      String email = sc.nextLine().trim();

      System.out.print("  Contact Number: ");
      String contact = sc.nextLine().trim();

      System.out.print("  Department (default: CIS): ");
      String department = sc.nextLine().trim();
      if (department.isEmpty())
         department = "CIS";

      System.out.print("  Password (default: password123): ");
      String password = sc.nextLine().trim();
      if (password.isEmpty())
         password = "password123";

      String sql = """
            INSERT INTO USER (first_name, last_name, email, contact_number,
                              user_type, department, password, account_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'Active')
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

         ps.setString(1, firstName);
         ps.setString(2, lastName);
         ps.setString(3, email);
         ps.setString(4, contact);
         ps.setString(5, userType);
         ps.setString(6, department);
         ps.setString(7, password);

         int rows = ps.executeUpdate();
         if (rows > 0) {
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
               System.out.println("  ✓ " + userType + " added successfully! User ID: " + keys.getInt(1));
            }
         }
      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── REQUIREMENT 4: EXTRACT LABORATORY CLASSES (WITH STUDENTS) ────────────
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
      if (!input.equals("0") && input.matches("\\d+")) {
         viewStudentsInClass(Integer.parseInt(input));
      }
   }

   private static void viewStudentsInClass(int classId) {
      String sql = """
            SELECT u.user_id, u.first_name, u.last_name, u.email, cs.enrollment_status
            FROM CLASS_STUDENT cs
            JOIN USER u ON cs.student_id = u.user_id
            WHERE cs.class_id = ? AND cs.enrollment_status = 'Enrolled'
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

   // ─── REQUIREMENT 6: LOG BORROWED ITEMS ────────────────────────────────────
   public static void logBorrowedItems(Scanner sc) {
      System.out.println("\n--- LOG BORROWED ITEMS ---");
      System.out.println("  Select purpose:");
      System.out.println("    [1] Class");
      System.out.println("    [2] Event/Activity");
      System.out.print("  Choice: ");
      String purposeChoice = sc.nextLine().trim();

      switch (purposeChoice) {
         case "1" -> logBorrowForClass(sc);
         case "2" -> {
            System.out.println("    [1] Create new event log");
            System.out.println("    [2] Use existing activity request");
            System.out.print("  Choice: ");
            String eventChoice = sc.nextLine().trim();
            if (eventChoice.equals("1")) {
               logBorrowForNewEvent(sc);
            } else if (eventChoice.equals("2")) {
               logBorrowForExistingActivity(sc);
            } else {
               System.out.println("  Invalid choice.");
            }
         }
         default -> System.out.println("  Invalid choice.");
      }
   }

   private static void logBorrowForClass(Scanner sc) {
      System.out.println("\n--- LOG BORROWED ITEMS FOR CLASS ---");

      showLaboratoryClasses();

      System.out.print("  Enter Class ID: ");
      int classId = Integer.parseInt(sc.nextLine().trim());

      System.out.print("  Borrower ID (Student or Instructor ID): ");
      int borrowerId = Integer.parseInt(sc.nextLine().trim());

      System.out.print("  Custodian ID (your ID): ");
      int custodianId = Integer.parseInt(sc.nextLine().trim());

      System.out.print("  Borrow Date (YYYY-MM-DD): ");
      String borrowDate = sc.nextLine().trim();

      String className = getClassName(classId);

      String borrowSql = """
            INSERT INTO BORROW_RECORD (borrower_id, custodian_id, borrow_date,
                                       purpose, status, remarks)
            VALUES (?, ?, ?, ?, 'Borrowed', ?)
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(borrowSql, Statement.RETURN_GENERATED_KEYS)) {

         ps.setInt(1, borrowerId);
         ps.setInt(2, custodianId);
         ps.setString(3, borrowDate);
         ps.setString(4, "Class: " + className);
         ps.setString(5, "Class ID: " + classId);

         int rows = ps.executeUpdate();
         if (rows > 0) {
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
               int borrowId = keys.getInt(1);
               System.out.println("  ✓ Borrow record created! Borrow ID: " + borrowId);
               addItemsToBorrow(sc, conn, borrowId);
               System.out.println("\n  ✓ Borrowing logged successfully!");
            }
         }
      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   private static void logBorrowForNewEvent(Scanner sc) {
      System.out.println("\n--- LOG BORROWED ITEMS FOR NEW EVENT ---");

      System.out.print("  Event Name: ");
      String eventName = sc.nextLine().trim();

      System.out.print("  Borrower ID: ");
      int borrowerId = Integer.parseInt(sc.nextLine().trim());

      System.out.print("  Custodian ID (your ID): ");
      int custodianId = Integer.parseInt(sc.nextLine().trim());

      System.out.print("  Borrow Date (YYYY-MM-DD): ");
      String borrowDate = sc.nextLine().trim();

      String borrowSql = """
            INSERT INTO BORROW_RECORD (borrower_id, custodian_id, borrow_date,
                                       purpose, status, remarks)
            VALUES (?, ?, ?, ?, 'Borrowed', ?)
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(borrowSql, Statement.RETURN_GENERATED_KEYS)) {

         ps.setInt(1, borrowerId);
         ps.setInt(2, custodianId);
         ps.setString(3, borrowDate);
         ps.setString(4, "Event: " + eventName);
         ps.setString(5, "New event");

         int rows = ps.executeUpdate();
         if (rows > 0) {
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
               int borrowId = keys.getInt(1);
               System.out.println("  ✓ Borrow record created! Borrow ID: " + borrowId);
               addItemsToBorrow(sc, conn, borrowId);
               System.out.println("\n  ✓ Borrowing logged successfully!");
            }
         }
      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   private static void logBorrowForExistingActivity(Scanner sc) {
      System.out.println("\n--- LOG BORROWED ITEMS FOR EXISTING ACTIVITY ---");

      showPendingActivities();

      System.out.print("  Enter Activity ID: ");
      int activityId = Integer.parseInt(sc.nextLine().trim());

      System.out.print("  Custodian ID (your ID): ");
      int custodianId = Integer.parseInt(sc.nextLine().trim());

      System.out.print("  Borrow Date (YYYY-MM-DD): ");
      String borrowDate = sc.nextLine().trim();

      String activityInfo = getActivityInfo(activityId);

      String borrowSql = """
            INSERT INTO BORROW_RECORD (borrower_id, custodian_id, borrow_date,
                                       purpose, status, remarks)
            VALUES ((SELECT requester_id FROM ACTIVITY WHERE activity_id = ?),
                    ?, ?, ?, 'Borrowed', ?)
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(borrowSql, Statement.RETURN_GENERATED_KEYS)) {

         ps.setInt(1, activityId);
         ps.setInt(2, custodianId);
         ps.setString(3, borrowDate);
         ps.setString(4, "Event: " + activityInfo);
         ps.setString(5, "Linked to activity ID: " + activityId);

         int rows = ps.executeUpdate();
         if (rows > 0) {
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
               int borrowId = keys.getInt(1);
               System.out.println("  ✓ Borrow record created! Borrow ID: " + borrowId);
               addItemsToBorrow(sc, conn, borrowId);
               System.out.println("\n  ✓ Borrowing logged successfully!");
            }
         }
      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── REQUIREMENT 7: LOG RETURNED ITEMS (WITH DAMAGE/ISSUES) ───────────────
   public static void logReturnedItems(Scanner sc) {
      System.out.println("\n--- LOG RETURNED ITEMS ---");

      System.out.print("  Enter Borrow ID to return: ");
      int borrowId = Integer.parseInt(sc.nextLine().trim());

      viewItemsInBorrowRecord(borrowId);

      System.out.print("  Custodian ID (your ID): ");
      int custodianId = Integer.parseInt(sc.nextLine().trim());

      System.out.print("  Actual Return Date (YYYY-MM-DD): ");
      String returnDate = sc.nextLine().trim();

      System.out.print("  Has damage/issues? (Yes/No): ");
      String hasDamage = sc.nextLine().trim();

      String conditionNotes = null;
      String damageDesc = null;

      if (hasDamage.equalsIgnoreCase("Yes")) {
         System.out.print("  Condition Notes: ");
         conditionNotes = sc.nextLine().trim();
         System.out.print("  Damage Description: ");
         damageDesc = sc.nextLine().trim();
      }

      String returnSql = """
            INSERT INTO RETURN_RECORD (borrow_id, custodian_id, actual_return_date,
                                       condition_notes, has_damage, damage_description)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(returnSql)) {

         ps.setInt(1, borrowId);
         ps.setInt(2, custodianId);
         ps.setString(3, returnDate);
         ps.setString(4, conditionNotes);
         ps.setString(5, hasDamage);
         ps.setString(6, damageDesc);
         ps.executeUpdate();

         String updateBorrow = "UPDATE BORROW_RECORD SET return_date = ?, status = 'Returned' WHERE borrow_id = ?";
         try (PreparedStatement ps2 = conn.prepareStatement(updateBorrow)) {
            ps2.setString(1, returnDate);
            ps2.setInt(2, borrowId);
            ps2.executeUpdate();
         }

         String updateItems = """
               UPDATE ITEM i
               JOIN BORROW_ITEM bi ON i.item_id = bi.item_id
               SET i.availability_status = 'Available',
                   i.condition_status = CASE
                       WHEN ? = 'Yes' THEN 'Damaged'
                       ELSE i.condition_status
                   END
               WHERE bi.borrow_id = ?
               """;
         try (PreparedStatement ps3 = conn.prepareStatement(updateItems)) {
            ps3.setString(1, hasDamage);
            ps3.setInt(2, borrowId);
            ps3.executeUpdate();
         }

         String updateBorrowItem = "UPDATE BORROW_ITEM SET item_condition_on_return = ? WHERE borrow_id = ?";
         try (PreparedStatement ps4 = conn.prepareStatement(updateBorrowItem)) {
            ps4.setString(1, hasDamage.equalsIgnoreCase("Yes") ? "Damaged" : "Good");
            ps4.setInt(2, borrowId);
            ps4.executeUpdate();
         }

         System.out.println("  ✓ Return logged successfully!");

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── REQUIREMENT 8: VIEW BORROW STATUS BY CLASS OR EVENT ─────────────────
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
                   br.borrow_date, br.return_date,
                   br.purpose, br.status, br.remarks
            FROM BORROW_RECORD br
            JOIN USER u ON br.borrower_id = u.user_id
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
            System.out.printf("%-6s %-22s %-22s %-12s %-12s %-15s %-20s%n",
                  "BorrID", "Borrower", "Custodian", "Borrow Date", "Return Date", "Purpose", "Status");
            printLine();

            while (rs.next()) {
               System.out.printf("%-6d %-22s %-22s %-12s %-12s %-15s %-20s%n",
                     rs.getInt("borrow_id"),
                     truncate(rs.getString("borrower"), 22),
                     truncate(rs.getString("custodian"), 22),
                     rs.getString("borrow_date"),
                     rs.getString("return_date") == null ? "Not returned" : rs.getString("return_date"),
                     truncate(rs.getString("purpose"), 15),
                     rs.getString("status"));
            }
            printLine();
         }
      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── REQUIREMENT 9: VIEW UNRETURNED / RETURNS WITH ISSUES ────────────────
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

   // ─── REQUIREMENT 10: VIEW ALL ITEMS / EQUIPMENT STATUS ───────────────────
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

   // ─── REQUIREMENT 11: VIEW ALL BORROW RECORDS ─────────────────────────────
   public static void viewAllBorrowRecords() {
      System.out.println("\n--- ALL BORROW RECORDS ---");
      String sql = """
            SELECT br.borrow_id,
                   CONCAT(u.first_name, ' ', u.last_name) AS borrower,
                   CONCAT(c.first_name, ' ', c.last_name) AS custodian,
                   br.borrow_date, br.return_date,
                   br.purpose, br.status
            FROM BORROW_RECORD br
            JOIN USER u ON br.borrower_id = u.user_id
            JOIN USER c ON br.custodian_id = c.user_id
            ORDER BY br.borrow_date DESC
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

         printLine();
         System.out.printf("%-6s %-22s %-22s %-12s %-12s %-20s %-15s%n",
               "ID", "Borrower", "Custodian", "Borrow Date", "Return Date", "Purpose", "Status");
         printLine();

         while (rs.next()) {
            System.out.printf("%-6d %-22s %-22s %-12s %-12s %-20s %-15s%n",
                  rs.getInt("borrow_id"),
                  truncate(rs.getString("borrower"), 22),
                  truncate(rs.getString("custodian"), 22),
                  rs.getString("borrow_date"),
                  rs.getString("return_date") == null ? "Not returned" : rs.getString("return_date"),
                  truncate(rs.getString("purpose"), 20),
                  rs.getString("status"));
         }
         printLine();

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── REQUIREMENT 12: APPROVE BORROWER REQUESTS ───────────────────────────
   public static void approveBorrowRequests(Scanner sc, DataClasses.User custodian) {
      System.out.println("\n--- APPROVE BORROWER REQUESTS ---");

      String pendingSql = """
            SELECT br.request_id, br.request_date, br.purpose, br.purpose_ref,
                   u.user_id AS borrower_id,
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
                     u.user_id, u.first_name, u.last_name, u.email, u.contact_number
            ORDER BY br.request_date ASC
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(pendingSql);
            ResultSet rs = ps.executeQuery()) {

         printLine();
         System.out.printf("%-8s %-12s %-25s %-15s %-50s%n",
               "Req ID", "Date", "Borrower", "Purpose", "Requested Items");
         printLine();

         boolean hasRequests = false;
         while (rs.next()) {
            hasRequests = true;
            System.out.printf("%-8d %-12s %-25s %-15s %-50s%n",
                  rs.getInt("request_id"),
                  rs.getString("request_date"),
                  truncate(rs.getString("borrower_name"), 25),
                  truncate(rs.getString("purpose"), 15),
                  truncate(rs.getString("requested_items"), 50));
         }
         printLine();

         if (!hasRequests) {
            System.out.println("  No pending requests to approve.");
            return;
         }

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
         return;
      }

      System.out.print("\n  Enter Request ID to process: ");
      int requestId = Integer.parseInt(sc.nextLine().trim());

      System.out.println("\n  [1] Approve and Log Borrowing");
      System.out.println("  [2] Reject");
      System.out.print("  Choice: ");
      String action = sc.nextLine().trim();

      if (action.equals("1")) {
         approveAndLog(sc, custodian, requestId);
      } else if (action.equals("2")) {
         rejectRequest(sc, custodian, requestId);
      } else {
         System.out.println("  Invalid choice.");
      }
   }

   private static void approveAndLog(Scanner sc, DataClasses.User custodian, int requestId) {
      System.out.println("\n--- APPROVING REQUEST #" + requestId + " ---");

      try (Connection conn = Database.getConnection()) {

         String getRequestSql = """
               SELECT br.borrower_id, br.purpose, br.purpose_ref,
                      ri.item_id, ri.quantity
               FROM BORROW_REQUEST br
               JOIN REQUEST_ITEM ri ON br.request_id = ri.request_id
               WHERE br.request_id = ? AND br.status = 'Pending'
               """;

         PreparedStatement psGet = conn.prepareStatement(getRequestSql);
         psGet.setInt(1, requestId);
         ResultSet rs = psGet.executeQuery();

         java.util.ArrayList<Integer> itemIds = new java.util.ArrayList<>();
         java.util.ArrayList<Integer> quantities = new java.util.ArrayList<>();
         int borrowerId = 0;
         String purpose = "";
         String purposeRef = "";

         while (rs.next()) {
            borrowerId = rs.getInt("borrower_id");
            purpose = rs.getString("purpose");
            purposeRef = rs.getString("purpose_ref");
            itemIds.add(rs.getInt("item_id"));
            quantities.add(rs.getInt("quantity"));
         }

         if (borrowerId == 0) {
            System.out.println("  ✗ Request not found or already processed.");
            return;
         }

         boolean allAvailable = true;
         for (int itemId : itemIds) {
            String checkSql = "SELECT availability_status FROM ITEM WHERE item_id = ?";
            PreparedStatement psCheck = conn.prepareStatement(checkSql);
            psCheck.setInt(1, itemId);
            ResultSet rsCheck = psCheck.executeQuery();
            if (rsCheck.next() && !"Available".equals(rsCheck.getString("availability_status"))) {
               System.out.println("  ✗ Item ID " + itemId + " is no longer available.");
               allAvailable = false;
            }
         }

         if (!allAvailable) {
            System.out.println("\n  Cannot approve: some items are unavailable.");
            return;
         }

         String fullPurpose = (purposeRef != null && !purposeRef.isEmpty())
               ? purpose + ": " + purposeRef
               : purpose;
         if (fullPurpose.length() > 100)
            fullPurpose = fullPurpose.substring(0, 97) + "...";

         String remarks = "Approved from request #" + requestId;

         conn.setAutoCommit(false);

         String insertBorrow = """
               INSERT INTO BORROW_RECORD (borrower_id, custodian_id, borrow_date, purpose, status, remarks)
               VALUES (?, ?, CURDATE(), ?, 'Borrowed', ?)
               """;

         PreparedStatement psBorrow = conn.prepareStatement(insertBorrow, Statement.RETURN_GENERATED_KEYS);
         psBorrow.setInt(1, borrowerId);
         psBorrow.setInt(2, custodian.userId);
         psBorrow.setString(3, fullPurpose);
         psBorrow.setString(4, remarks);
         psBorrow.executeUpdate();

         int borrowId = 0;
         ResultSet keys = psBorrow.getGeneratedKeys();
         if (keys.next())
            borrowId = keys.getInt(1);

         for (int i = 0; i < itemIds.size(); i++) {
            int itemId = itemIds.get(i);
            int quantity = quantities.get(i);

            PreparedStatement psItem = conn.prepareStatement(
                  "INSERT INTO BORROW_ITEM (borrow_id, item_id, quantity) VALUES (?, ?, ?)");
            psItem.setInt(1, borrowId);
            psItem.setInt(2, itemId);
            psItem.setInt(3, quantity);
            psItem.executeUpdate();

            PreparedStatement psUpdate = conn.prepareStatement(
                  "UPDATE ITEM SET availability_status = 'Borrowed' WHERE item_id = ?");
            psUpdate.setInt(1, itemId);
            psUpdate.executeUpdate();
         }

         PreparedStatement psUpdateReq = conn.prepareStatement(
               "UPDATE BORROW_REQUEST SET status = 'Approved', processed_by = ?, processed_date = CURDATE() WHERE request_id = ?");
         psUpdateReq.setInt(1, custodian.userId);
         psUpdateReq.setInt(2, requestId);
         psUpdateReq.executeUpdate();

         conn.commit();

         System.out.println("\n  ✓ Request #" + requestId + " approved!");
         System.out.println("  ✓ Borrow Record #" + borrowId + " created.");
         System.out.println("  ✓ Borrowing logged successfully.");

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   private static void rejectRequest(Scanner sc, DataClasses.User custodian, int requestId) {
      System.out.print("\n  Enter reason for rejection: ");
      String reason = sc.nextLine().trim();

      String sql = """
            UPDATE BORROW_REQUEST
            SET status = 'Rejected', processed_by = ?, processed_date = CURDATE(), remarks = ?
            WHERE request_id = ?
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

         ps.setInt(1, custodian.userId);
         ps.setString(2, reason);
         ps.setInt(3, requestId);

         int rows = ps.executeUpdate();
         if (rows > 0) {
            System.out.println("  ✓ Request #" + requestId + " rejected.");
            System.out.println("  Reason: " + reason);
         } else {
            System.out.println("  ✗ Request not found.");
         }

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── SHARED HELPERS ──────────────────────────────────────────────────────
   private static void addItemsToBorrow(Scanner sc, Connection conn, int borrowId) throws SQLException {
      boolean addMore = true;
      while (addMore) {
         showAvailableItems();

         System.out.print("\n  Enter Item ID to borrow: ");
         int itemId = Integer.parseInt(sc.nextLine().trim());
         System.out.print("  Quantity: ");
         int qty = Integer.parseInt(sc.nextLine().trim());

         try (PreparedStatement ps2 = conn.prepareStatement(
               "INSERT INTO BORROW_ITEM (borrow_id, item_id, quantity) VALUES (?, ?, ?)")) {
            ps2.setInt(1, borrowId);
            ps2.setInt(2, itemId);
            ps2.setInt(3, qty);
            ps2.executeUpdate();
            System.out.println("  ✓ Item added to borrow log.");
         }

         try (PreparedStatement ps3 = conn.prepareStatement(
               "UPDATE ITEM SET availability_status = 'Borrowed' WHERE item_id = ?")) {
            ps3.setInt(1, itemId);
            ps3.executeUpdate();
         }

         System.out.print("  Add another item? (y/n): ");
         addMore = sc.nextLine().trim().equalsIgnoreCase("y");
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

   private static void showPendingActivities() {
      System.out.println("\n  --- PENDING ACTIVITIES ---");
      String sql = """
            SELECT a.activity_id, a.activity_name, a.event_type, a.event_date,
                   CONCAT(u.first_name, ' ', u.last_name) AS requester
            FROM ACTIVITY a
            JOIN USER u ON a.requester_id = u.user_id
            WHERE a.approval_status = 'Pending'
            ORDER BY a.event_date
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

         System.out.printf("  %-6s %-35s %-15s %-12s %-25s%n",
               "Act ID", "Activity Name", "Type", "Date", "Requester");
         System.out.println("  " + "─".repeat(95));

         while (rs.next()) {
            System.out.printf("  %-6d %-35s %-15s %-12s %-25s%n",
                  rs.getInt("activity_id"),
                  truncate(rs.getString("activity_name"), 35),
                  rs.getString("event_type"),
                  rs.getString("event_date"),
                  truncate(rs.getString("requester"), 25));
         }
         System.out.println("  " + "─".repeat(95));

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   private static void showAvailableItems() {
      System.out.println("\n  --- AVAILABLE ITEMS ---");
      String sql = """
            SELECT item_id, item_name, item_type, model, availability_status
            FROM ITEM
            WHERE availability_status = 'Available'
            ORDER BY item_type, item_name
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

         System.out.printf("  %-6s %-30s %-15s %-15s %-12s%n",
               "ID", "Item Name", "Type", "Model", "Status");
         System.out.println("  " + "─".repeat(80));

         boolean hasItems = false;
         while (rs.next()) {
            hasItems = true;
            System.out.printf("  %-6d %-30s %-15s %-15s %-12s%n",
                  rs.getInt("item_id"),
                  truncate(rs.getString("item_name"), 30),
                  rs.getString("item_type"),
                  rs.getString("model") == null ? "N/A" : truncate(rs.getString("model"), 15),
                  rs.getString("availability_status"));
         }
         System.out.println("  " + "─".repeat(80));
         if (!hasItems)
            System.out.println("  No available items.");

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
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
            System.out.printf("  %-5s %-30s %-14s %-12s %-5s %-15s%n",
                  "ID", "Item Name", "Barcode", "Type", "Qty", "Return Condition");
            printLine();
            while (rs.next()) {
               System.out.printf("  %-5d %-30s %-14s %-12s %-5d %-15s%n",
                     rs.getInt("borrow_item_id"),
                     truncate(rs.getString("item_name"), 30),
                     rs.getString("barcode"),
                     rs.getString("item_type"),
                     rs.getInt("quantity"),
                     rs.getString("item_condition_on_return") == null
                           ? "Not returned"
                           : rs.getString("item_condition_on_return"));
            }
            printLine();
         }
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

   // ─── PRINT HELPERS ────────────────────────────────────────────────────────
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