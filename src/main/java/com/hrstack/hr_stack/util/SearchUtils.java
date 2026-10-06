package com.hrstack.hr_stack.util;

public final class SearchUtils {

    private SearchUtils() {}


    public static String toLikePattern(String term) {
        String escaped = term.toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");

        return "%" + escaped + "%";
    }
}