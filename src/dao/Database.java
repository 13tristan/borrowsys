package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central database helper class.
 *
 * This class contains the MySQL connection settings and returns a shared
 * JDBC Connection object to the rest of the program. Keeping the connection
 * code here prevents every UI/service class from repeating the same
 * DriverManager logic.
 */
public class Database {
   // JDBC URL: connects Java to the MySQL database named 221borrowapp on localhost.
   private static final String URL = "jdbc:mysql://localhost:3306/221app";
   // Default local MySQL credentials used in XAMPP/phpMyAdmin setups.
   private static final String USER = "root";
   private static final String PASSWORD = "";

   // Shared connection reference. It is reused while the program is running.
   private static Connection conn = null;

   /**
    * Returns an active database connection.
    * If the connection is still null or was already closed, a new connection
    * is created using DriverManager.
    */
   public static Connection getConnection() throws SQLException {
      if (conn == null || conn.isClosed()) {
         try {
            // DriverManager establishes the actual connection to MySQL.
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

         } catch (SQLException e) {
             e.printStackTrace();
            throw new SQLException("Failed to connect to the database.", e);
         }
      }
      return conn;

   }

   /**
    * Closes the shared connection when the application no longer needs it.
    */
   public static void close() {
      try {
         if (conn != null)
            conn.close();
      } catch (SQLException ignored) {
      }
   }
}
