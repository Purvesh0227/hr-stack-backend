package com.hrstack.hr_stack.util;

public class StringUtil {

    public static String toDisplayText(String value) {

        if (value == null || value.isBlank()) {
            return "";
        }

        String[] words = value
                .replace("_", " ")
                .toLowerCase()
                .split(" ");

        StringBuilder result = new StringBuilder();

        for (String word : words) {

            if (word.isBlank()) {
                continue;
            }

            result.append(
                    Character.toUpperCase(word.charAt(0))
            );

            if (word.length() > 1) {
                result.append(word.substring(1));
            }

            result.append(" ");
        }

        return result.toString().trim();
    }
}