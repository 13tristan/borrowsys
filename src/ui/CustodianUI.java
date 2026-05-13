package ui;

import java.util.Scanner;

import models.DataClasses;
import service.CustodianService;
import ui.utils.HelperUi;

public class CustodianUI {

   private static final CustodianService custodianService = new CustodianService();

   public static void menu(DataClasses.User user, Scanner sc) {
      boolean back = false;
      while (!back) {
         System.out.println("\n╔════════════════════════════════════════════════════════════════════╗");
         System.out.println("║                         CUSTODIAN MENU                             ║");
         System.out.println("║  Logged in as: " + HelperUi.padRight(user.getFullName(), 48) + "    ║");
         System.out.println("╠════════════════════════════════════════════════════════════════════╣");
         System.out.println("║  [1] View Pending Borrow Requests                                  ║");
         System.out.println("║  [2] Create Walk-in Borrow Request                                 ║");
         System.out.println("║  [3] Process Borrow Request (Approve/Reject + Checkout)            ║");
         System.out.println("║  [4] Log Returned Items                                            ║");
         System.out.println("║  [5] Add Equipment / Accessory / Peripheral                        ║");
         System.out.println("║  [6] Update Item Condition / Availability                          ║");
         System.out.println("║  [7] View Equipment / Inventory Status                             ║");
         System.out.println("║  [8] View Borrow Records                                           ║");
         System.out.println("║  [9] View Unreturned Items / Returns with Issues                   ║");
         System.out.println("║  [0] Logout                                                        ║");
         System.out.println("╚════════════════════════════════════════════════════════════════════╝");
         System.out.print("  Choice: ");
         String choice = sc.nextLine().trim();

         switch (choice) {
            case "1" -> custodianService.viewPendingRequests();
            case "2" -> custodianService.createWalkInBorrowRequest(user, sc);
            case "3" -> custodianService.processBorrowRequest(user, sc);
            case "4" -> custodianService.logReturnedItems(user, sc);
            case "5" -> custodianService.addNewItem(sc);
            case "6" -> custodianService.updateItemConditionStatus(sc);
            case "7" -> custodianService.viewEquipmentStatus();
            case "8" -> custodianService.viewBorrowRecords();
            case "9" -> custodianService.viewUnreturnedAndIssues();
            case "0" -> back = true;
            default -> System.out.println("  Invalid choice.");
         }
      }
   }
}
