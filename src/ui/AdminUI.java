package ui;
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
}