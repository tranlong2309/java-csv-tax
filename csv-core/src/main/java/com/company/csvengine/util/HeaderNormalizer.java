package com.company.csvengine.util;
import java.text.Normalizer;
import java.util.List;

public final class HeaderNormalizer {
    private HeaderNormalizer() {}
    public static String normalize(String header) {
        if (header == null) return "";
        String stripped = header;
        if (stripped.startsWith("\uFEFF")) {
            stripped = stripped.substring(1);
        }
        
        stripped = stripped.replace('đ', 'd').replace('Đ', 'd');
        stripped = Normalizer.normalize(stripped, Normalizer.Form.NFD)

                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
        return stripped.trim();
    }
}
