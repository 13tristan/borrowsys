package service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for printing ResultSet data in a clean console table.
 *
 * This keeps the CLI output consistent for all roles. The database routines
 * already decide which rows should be returned and how they are ordered; this
 * class only formats those rows for readability.
 */
public class ConsoleTable {
    private static final int MAX_WIDTH = 45;

    public static void print(ResultSet rs, String heading) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        int[] widths = new int[columnCount];

        for (int i = 1; i <= columnCount; i++) {
            String header = formatHeader(meta.getColumnLabel(i));
            headers.add(header);
            widths[i - 1] = Math.min(MAX_WIDTH, Math.max(8, header.length()));
        }

        while (rs.next()) {
            List<String> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                String value = rs.getString(i);
                if (value == null || value.isBlank()) {
                    value = "N/A";
                }
                value = value.replace('\n', ' ').replace('\r', ' ');
                if (value.length() > MAX_WIDTH) {
                    value = value.substring(0, MAX_WIDTH - 3) + "...";
                }
                row.add(value);
                widths[i - 1] = Math.min(MAX_WIDTH, Math.max(widths[i - 1], value.length()));
            }
            rows.add(row);
        }

        System.out.println("\n  " + heading);
        if (rows.isEmpty()) {
            System.out.println("  No records found.");
            return;
        }

        printSeparator(widths);
        printRow(headers, widths);
        printSeparator(widths);
        for (List<String> row : rows) {
            printRow(row, widths);
        }
        printSeparator(widths);
        System.out.println("  Total records: " + rows.size());
    }

    private static String formatHeader(String header) {
        if (header == null || header.isBlank()) {
            return "COLUMN";
        }
        return header.replace('_', ' ').toUpperCase();
    }

    private static void printSeparator(int[] widths) {
        System.out.print("  +");
        for (int width : widths) {
            System.out.print("-".repeat(width + 2) + "+");
        }
        System.out.println();
    }

    private static void printRow(List<String> values, int[] widths) {
        System.out.print("  |");
        for (int i = 0; i < values.size(); i++) {
            System.out.printf(" %-" + widths[i] + "s |", values.get(i));
        }
        System.out.println();
    }
}
