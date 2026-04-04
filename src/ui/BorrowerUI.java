package ui;

import java.sql.*;
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
            System.out.println("║  [1] View My Borrow History                  ║");
            System.out.println("║  [2] View Available Items                    ║");
            System.out.println("║  [0] Logout                                  ║");
            System.out.println("╚══════════════════════════════════════════════╝");
            System.out.print("  Choice: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> viewBorrowHistory(user);
                case "2" -> viewAvailableItems();
                case "0" -> back = true;
                default  -> System.out.println("  Invalid choice.");
            }
        }
    }

    // ─── UI-B1: VIEW MY BORROW HISTORY ───────────────────────────────────────
    public static void viewBorrowHistory(DataClasses.User user) {
        System.out.println("\n--- MY BORROW HISTORY ---");
        System.out.println("  User: " + user.getFullName() + " (" + user.email + ")");

        String sql = """
            SELECT br.borrow_id,
                   CONCAT(c.first_name, ' ', c.last_name) AS custodian,
                   br.borrow_date, br.return_date,
                   br.purpose, br.status, br.remarks
            FROM BORROW_RECORD br
            JOIN USER c ON br.custodian_id = c.user_id
            WHERE br.borrower_id = ?
            ORDER BY br.borrow_date DESC
            """;

        String itemSql = """
            SELECT i.item_name, i.barcode, i.item_type,
                   bi.quantity, bi.item_condition_on_return
            FROM BORROW_ITEM bi
            JOIN ITEM i ON bi.item_id = i.item_id
            WHERE bi.borrow_id = ?
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, user.userId);

            try (ResultSet rs = ps.executeQuery()) {
                printLine();

                int count = 0;
                boolean hasRecord = false;

                while (rs.next()) {
                    hasRecord = true;
                    System.out.printf("  Borrow #%-4d | Custodian: %-20s | Purpose: %-8s | Status: %-30s%n",
                        rs.getInt("borrow_id"),
                        rs.getString("custodian"),
                        rs.getString("purpose"),
                        rs.getString("status"));
                    System.out.printf("               Borrowed: %-20s | Returned: %s%n",
                        rs.getString("borrow_date"),
                        rs.getString("return_date") == null
                            ? "Not yet returned" : rs.getString("return_date"));

                    if (rs.getString("remarks") != null) {
                        System.out.println("               Remarks : " + rs.getString("remarks"));
                    }

                    // Show items for each borrow record
                    try (PreparedStatement itemPs = conn.prepareStatement(itemSql)) {
                        itemPs.setInt(1, rs.getInt("borrow_id"));
                        try (ResultSet itemRs = itemPs.executeQuery()) {
                            System.out.println("Items Borrowed:");
                            int itemIndex = 1;

                            while (itemRs.next()) {

                                System.out.printf("[" + (itemIndex++) + "] %s | Barcode: %-12s | Qty: %d | Return Cond: %s%n",
                                    itemRs.getString("item_name"),
                                    itemRs.getString("barcode"),
                                    itemRs.getInt("quantity"),
                                    itemRs.getString("item_condition_on_return") == null
                                        ? "N/A" : itemRs.getString("item_condition_on_return"));
                            }
                        }
                    }
                    printLine();
                    count++;
                }

                if (!hasRecord) {
                    System.out.println("  No borrow records found.");
                    printLine();
                } else {
                    System.out.println("  Total borrow records: " + count);
                }
            }

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    // ─── UI-B2: VIEW AVAILABLE ITEMS ─────────────────────────────────────────
    public static void viewAvailableItems() {
        System.out.println("\n--- AVAILABLE ITEMS ---");
        String sql = """
            SELECT item_id, barcode, item_name, item_type,
                   model, tag, condition_status
            FROM ITEM
            WHERE availability_status = 'Available'
              AND condition_status != 'Under Maintenance'
            ORDER BY item_type, item_name
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printLine();
            System.out.printf("  %-5s %-14s %-25s %-11s %-20s %-10s %-18s%n",
                "ID", "Barcode", "Item Name", "Type", "Model", "Tag", "Condition");
            printLine();

            int count = 0;
            while (rs.next()) {
                System.out.printf("  %-5d %-14s %-25s %-11s %-20s %-10s %-18s%n",
                    rs.getInt("item_id"),
                    rs.getString("barcode"),
                    rs.getString("item_name"),
                    rs.getString("item_type"),
                    rs.getString("model") == null ? "N/A" : rs.getString("model"),
                    rs.getString("tag")   == null ? "N/A" : rs.getString("tag"),
                    rs.getString("condition_status"));
                count++;
            }
            printLine();
            System.out.println("  Total available items: " + count);

        } catch (SQLException e) {
            System.out.println("  [DB ERROR] " + e.getMessage());
        }
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────
    private static void printLine() {
        System.out.println("  " + "─".repeat(130));
    }
}