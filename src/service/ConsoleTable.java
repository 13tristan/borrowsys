package service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for printing SQL result sets in a clean tabular format.
 *
 * This is used by the service layer so every list/report shown in the console
 * is easier to read during testing and presentation. It only formats the data
 * already returned by stored procedures; it does not perform any database call.
 */
public final class ConsoleTable {
    private static final int MAX_COLUMN_WIDTH = 32;

    private ConsoleTable() {
        // Utility class: no object should be created.
    }

    public static void printResultSet(ResultSet rs, String title) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        List<String> headers = new ArrayList<>();
        int[] widths = new int[columnCount];

        for (int i = 1; i <= columnCount; i++) {
            String header = formatHeader(meta.getColumnLabel(i));
            headers.add(header);
            widths[i - 1] = Math.min(MAX_COLUMN_WIDTH, Math.max(header.length(), 4));
        }

        List<List<String>> rows = new ArrayList<>();
        while (rs.next()) {
            List<String> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                String value = BorrowerService.nvl(rs.getString(i));
                value = value.replace("\r", " ").replace("\n", " ").trim();
                String displayValue = truncate(value, MAX_COLUMN_WIDTH);
                row.add(displayValue);
                widths[i - 1] = Math.min(MAX_COLUMN_WIDTH, Math.max(widths[i - 1], displayValue.length()));
            }
            rows.add(row);
        }

        System.out.println("\n  " + title);
        if (rows.isEmpty()) {
            System.out.println("  No records found.");
            return;
        }

        printLine(widths);
        printRow(headers, widths);
        printLine(widths);
        for (List<String> row : rows) {
            printRow(row, widths);
        }
        printLine(widths);
        System.out.println("  Total records: " + rows.size());
    }

    public static void printMessage(String message) {
        System.out.println("  " + message);
    }

    private static String formatHeader(String label) {
        if (label == null || label.isBlank()) return "COLUMN";
        return label.trim().replace('_', ' ').toUpperCase();
    }

    private static String truncate(String value, int maxWidth) {
        if (value == null) return "N/A";
        if (value.length() <= maxWidth) return value;
        if (maxWidth <= 3) return value.substring(0, maxWidth);
        return value.substring(0, maxWidth - 3) + "...";
    }

    private static void printLine(int[] widths) {
        StringBuilder sb = new StringBuilder("  +");
        for (int width : widths) {
            sb.append("-").append("-".repeat(width)).append("-+");
        }
        System.out.println(sb);
    }

    private static void printRow(List<String> values, int[] widths) {
        StringBuilder sb = new StringBuilder("  |");
        for (int i = 0; i < widths.length; i++) {
            String value = i < values.size() ? values.get(i) : "";
            sb.append(' ').append(padRight(value, widths[i])).append(" |");
        }
        System.out.println(sb);
    }

    private static String padRight(String value, int width) {
        if (value == null) value = "";
        if (value.length() >= width) return value;
        return value + " ".repeat(width - value.length());
    }
}
