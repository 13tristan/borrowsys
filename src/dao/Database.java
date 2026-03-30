package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final String URL = "jdbc:mysql://localhost:3306/borrowsys_db";
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
}
