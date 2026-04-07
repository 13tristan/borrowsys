package ui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

import dao.Database;
import models.DataClasses;

public class BorrowerUI {

   // ─── MENU ────────────────────────────────────────────────────────────────
   public static void menu(DataClasses.User user, Scanner sc) {
      boolean back = false;

      System.out.println("\n  Welcome, " + user.getFullName());
      while (!back) {
         System.out.println("\n╔══════════════════════════════════════════════╗");
         System.out.println("║          BORROWER MENU                       ║");
         System.out.println("╠══════════════════════════════════════════════╣");
         System.out.println("║  [1] View Available Items                    ║");
         System.out.println("║  [2] View My Borrow History                  ║");
         System.out.println("║  [3] View My Pending Requests                ║");
         System.out.println("║  [0] Logout                                  ║");
         System.out.println("╚══════════════════════════════════════════════╝");
         System.out.print("  Choice: ");
         String choice = sc.nextLine().trim();
         switch (choice) {
            case "1" -> viewAvailableItems();
            case "2" -> viewBorrowHistory(user);
            case "3" -> viewPendingRequests(user);
            case "0" -> back = true;
            default -> System.out.println("  Invalid choice.");
         }
      }
   }

   // ─── UI-B2: VIEW AVAILABLE ITEMS ─────────────────
   public static void viewAvailableItems() {
      System.out.println("\n--- AVAILABLE ITEMS (Borrow by Item ID) ---");

      String sql = """
            SELECT DISTINCT i.item_id, i.barcode, i.item_name, i.item_type,
                   i.model, i.tag, i.condition_status, i.availability_status
            FROM ITEM i
            WHERE i.availability_status = 'Available'
              AND i.condition_status NOT IN ('Damaged', 'Under Maintenance')
              AND NOT EXISTS (
                  SELECT 1
                  FROM BORROW_ITEM bi
                  JOIN BORROW_RECORD br ON bi.borrow_id = br.borrow_id
                  WHERE bi.item_id = i.item_id
                    AND br.status IN ('Borrowed', 'Overdue')
              )
            ORDER BY i.item_type, i.item_name
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

         printLine();
         System.out.printf("  %-6s %-14s %-30s %-15s %-15s %-10s %-15s%n",
               "Item ID", "Barcode", "Item Name", "Type", "Model", "Condition", "Status");
         printLine();

         int count = 0;
         while (rs.next()) {
            System.out.printf("  %-6d %-14s %-30s %-15s %-15s %-10s %-15s%n",
                  rs.getInt("item_id"),
                  rs.getString("barcode"),
                  truncate(rs.getString("item_name"), 30),
                  rs.getString("item_type"),
                  rs.getString("model") == null ? "N/A" : truncate(rs.getString("model"), 15),
                  rs.getString("condition_status"),
                  rs.getString("availability_status"));
            count++;
         }
         printLine();
         System.out.println("  Total available items: " + count);
         System.out.println("\n  To borrow, enter the exact Item ID shown above.");

         if (count == 0) {
            System.out.println("\n  No items available for borrowing at this time.");
         }

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── UI-B3: VIEW MY BORROW HISTORY ────────────────────────────
   public static void viewBorrowHistory(DataClasses.User user) {
      System.out.println("\n--- MY BORROW HISTORY ---");

      String sql = """
            SELECT br.borrow_id, br.borrow_date, br.return_date,
                   br.purpose, br.status,
                   GROUP_CONCAT(DISTINCT i.item_name SEPARATOR ', ') AS items,
                   GROUP_CONCAT(DISTINCT i.item_id SEPARATOR ', ') AS item_ids
            FROM BORROW_RECORD br
            LEFT JOIN BORROW_ITEM bi ON br.borrow_id = bi.borrow_id
            LEFT JOIN ITEM i ON bi.item_id = i.item_id
            WHERE br.borrower_id = ?
            GROUP BY br.borrow_id, br.borrow_date, br.return_date, br.purpose, br.status
            ORDER BY br.borrow_date DESC
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

         ps.setInt(1, user.userId);

         try (ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-8s %-12s %-12s %-12s %-12s %-40s%n",
                  "Borr ID", "Borrow Date", "Return Date", "Purpose", "Status", "Items Borrowed");
            printLine();

            int count = 0;
            while (rs.next()) {
               String returnDate = rs.getString("return_date");
               if (returnDate == null)
                  returnDate = "Not returned";

               String items = rs.getString("items");
               if (items == null)
                  items = "No items";

               String itemIds = rs.getString("item_ids");
               if (itemIds != null && !itemIds.isEmpty()) {
                  items = items + " (IDs: " + itemIds + ")";
               }

               System.out.printf("%-8d %-12s %-12s %-12s %-12s %-40s%n",
                     rs.getInt("borrow_id"),
                     rs.getString("borrow_date"),
                     returnDate,
                     truncate(rs.getString("purpose"), 12),
                     rs.getString("status"),
                     items.length() > 40 ? items.substring(0, 37) + "..." : items);
               count++;
            }
            printLine();

            if (count == 0) {
               System.out.println("\n  You have no borrow history yet.");
            } else {
               System.out.println("  Total records: " + count);
            }
         }

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── UI-B4: VIEW PENDING REQUESTS ────────────────────────────────────────
   public static void viewPendingRequests(DataClasses.User user) {
      System.out.println("\n--- MY PENDING BORROW REQUESTS ---");

      String sql = """
            SELECT br.request_id, br.request_date, br.purpose, br.purpose_ref, br.status,
                   GROUP_CONCAT(CONCAT(i.item_name, ' (x', ri.quantity, ')')
                                SEPARATOR ', ') AS items
            FROM BORROW_REQUEST br
            LEFT JOIN REQUEST_ITEM ri ON br.request_id = ri.request_id
            LEFT JOIN ITEM i ON ri.item_id = i.item_id
            WHERE br.borrower_id = ? AND br.status = 'Pending'
            GROUP BY br.request_id, br.request_date, br.purpose, br.purpose_ref, br.status
            ORDER BY br.request_date DESC
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

         ps.setInt(1, user.userId);

         try (ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("%-8s %-12s %-12s %-20s %-50s%n",
                  "Req ID", "Request Date", "Purpose", "Status", "Items Requested");
            printLine();

            int count = 0;
            while (rs.next()) {
               String items = rs.getString("items");
               if (items == null)
                  items = "No items";

               System.out.printf("%-8d %-12s %-12s %-20s %-50s%n",
                     rs.getInt("request_id"),
                     rs.getString("request_date"),
                     truncate(rs.getString("purpose"), 12),
                     rs.getString("status"),
                     items.length() > 50 ? items.substring(0, 47) + "..." : items);
               count++;
            }
            printLine();

            if (count == 0) {
               System.out.println("\n  You have no pending requests.");
            } else {
               System.out.println("  Total pending requests: " + count);
               System.out.println("\n  Note: Wait for custodian approval. Once approved,");
               System.out.println("        the custodian will check out the items for you.");
            }
         }

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
      }
   }

   // ─── HELPER METHODS ──────────────────────────────────────────────────────
   private static void printLine() {
      System.out.println("  " + "─".repeat(140));
   }

   private static String truncate(String s, int maxLen) {
      if (s == null)
         return "N/A";
      if (s.length() <= maxLen)
         return s;
      return s.substring(0, maxLen - 3) + "...";
   }
}