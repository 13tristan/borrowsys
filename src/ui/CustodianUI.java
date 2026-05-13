package ui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

import dao.Database;
import models.DataClasses;
import service.CustodianService;
import ui.utils.HelperUi;

public class CustodianUI {

    public static void menu(DataClasses.User user, Scanner sc) {
        boolean back = false;
        while (!back) {
            System.out.println("\n╔════════════════════════════════════════════════════════════════════╗");
            System.out.println("║                         CUSTODIAN MENU                             ║");
            System.out.println("║  Logged in as: " + HelperUi.padRight(user.getFullName(), 48) + "║");
            System.out.println("╠════════════════════════════════════════════════════════════════════╣");
            System.out.println("║  [1]  View Student Data (Currently Enrolled)                       ║");
            System.out.println("║  [2]  View CIS Staff and Faculty                                   ║");
            System.out.println("║  [3]  View Laboratory Classes (with students)                      ║");
            System.out.println("║  [4]  View Borrowed Items (by Class or Event)                      ║");
            System.out.println("║  [5]  View Borrow Status (by Class or Event)                       ║");
            System.out.println("║  [6]  View Borrowers with Unreturned Items / Returns with Issues   ║");
            System.out.println("║  [7]  View All Items / Equipment Status                            ║");
            System.out.println("║  [8]  View All Borrow Records                                      ║");
            System.out.println("║  [9]  View Pending Borrower Requests                               ║");
            System.out.println("║  [0]  Logout                                                       ║");
            System.out.println("╚════════════════════════════════════════════════════════════════════╝");
            System.out.print("  Choice: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> CustodianService.extractStudents();
                case "2" -> CustodianService.viewAllStaffFaculty();
                case "3" -> CustodianService.extractLabClasses(sc);
                case "4" -> CustodianService.getBorrowedItemsByClassOrEvent(sc);
                case "5" -> CustodianService.viewBorrowStatus(sc);
                case "6" -> CustodianService.viewUnreturnedAndIssues();
                case "7" -> CustodianService.viewAllItems();
                case "8" -> CustodianService.viewAllBorrowRecords();
                case "9" -> CustodianService.viewPendingRequests();
                case "0" -> back = true;
                default -> System.out.println("  Invalid choice.");
            }
        }
    }
}