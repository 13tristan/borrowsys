package ui.utils;

public class HelperUi {
    public static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
