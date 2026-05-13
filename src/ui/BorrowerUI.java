package ui;

import java.util.Scanner;

import models.DataClasses;
import service.BorrowerService;

public class BorrowerUI {

   private static final BorrowerService borrowerService = new BorrowerService();

   public static void menu(DataClasses.User user, Scanner sc) {
      boolean back = false;
      System.out.println("\n  Welcome, " + user.getFullName());

      while (!back) {
         System.out.println("\n╔════════════════════════════════════════════════════╗");
         System.out.println("║                  BORROWER MENU                     ║");
         System.out.println("╠════════════════════════════════════════════════════╣");
         System.out.println("║  [1] View Available Items                          ║");
         System.out.println("║  [2] Create Borrow Request                         ║");
         System.out.println("║  [3] View My Requests                              ║");
         System.out.println("║  [4] Cancel Pending Request                        ║");
         System.out.println("║  [5] View My Active Borrowed Items                 ║");
         System.out.println("║  [6] View My Borrow History                        ║");
         System.out.println("║  [7] Update My Account Information                 ║");
         System.out.println("║  [0] Logout                                        ║");
         System.out.println("╚════════════════════════════════════════════════════╝");
         System.out.print("  Choice: ");
         String choice = sc.nextLine().trim();

         switch (choice) {
            case "1" -> borrowerService.viewAvailableItems();
            case "2" -> borrowerService.createBorrowRequest(user, sc);
            case "3" -> borrowerService.viewMyRequests(user);
            case "4" -> borrowerService.cancelRequest(user, sc);
            case "5" -> borrowerService.viewActiveBorrowedItems(user);
            case "6" -> borrowerService.viewBorrowHistory(user);
            case "7" -> borrowerService.updateAccountInfo(user, sc);
            case "0" -> back = true;
            default -> System.out.println("  Invalid choice.");
         }
      }
   }
}
