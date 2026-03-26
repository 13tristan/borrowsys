import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
   public static void main(String[] args) {
      String url = "jdbc:mysql://127.0.0.1:3306/movies"; // JDBC URL;
      String user = "root"; // USERNAME
      String password = ""; // PASSWORD

      try (Connection conn = DriverManager.getConnection(url, user, password)) {
         System.out.println("✅ Connected to MySQL successfully!");
      } catch (SQLException e) {
         System.out.println("❌ Connection failed: " + e.getMessage());
      }
   }
}