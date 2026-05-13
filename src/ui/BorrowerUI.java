package ui;

import java.util.Scanner;
<<<<<<< HEAD

import models.DataClasses;
import service.BorrowerService;

public class BorrowerUI {

   private static final BorrowerService borrowerService = new BorrowerService();

   // ─── MENU ────────────────────────────────────────────────────
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
         System.out.println("║  [4] Create Borrow Request                   ║");
         System.out.println("║  [5] Update Borrowed Item Condition          ║");
         System.out.println("║  [6] Update Account Information              ║");
         System.out.println("║  [7] Cancel Borrow Request                   ║");
         System.out.println("║  [0] Logout                                  ║");
         System.out.println("╚══════════════════════════════════════════════╝");

         System.out.print("  Choice: ");

         String choice = sc.nextLine().trim();

         switch (choice) {

            case "1" -> borrowerService.viewAvailableItems();

            case "2" -> borrowerService.viewBorrowHistory(user);

            case "3" -> borrowerService.viewPendingRequests(user);

            case "4" -> borrowerService.createBorrowRequest(user, sc);

            case "5" -> borrowerService.updateBorrowedItem(sc);

            case "6" -> borrowerService.updateAccountInfo(user, sc);

            case "7" -> borrowerService.cancelRequest(user, sc);

            case "0" -> back = true;

            default -> System.out.println("  Invalid choice.");
         }
      }
   }
=======
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
>>>>>>> 5982ff0ffa56f3b6bce2206614edf924ba960533
}