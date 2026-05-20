package com.wiredid.skytree.util;

public class NumberUtil {

    public static String formatCurrency(double amount) {
        if (amount >= 1_000_000_000) {
            return String.format("\u20AE %.2fB", amount / 1_000_000_000);
        } else if (amount >= 1_000_000) {
            return String.format("\u20AE %.2fM", amount / 1_000_000);
        } else if (amount >= 1_000) {
            return String.format("\u20AE %.2fK", amount / 1_000);
        } else {
            return String.format("\u20AE %.2f", amount);
        }
    }

    public static String formatBTC(double amount) {
        return formatCurrency(amount);
    }

    /**
     * Parses a string with potential suffixes (k, m, b, t) into a double.
     * 
     * @param input The string to parse
     * @return The parsed double
     * @throws NumberFormatException if the string is not a valid number
     */
    public static double parseSmartNumber(String input) throws NumberFormatException {
        if (input == null || input.isEmpty()) {
            throw new NumberFormatException("Empty input");
        }

        input = input.trim().toLowerCase();
        double multiplier = 1.0;

        if (input.endsWith("k")) {
            multiplier = 1_000.0;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("m")) {
            multiplier = 1_000_000.0;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("b")) {
            multiplier = 1_000_000_000.0;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("t")) {
            multiplier = 1_000_000_000_000.0;
            input = input.substring(0, input.length() - 1);
        }

        try {
            return Double.parseDouble(input) * multiplier;
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid number format: " + input);
        }
    }
}
