import java.sql.Connection;
import java.util.Scanner;

import dao.Database;
import models.DataClasses;
import ui.AdminUI;
import ui.BorrowerUI;
import ui.CustodianUI;

public class Main {

   public static void main(String[] args) {

      Scanner sc = new Scanner(System.in);
      Connection conn = null;

      printBanner();
      try {
         System.out.println("Connecting to database...");
         conn = Database.getConnection();
         System.out.println("Database connection successful!");
      } catch (Exception e) {
         System.out.println("An unexpected error occurred: " + e.getMessage());

      }

      boolean running = true;
      while (running) {

         if (conn == null) {
            System.out.println("Database connection is not available");
            running = false;
            break;

         }
         System.out.println("\n╔══════════════════════════════════════╗");
         System.out.println("║         MAIN MENU                    ║");
         System.out.println("╠══════════════════════════════════════╣");
         System.out.println("║  [1] Login                           ║");
         System.out.println("║  [2] Register                        ║");
         System.out.println("║  [0] Exit                            ║");
         System.out.println("╚══════════════════════════════════════╝");
         System.out.print("  Choice: ");
         String choice = sc.nextLine().trim();

         switch (choice) {

            case "1" -> {
               DataClasses.User user = Auth.login(sc);
               if (user != null) {
                  routeToMenu(user, sc);
               }
            }

            case "2" -> Auth.register(sc);

            case "0" -> {
               System.out.println("\n  Goodbye! Exiting the system...");
               running = false;
            }

            default -> System.out.println("  Invalid choice. Please try again.");
         }
      }

      sc.close();
   }

   private static void routeToMenu(DataClasses.User user, Scanner sc) {
        switch (user.userType) {
            case "Custodian"   -> CustodianUI.menu(user, sc);
            case "Admin"       -> AdminUI.menu(user, sc);
            case "Student",
                 "Instructor",
                 "Staff"       -> BorrowerUI.menu(user, sc);
            default            -> System.out.println("  Unknown user type. Access denied.");
        }

        System.out.println("\n  You have been logged out. Returning to main menu...");
    }

   private static void printBanner() {
      System.out.println();
      System.out.println("  ╔════════════════════════════════════════════════════════╗");
      System.out.println("  ║      FACILITY / EQUIPMENT BORROWING SYSTEM             ║");
      System.out.println("  ║      CIS Department - Saint Louis University           ║");
      System.out.println("  ║      CIS 221 - Information Management                  ║");
      System.out.println("  ╚════════════════════════════════════════════════════════╝");
   }
}
