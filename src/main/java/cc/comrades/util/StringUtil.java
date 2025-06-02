package cc.comrades.util;

public class StringUtil {
    public static String[] getWhitelistArray(String input) {
        String[] parts = input.split(": ");
        if (parts.length > 1) {
            return parts[1].split(",\\s*");
        }
        return new String[0];
    }

    public static String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9_]", "");
    }
}
