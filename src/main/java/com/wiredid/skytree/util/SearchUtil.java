package com.wiredid.skytree.util;

import java.util.regex.Pattern;

/**
 * Utility for advanced search matching.
 */
public class SearchUtil {

    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    /**
     * Checks if a target string matches an advanced search query.
     * The query is split into words (tokens), and every token must be present in
     * the target.
     * 
     * @param target The string to search in (e.g., item name).
     * @param query  The search query (e.g., "neth ing").
     * @return true if all tokens match, false otherwise.
     */
    public static boolean matches(String target, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }

        if (target == null) {
            return false;
        }

        // Clean target of colors and lowercase
        String cleanTarget = COLOR_PATTERN.matcher(target).replaceAll("").toLowerCase();
        String lowerQuery = query.toLowerCase();

        // Split query into tokens
        String[] tokens = lowerQuery.split("\\s+");

        for (String token : tokens) {
            if (token.isEmpty())
                continue;
            if (!cleanTarget.contains(token)) {
                return false;
            }
        }

        return true;
    }
}
