package ui;

import java.util.Scanner;

import models.DataClasses;
import service.AdminService;

public class AdminUI {

   private static final AdminService adminService = new AdminService();

   public static void menu(DataClasses.User user, Scanner sc) {
      boolean back = false;

      while (!back) {
         System.out.println("\n╔════════════════════════════════════════════════════════════╗");
         System.out.println("║                         ADMIN MENU                         ║");
         System.out.println("║  Logged in as: " + padRight(user.getFullName(), 42) + "  ║");
         System.out.println("╠════════════════════════════════════════════════════════════╣");
         System.out.println("║  [1] View Dashboard Summary                                ║");
         System.out.println("║  [2] Add Custodian Account                                 ║");
         System.out.println("║  [3] Activate / Deactivate Custodian Account               ║");
         System.out.println("║  [4] Delete User Account                                   ║");
         System.out.println("║  [5] View All Users                                        ║");
         System.out.println("║  [6] View Equipment / Inventory Status                     ║");
         System.out.println("║  [7] View Borrow Requests                                  ║");
         System.out.println("║  [8] View Borrow Records                                   ║");
         System.out.println("║  [9] View Return Records / Issues                          ║");
         System.out.println("║  [0] Logout                                                ║");
         System.out.println("╚════════════════════════════════════════════════════════════╝");
         System.out.print("  Choice: ");
         String choice = sc.nextLine().trim();

         switch (choice) {
            case "1" -> adminService.viewDashboardSummary();
            case "2" -> adminService.addCustodian(sc);
            case "3" -> adminService.setCustodianStatus(sc, user.userId);
            case "4" -> adminService.deleteUserAccount(sc, user.userId);
            case "5" -> adminService.viewAllUsers();
            case "6" -> adminService.viewInventoryStatus();
            case "7" -> adminService.viewBorrowRequests();
            case "8" -> adminService.viewBorrowRecords();
            case "9" -> adminService.viewReturnRecords();
            case "0" -> back = true;
            default -> System.out.println("  Invalid choice.");
         }
      }
   }

   private static String padRight(String s, int n) {
      return String.format("%-" + n + "s", s == null ? "" : s);
   }
}
