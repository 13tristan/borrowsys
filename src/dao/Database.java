package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mysql.cj.protocol.Resultset;

public class Database {
    private static final String URL = "jdbc:mysql://localhost:3306/borrow_sys";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection conn = null;

    public static Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            try {
                conn = DriverManager.getConnection(URL, USER, PASSWORD);

                }
            catch (SQLException e) {
                throw new SQLException("Failed to connect to the database.", e);
            }
        }return conn;

    }

    public static void close() {
        try {
            if (conn != null)
                conn.close();
        } catch (SQLException ignored) {
        }
    }

    public static void main(String[] args) {
        try {
            getConnection();
            System.out.println("Connection successful!");


            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM user");
            stmt.executeQuery();

            ResultSet rs = stmt.getResultSet();
                
            while (rs.next()) {
            int id = rs.getInt("user_id");
            String fname = rs.getString("first_name");
            String lname = rs.getString("last_name");
            String email = rs.getString("email");
            System.out.println("ID: " + id + ", Name: " + fname + " " + lname + ", Email: " + email);
            }
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        } finally {
            close();
        }
    }
}
