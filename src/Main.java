import java.sql.Connection;
import java.util.Scanner;

import dao.Database;
import models.DataClasses;
import service.AuthService;
import ui.AdminUI;
import ui.BorrowerUI;
import ui.CustodianUI;

/**
 * Entry point of the Facility / Equipment Borrowing System.
 *
 * Responsibilities of this class:
 * 1. Start the program.
 * 2. Test the database connection.
 * 3. Show the main Login/Register menu.
 * 4. Route users to the correct UI based on their user_type.
 */
public class Main {

   public static void main(String[] args) {

      // One Scanner is shared across menus to read user input from the console.
      Scanner sc = new Scanner(System.in);
      // conn is used only to verify that the database is reachable before showing menus.
      Connection conn = null;

      printBanner();
      try {
         System.out.println("Connecting to database...");
         // If this succeeds, the application can safely continue using the database.
         conn = Database.getConnection();
         System.out.println("Database connection successful!");
      } catch (Exception e) {
         System.out.println("An unexpected error occurred: " + e.getMessage());

      }

      // Main loop keeps the application running until the user selects Exit.
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
               // AuthService returns a User object only when credentials are valid and active.
               DataClasses.User user = AuthService.login(sc);
               if (user != null) {
                  // The logged-in user is sent to the menu allowed for their role.
                  routeToMenu(user, sc);
               }
            }
            case "2" -> {
               AuthService.register(sc);
            }
            case "0" -> {
               System.out.println("\n  Goodbye! Exiting the system...");
               running = false;
            }

            default -> System.out.println("  Invalid choice. Please try again.");
         }
      }

      sc.close();
   }

   /**
    * Sends the logged-in user to the correct menu.
    * The database column user_type controls the role-based access.
    */
   private static void routeToMenu(DataClasses.User user, Scanner sc) {
      switch (user.userType) {
         case "Custodian" -> CustodianUI.menu(user, sc);
         case "Admin" -> AdminUI.menu(user, sc);
         case "Student",
              "Instructor",
              "Staff" ->
                 BorrowerUI.menu(user, sc);
         default -> System.out.println("  Unknown user type. Access denied.");
      }

      System.out.println("\n  You have been logged out. Returning to main menu...");
   }

   // Prints the title banner shown when the program starts.
   private static void printBanner() {
      System.out.println();
      System.out.println("  ╔════════════════════════════════════════════════════════╗");
      System.out.println("  ║      FACILITY / EQUIPMENT BORROWING SYSTEM             ║");
      System.out.println("  ║      CIS Department - Saint Louis University           ║");
      System.out.println("  ║      CIS 221 - Information Management                  ║");
      System.out.println("  ╚════════════════════════════════════════════════════════╝");
   }
}
