package com.wiredid.skytree.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TextUtil {

    public static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return Arrays.stream(input.toLowerCase().split(" "))
                .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
