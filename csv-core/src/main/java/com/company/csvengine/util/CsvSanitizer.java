package com.company.csvengine.util;

/** Prevents spreadsheet formula injection in generated CSV text. */
public final class CsvSanitizer {
    private static final char[] DANGEROUS_PREFIXES = {'=', '+', '-', '@', '\t', '\r'};

    private CsvSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        for (char prefix : DANGEROUS_PREFIXES) {
            if (input.charAt(0) == prefix) {
                return "'" + input;
            }
        }
        return input;
    }
}