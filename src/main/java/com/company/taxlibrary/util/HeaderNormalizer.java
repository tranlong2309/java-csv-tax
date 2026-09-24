package com.company.taxlibrary.util;

import com.company.taxlibrary.config.MetadataConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Normalizes CSV headers and converts VAT text to a decimal ratio. */
public final class HeaderNormalizer {
    public static final String ITEM_NAME = "itemName";
    public static final String QUANTITY = "quantity";
    public static final String UNIT_PRICE = "unitPrice";
    public static final String VAT_RATE = "vatRate";
    public static final String SUBTOTAL = "subtotal";

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final java.util.regex.Pattern PERCENT_PATTERN = java.util.regex.Pattern.compile("[%\\s]");
    private static final java.util.regex.Pattern DIACRITICS_PATTERN = java.util.regex.Pattern.compile("\\p{M}");
    private static final java.util.regex.Pattern NON_ALPHANUMERIC_PATTERN = java.util.regex.Pattern.compile("[^a-z0-9]");
    private static final java.util.regex.Pattern BOM_PATTERN = java.util.regex.Pattern.compile("^\\uFEFF");

    private HeaderNormalizer() {
    }

    public static String normalize(String header) {
        if (header == null) {
            return "";
        }
        String value = BOM_PATTERN.matcher(header).replaceAll("").trim().toLowerCase(Locale.ROOT);
        value = value.replace('đ', 'd').replace('Đ', 'd');
        value = Normalizer.normalize(value, Normalizer.Form.NFD);
        value = DIACRITICS_PATTERN.matcher(value).replaceAll("");
        return NON_ALPHANUMERIC_PATTERN.matcher(value).replaceAll("");
    }

    public static String normalizeHeader(String header) {
        return normalize(header);
    }

    public static Map<String, Integer> resolveColumnIndexes(String[] headers) {
        return resolveColumnIndexes(headers == null ? null : Arrays.asList(headers), MetadataConfig.defaults());
    }

    public static Map<String, Integer> resolveColumnIndexes(List<String> headers) {
        return resolveColumnIndexes(headers, MetadataConfig.defaults());
    }

    public static Map<String, Integer> resolveColumnIndexes(
            List<String> headers,
            MetadataConfig metadataConfig) {
        if (headers == null || headers.isEmpty()) {
            return Collections.emptyMap();
        }
        MetadataConfig config = metadataConfig == null ? MetadataConfig.defaults() : metadataConfig;
        MetadataConfig.ColumnMapping mapping = config.getColumnMapping();
        Map<String, Integer> indexes = new LinkedHashMap<>();
        addFirstMatch(indexes, ITEM_NAME, headers, mapping.getItemNameHeader());
        addFirstMatch(indexes, QUANTITY, headers, mapping.getQuantityHeader());
        addFirstMatch(indexes, UNIT_PRICE, headers, mapping.getUnitPriceHeader());
        addFirstMatch(indexes, VAT_RATE, headers, mapping.getVatRateHeader());
        addFirstMatch(indexes, SUBTOTAL, headers, mapping.getSubtotalHeader());
        return Collections.unmodifiableMap(indexes);
    }

    public static Integer findColumnIndex(List<String> headers, Collection<String> aliases) {
        if (headers == null || aliases == null) {
            return null;
        }
        for (int index = 0; index < headers.size(); index++) {
            String normalizedHeader = normalize(headers.get(index));
            for (String alias : aliases) {
                if (normalizedHeader.equals(normalize(alias))) {
                    return index;
                }
            }
        }
        return null;
    }

    public static BigDecimal parseVatRate(String rawVatRate) {
        if (rawVatRate == null || rawVatRate.trim().isEmpty()) {
            throw new IllegalArgumentException("VAT rate must not be blank");
        }
        boolean percentage = rawVatRate.contains("%");
        String value = PERCENT_PATTERN.matcher(rawVatRate).replaceAll("");
        try {
            BigDecimal rate = new BigDecimal(value);
            if (rate.signum() < 0) {
                throw new IllegalArgumentException("VAT rate must not be negative");
            }
            if (percentage || rate.compareTo(ONE) >= 0) {
                rate = rate.divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP);
            }
            return rate.stripTrailingZeros();
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid VAT rate: " + rawVatRate, exception);
        }
    }

    private static void addFirstMatch(
            Map<String, Integer> indexes,
            String field,
            List<String> headers,
            Collection<String> aliases) {
        Integer index = findColumnIndex(headers, aliases);
        if (index != null) {
            indexes.put(field, index);
        }
    }
}