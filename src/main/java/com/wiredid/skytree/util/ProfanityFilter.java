package com.wiredid.skytree.util;

import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;

public class ProfanityFilter {

    private static final List<String> BLACKLIST = Arrays.asList(
            "fuck", "shit", "bitch", "asshole", "dick", "pussy", "nigger", "faggot");

    private static final Pattern PATTERN = Pattern.compile(
            "(" + String.join("|", BLACKLIST) + ")",
            Pattern.CASE_INSENSITIVE);

    public static String filter(String message) {
        if (message == null)
            return null;
        return PATTERN.matcher(message).replaceAll(match -> {
            char[] chars = new char[match.group().length()];
            Arrays.fill(chars, '*');
            return new String(chars);
        });
    }

    public static boolean containsProfanity(String message) {
        if (message == null)
            return false;
        return PATTERN.matcher(message).find();
    }
}
