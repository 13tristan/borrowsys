package dao;

import models.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Queries {

    public static List<User> getAllUsers() throws SQLException {
        return queryUsers("SELECT * FROM user ORDER BY last_name", null);
    }

    public static List<User> getUsersByType(String type) throws SQLException {
        return queryUsers("SELECT * FROM user WHERE type = ? ORDER BY last_name", type);
    }

    private static List<User> queryUsers(String sql, String param) throws SQLException {
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = prepare(sql, param);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new User(
                    rs.getInt("user_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("contact_number"),
                    rs.getString("type"),
                    rs.getString("department")
                ));
            }
        }
        return list;
    }

    public static List<Item> getAllItems() throws SQLException {
        return queryItems("SELECT * FROM item ORDER BY item_name", null);
    }

    public static List<Item> getItemsByAvailability(String status) throws SQLException {
        return queryItems(
            "SELECT * FROM item WHERE availability_status = ? ORDER BY item_name", status);
    }

    public static List<Item> getItemsByCondition(String condition) throws SQLException {
        return queryItems(
            "SELECT * FROM item WHERE condition_status = ? ORDER BY item_name", condition);
    }

    private static List<Item> queryItems(String sql, String param) throws SQLException {
        List<Item> list = new ArrayList<>();
        try (PreparedStatement ps = prepare(sql, param);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Item(
                    rs.getInt("item_id"),
                    rs.getString("item_name"),
                    rs.getString("item_type"),
                    rs.getString("description"),
                    rs.getString("model"),
                    rs.getString("tag"),
                    rs.getString("condition_status"),
                    rs.getString("availability_status"),
                    rs.getDate("date_acquired")
                ));
            }
        }
        return list;
    }


    private static final String BORROW_BASE =
        "SELECT br.borrow_id, " +
        "CONCAT(b.first_name,' ',b.last_name) AS borrower_name, " +
        "CONCAT(c.first_name,' ',c.last_name) AS custodian_name, " +
        "br.borrowed_date, br.purpose, br.return_date, br.status, br.remarks " +
        "FROM borrow_record br " +
        "JOIN user b ON br.borrower_id  = b.user_id " +
        "JOIN user c ON br.custodian_id = c.user_id ";

    public static List<BorrowRecord> getAllBorrowRecords() throws SQLException {
        return queryBorrowRecords(BORROW_BASE + "ORDER BY br.borrowed_date DESC", null);
    }

    public static List<BorrowRecord> getBorrowRecordsByStatus(String status) throws SQLException {
        return queryBorrowRecords(
            BORROW_BASE + "WHERE br.status = ? ORDER BY br.borrowed_date DESC", status);
    }

    public static List<BorrowRecord> getBorrowRecordsByPurpose(String purpose) throws SQLException {
        return queryBorrowRecords(
            BORROW_BASE + "WHERE br.purpose = ? ORDER BY br.borrowed_date DESC", purpose);
    }

    public static List<BorrowRecord> getBorrowHistoryByUser(int userId) throws SQLException {
        String sql = BORROW_BASE + "WHERE br.borrower_id = ? ORDER BY br.borrowed_date DESC";
        List<BorrowRecord> list = new ArrayList<>();
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapBorrowRecord(rs));
            }
        }
        return list;
    }

    public static List<BorrowRecord> getUnreturnedBorrows() throws SQLException {
        return queryBorrowRecords(
            BORROW_BASE + "WHERE br.status IN ('Borrowed','Overdue') ORDER BY br.borrowed_date", null);
    }

    public static List<BorrowRecord> getReturnsWithIssues() throws SQLException {
        return queryBorrowRecords(
            BORROW_BASE + "WHERE br.status = 'Returned with damage/s' ORDER BY br.return_date DESC", null);
    }

    private static List<BorrowRecord> queryBorrowRecords(String sql, String param) throws SQLException {
        List<BorrowRecord> list = new ArrayList<>();
        try (PreparedStatement ps = prepare(sql, param);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapBorrowRecord(rs));
        }
        return list;
    }

    private static BorrowRecord mapBorrowRecord(ResultSet rs) throws SQLException {
        return new BorrowRecord(
            rs.getInt("borrow_id"),
            rs.getString("borrower_name"),
            rs.getString("custodian_name"),
            rs.getDate("borrowed_date"),
            rs.getString("purpose"),
            rs.getDate("return_date"),
            rs.getString("status"),
            rs.getString("remarks")
        );
    }


    public static List<BorrowItem> getBorrowItemsByRecord(int borrowId) throws SQLException {
        List<BorrowItem> list = new ArrayList<>();
        String sql =
            "SELECT bi.borrow_item_id, bi.borrow_id, i.item_name, i.tag, bi.quantity " +
            "FROM borrow_item bi JOIN item i ON bi.item_id = i.item_id " +
            "WHERE bi.borrow_id = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, borrowId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new BorrowItem(
                        rs.getInt("borrow_item_id"),
                        rs.getInt("borrow_id"),
                        rs.getString("item_name"),
                        rs.getString("tag"),
                        rs.getInt("quantity")
                    ));
                }
            }
        }
        return list;
    }

    private static final String ACTIVITY_BASE =
        "SELECT a.activity_id, a.activity_name, f.facility_name, a.event_type, a.event_date, " +
        "CONCAT(r.first_name,' ',r.last_name) AS requester_name, " +
        "CONCAT(ap.first_name,' ',ap.last_name) AS approved_by_name, " +
        "a.approval_status " +
        "FROM activity a " +
        "JOIN facility f ON a.facility_id = f.facility_id " +
        "JOIN user r ON a.requester_id = r.user_id " +
        "LEFT JOIN user ap ON a.approved_by = ap.user_id ";

    public static List<Activity> getAllActivities() throws SQLException {
        return queryActivities(ACTIVITY_BASE + "ORDER BY a.event_date DESC", null);
    }

    public static List<Activity> getActivitiesByStatus(String status) throws SQLException {
        return queryActivities(
            ACTIVITY_BASE + "WHERE a.approval_status = ? ORDER BY a.event_date DESC", status);
    }

    private static List<Activity> queryActivities(String sql, String param) throws SQLException {
        List<Activity> list = new ArrayList<>();
        try (PreparedStatement ps = prepare(sql, param);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Activity(
                    rs.getInt("activity_id"),
                    rs.getString("activity_name"),
                    rs.getString("facility_name"),
                    rs.getString("event_type"),
                    rs.getDate("event_date"),
                    rs.getString("requester_name"),
                    rs.getString("approved_by_name"),
                    rs.getString("approval_status")
                ));
            }
        }
        return list;
    }


    public static List<Facility> getAllFacilities() throws SQLException {
        List<Facility> list = new ArrayList<>();
        try (Statement st = Database.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM facility ORDER BY facility_name")) {
            while (rs.next()) {
                list.add(new Facility(
                    rs.getInt("facility_id"),
                    rs.getString("facility_name"),
                    rs.getString("room_number")
                ));
            }
        }
        return list;
    }


    private static final String REQUEST_BASE =
        "SELECT rq.request_id, " +
        "CONCAT(u.first_name,' ',u.last_name) AS requester_name, " +
        "a.activity_name, rq.request_date, rq.status, " +
        "CONCAT(ap.first_name,' ',ap.last_name) AS approved_by_name, rq.remarks " +
        "FROM borrow_request rq " +
        "JOIN user u ON rq.requester_id = u.user_id " +
        "JOIN activity a ON rq.event_id = a.activity_id " +
        "LEFT JOIN user ap ON rq.approved_by = ap.user_id ";

    public static List<BorrowRequest> getAllBorrowRequests() throws SQLException {
        return queryRequests(REQUEST_BASE + "ORDER BY rq.request_date DESC", null);
    }

    public static List<BorrowRequest> getBorrowRequestsByStatus(String status) throws SQLException {
        return queryRequests(
            REQUEST_BASE + "WHERE rq.status = ? ORDER BY rq.request_date DESC", status);
    }

    private static List<BorrowRequest> queryRequests(String sql, String param) throws SQLException {
        List<BorrowRequest> list = new ArrayList<>();
        try (PreparedStatement ps = prepare(sql, param);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new BorrowRequest(
                    rs.getInt("request_id"),
                    rs.getString("requester_name"),
                    rs.getString("activity_name"),
                    rs.getDate("request_date"),
                    rs.getString("status"),
                    rs.getString("approved_by_name"),
                    rs.getString("remarks")
                ));
            }
        }
        return list;
    }

    private static PreparedStatement prepare(String sql, String param) throws SQLException {
        PreparedStatement ps = Database.getConnection().prepareStatement(sql);
        if (param != null) ps.setString(1, param);
        return ps;
    }
}