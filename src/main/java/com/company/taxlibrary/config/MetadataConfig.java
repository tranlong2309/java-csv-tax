package com.company.taxlibrary.config;

import com.company.taxlibrary.exception.MetadataConfigException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable metadata used to map CSV headers to tax fields. */
public final class MetadataConfig {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String csvDelimiter;
    private final String charset;
    private final boolean lenientMode;
    private final ColumnMapping columnMapping;

    public MetadataConfig() {
        this(",", "UTF-8", true, null);
    }

    @JsonCreator
    public MetadataConfig(
            @JsonProperty("csvDelimiter") String csvDelimiter,
            @JsonProperty("charset") String charset,
            @JsonProperty("lenientMode") Boolean lenientMode,
            @JsonProperty("columnMapping") ColumnMapping columnMapping) {
        this.csvDelimiter = defaultIfNullOrBlank(csvDelimiter, ",");
        this.charset = defaultIfNullOrBlank(charset, "UTF-8");
        this.lenientMode = lenientMode == null || lenientMode;
        this.columnMapping = columnMapping == null ? ColumnMapping.defaults() : columnMapping;
    }

    public static MetadataConfig defaults() {
        return new MetadataConfig();
    }

    public static MetadataConfig fromJson(String json) {
        if (json == null) {
            throw new MetadataConfigException("Metadata JSON must not be null");
        }
        try {
            return OBJECT_MAPPER.readValue(json, MetadataConfig.class);
        } catch (IOException exception) {
            throw new MetadataConfigException("Unable to parse metadata JSON", exception);
        }
    }

    public static MetadataConfig fromJson(Reader reader) {
        if (reader == null) {
            throw new MetadataConfigException("Metadata reader must not be null");
        }
        try {
            return OBJECT_MAPPER.readValue(reader, MetadataConfig.class);
        } catch (IOException exception) {
            throw new MetadataConfigException("Unable to parse metadata JSON", exception);
        }
    }

    public static MetadataConfig fromJson(File file) {
        if (file == null) {
            throw new MetadataConfigException("Metadata file must not be null");
        }
        try {
            return OBJECT_MAPPER.readValue(file, MetadataConfig.class);
        } catch (IOException exception) {
            throw new MetadataConfigException("Unable to parse metadata JSON", exception);
        }
    }

    public static MetadataConfig fromJson(Path path) {
        return fromJson(path == null ? null : path.toFile());
    }

    public String getCsvDelimiter() {
        return csvDelimiter;
    }

    public String getCharset() {
        return charset;
    }

    public boolean isLenientMode() {
        return lenientMode;
    }

    public ColumnMapping getColumnMapping() {
        return columnMapping;
    }

    private static String defaultIfNullOrBlank(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    /** Immutable aliases for each logical CSV field. */
    public static final class ColumnMapping {
        private static final List<String> DEFAULT_ITEM_NAME_HEADERS = aliases(
                "mat_hang", "ten_mat_hang", "item_name", "item", "description", "sanpham");
        private static final List<String> DEFAULT_QUANTITY_HEADERS = aliases(
                "so_luong", "quantity", "qty", "sl");
        private static final List<String> DEFAULT_UNIT_PRICE_HEADERS = aliases(
                "don_gia", "unit_price", "price", "gia");
        private static final List<String> DEFAULT_VAT_RATE_HEADERS = aliases(
                "phan_tram_vat", "vat_rate", "vat", "thuevat", "thue");
        private static final List<String> DEFAULT_SUBTOTAL_HEADERS = aliases(
                "so_tong", "subtotal", "thanhtien");

        private final List<String> itemNameHeader;
        private final List<String> quantityHeader;
        private final List<String> unitPriceHeader;
        private final List<String> vatRateHeader;
        private final List<String> subtotalHeader;

        public ColumnMapping() {
            this(null, null, null, null, null);
        }

        @JsonCreator
        public ColumnMapping(
                @JsonProperty("itemNameHeader") List<String> itemNameHeader,
                @JsonProperty("quantityHeader") List<String> quantityHeader,
                @JsonProperty("unitPriceHeader") List<String> unitPriceHeader,
                @JsonProperty("vatRateHeader") List<String> vatRateHeader,
                @JsonProperty("subtotalHeader") List<String> subtotalHeader) {
            this.itemNameHeader = aliasesOrDefault(itemNameHeader, DEFAULT_ITEM_NAME_HEADERS);
            this.quantityHeader = aliasesOrDefault(quantityHeader, DEFAULT_QUANTITY_HEADERS);
            this.unitPriceHeader = aliasesOrDefault(unitPriceHeader, DEFAULT_UNIT_PRICE_HEADERS);
            this.vatRateHeader = aliasesOrDefault(vatRateHeader, DEFAULT_VAT_RATE_HEADERS);
            this.subtotalHeader = aliasesOrDefault(subtotalHeader, DEFAULT_SUBTOTAL_HEADERS);
        }

        public static ColumnMapping defaults() {
            return new ColumnMapping();
        }

        public List<String> getItemNameHeader() {
            return itemNameHeader;
        }

        public List<String> getQuantityHeader() {
            return quantityHeader;
        }

        public List<String> getUnitPriceHeader() {
            return unitPriceHeader;
        }

        public List<String> getVatRateHeader() {
            return vatRateHeader;
        }

        public List<String> getSubtotalHeader() {
            return subtotalHeader;
        }

        private static List<String> aliasesOrDefault(List<String> aliases, List<String> defaults) {
            if (aliases == null || aliases.isEmpty()) {
                return defaults;
            }
            return aliases(aliases.toArray(new String[0]));
        }

        private static List<String> aliases(String... values) {
            List<String> cleaned = new ArrayList<>();
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    cleaned.add(value);
                }
            }
            return Collections.unmodifiableList(cleaned);
        }
    }
}