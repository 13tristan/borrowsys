package ui;

import java.util.Scanner;

import models.DataClasses;
import service.CustodianService;
import ui.utils.HelperUi;

public class CustodianUI {

   public static void menu(DataClasses.User user, Scanner sc) {
      boolean back = false;
      while (!back) {
         System.out.println("\n╔════════════════════════════════════════════════════════════════════╗");
         System.out.println("║                         CUSTODIAN MENU                           ║");
         System.out.println("║  Logged in as: " + HelperUi.padRight(user.getFullName(), 48) + "║");
         System.out.println("╠════════════════════════════════════════════════════════════════════╣");
         System.out.println("║  [1]  View Pending Borrower Requests                             ║");
         System.out.println("║  [2]  Process Borrow Request (Approve/Reject + Checkout)         ║");
         System.out.println("║  [3]  Log Returned Items                                         ║");
         System.out.println("║  [4]  Add Equipment / Accessory / Peripheral                     ║");
         System.out.println("║  [5]  Update Item Details / Status                               ║");
         System.out.println("║  [6]  Delete Item from Inventory                                 ║");
         System.out.println("║  [7]  Create Activity / Event Request Record                     ║");
         System.out.println("║  [8]  Mark Borrow Record as Overdue                              ║");
         System.out.println("║  [9]  Sync Item Availability with Active Borrow Records          ║");
         System.out.println("║  [10] View Item Availability Summary (Statement Demo)            ║");
         System.out.println("║  [11] View All Items / Equipment Status                          ║");
         System.out.println("║  [12] View All Borrow Records                                    ║");
         System.out.println("║  [13] View Borrowed Items (by Class or Event)                    ║");
         System.out.println("║  [14] View Borrow Status (by Class / Event / All)                ║");
         System.out.println("║  [15] View Unreturned Items / Returns with Issues                ║");
         System.out.println("║  [16] Count Items in a Borrow Record (Stored Function Demo)      ║");
         System.out.println("║  [0]  Logout                                                     ║");
         System.out.println("╚════════════════════════════════════════════════════════════════════╝");
         System.out.print("  Choice: ");
         String choice = sc.nextLine().trim();
         switch (choice) {
            case "1" -> CustodianService.viewPendingRequests();
            case "2" -> CustodianService.processBorrowRequest(user, sc);
            case "3" -> CustodianService.logReturnedItems(user, sc);
            case "4" -> CustodianService.addNewItem(sc);
            case "5" -> CustodianService.updateItemDetails(sc);
            case "6" -> CustodianService.deleteItemFromInventory(sc);
            case "7" -> CustodianService.createActivityEvent(user, sc);
            case "8" -> CustodianService.markBorrowRecordOverdue(sc);
            case "9" -> CustodianService.syncItemAvailability();
            case "10" -> CustodianService.viewItemAvailabilitySummary();
            case "11" -> CustodianService.viewAllItems();
            case "12" -> CustodianService.viewAllBorrowRecords();
            case "13" -> CustodianService.getBorrowedItemsByClassOrEvent(sc);
            case "14" -> CustodianService.viewBorrowStatus(sc);
            case "15" -> CustodianService.viewUnreturnedAndIssues();
            case "16" -> CustodianService.countItemsInBorrowRecord(sc);
            case "0" -> back = true;
            default -> System.out.println("  Invalid choice.");
         }
      }
   }
}
