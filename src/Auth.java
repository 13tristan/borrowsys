import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

import dao.Database;
import models.DataClasses;

public class Auth {

   public static DataClasses.User login(Scanner sc) {
      System.out.println("\n╔══════════════════════════════════════╗");
      System.out.println("║              LOGIN                   ║");
      System.out.println("╚══════════════════════════════════════╝");

      System.out.print("  Email   : ");
      String email = sc.nextLine().trim();
      System.out.print("  Password: ");
      String password = sc.nextLine().trim();

      String sql = """
            SELECT user_id, first_name, last_name, email,
                   contact_number, user_type, department, account_status
            FROM USER
            WHERE email = ?
              AND password = ?
              AND account_status = 'Active'
            """;

      try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

         ps.setString(1, email);
         ps.setString(2, password);

         try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
               DataClasses.User user = new DataClasses.User();
               user.userId = rs.getInt("user_id");
               user.firstName = rs.getString("first_name");
               user.lastName = rs.getString("last_name");
               user.email = rs.getString("email");
               user.contactNumber = rs.getString("contact_number");
               user.userType = rs.getString("user_type");
               user.department = rs.getString("department");
               user.accountStatus = rs.getString("account_status");
               return user;
            } else {
               System.out.println("\n  ✘ Invalid email or password, or account is inactive.");
               return null;
            }
         }

      } catch (SQLException e) {
         System.out.println("  [DB ERROR] " + e.getMessage());
         return null;
      }
   }

}