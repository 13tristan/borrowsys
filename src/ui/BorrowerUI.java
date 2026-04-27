package ui;

import java.util.Scanner;
import models.DataClasses;
import service.BorrowerService;
import ui.utils.HelperUi;

public class BorrowerUI {

    // ─── MENU ────────────────────────────────────────────────────────────────
    public static void menu(DataClasses.User user, Scanner sc) {
        boolean back = false;

        System.out.println("\n  Welcome, " + user.getFullName());
        while (!back) {
            System.out.println("\n╔══════════════════════════════════════════════╗");
            System.out.println("║          BORROWER MENU                       ║");
            System.out.println("║  Logged in as: " + HelperUi.padRight(user.getFullName(), 30) + "║");
            System.out.println("╠══════════════════════════════════════════════╣");
            System.out.println("║  [1] View Available Items                    ║");
            System.out.println("║  [2] View My Borrow History                  ║");
            System.out.println("║  [3] View My Pending Requests                ║");
            System.out.println("║  [4] Update Account                          ║");
            System.out.println("║  [0] Logout                                  ║");
            System.out.println("╚══════════════════════════════════════════════╝");
            System.out.print("  Choice: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> BorrowerService.viewAvailableItems();
                case "2" -> BorrowerService.viewBorrowHistory(user);
                case "3" -> BorrowerService.viewPendingRequests(user);
                case "0" -> back = true;
                default -> System.out.println("  Invalid choice.");
            }
        }
    }
}