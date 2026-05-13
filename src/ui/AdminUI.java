package ui;

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
}