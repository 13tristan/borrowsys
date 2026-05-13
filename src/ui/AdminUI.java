package ui;
<<<<<<< HEAD

import java.util.Scanner;

import models.DataClasses;
import service.AdminService;

public class AdminUI {

   private static final AdminService adminService = new AdminService();

   // ─── MAIN MENU ────────────────────────────────────
   public static void menu(DataClasses.User user, Scanner sc) {

      boolean back = false;

      while (!back) {

         System.out.println("\n╔══════════════════════════════════════════════╗");
         System.out.println("║                 ADMIN MENU                   ║");
         System.out.println("║  Logged in as: " +
               padRight(user.getFullName(), 30) + "║");
         System.out.println("║ " + padRight(user.userType, 30) + "              ║");
         System.out.println("╠══════════════════════════════════════════════╣");
         System.out.println("║  [1] Add User                                ║");
         System.out.println("║  [2] View All Users                          ║");
         System.out.println("║  [3] Update User                             ║");
         System.out.println("║  [4] Delete User                             ║");
         System.out.println("║  [0] Logout                                  ║");
         System.out.println("╚══════════════════════════════════════════════╝");

         System.out.print("  Choice: ");
         String choice = sc.nextLine().trim();

         switch (choice) {

            case "1" -> adminService.addUser(sc);

            case "2" -> adminService.viewAllUsers();

            case "3" -> adminService.updateUser(sc);

            case "4" -> adminService.deleteUser(sc);

            case "0" -> back = true;

            default -> System.out.println("  Invalid choice.");
         }
      }
   }

   // ─── HELPER ───────────────────────────────────────
   private static String padRight(String s, int n) {
      return String.format("%-" + n + "s", s);
   }
=======
import java.util.Scanner;
import models.DataClasses;
import service.AdminService;
import ui.utils.HelperUi;

public class AdminUI {

    // ─── MENU ────────────────────────────────────────────────────────────────
    public static void menu(DataClasses.User user, Scanner sc) {
        boolean back = false;
        while (!back) {
            System.out.println("\n╔══════════════════════════════════════════════╗");
            System.out.println("║          ADMIN MENU                          ║");
            System.out.println("║  Logged in as: " + HelperUi.padRight(user.getFullName(), 30) + "║");
            System.out.println("╠══════════════════════════════════════════════╣");
            System.out.println("║  [1] View All User Accounts                  ║");
            System.out.println("║  [2] View Custodian Accounts                 ║");
            System.out.println("║  [3] View Equipment Status                   ║");
            System.out.println("║  [4] View All Borrow Records                 ║");
            System.out.println("║  [5] View All Activities / Requests          ║");
            System.out.println("║  [6] View Return Records                     ║");
            System.out.println("║  [0] Logout                                  ║");
            System.out.println("╚══════════════════════════════════════════════╝");
            System.out.print("  Choice: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> AdminService.viewAllUsers();
                case "2" -> AdminService.viewCustodianAccounts();
                case "3" -> AdminService.viewEquipmentStatus();
                case "4" -> AdminService.viewAllBorrowRecords();
                case "5" -> AdminService.viewActivitiesAndRequests();
                case "6" -> AdminService.viewAllReturnRecords();
                case "0" -> back = true;
                default -> System.out.println("  Invalid choice.");
            }
        }
    }
>>>>>>> 5982ff0ffa56f3b6bce2206614edf924ba960533
}