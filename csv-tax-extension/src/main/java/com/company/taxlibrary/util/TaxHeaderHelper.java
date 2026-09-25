package com.company.taxlibrary.util;
public final class TaxHeaderHelper {
    public static final String ITEM_NAME = "itemName";
    public static final String QUANTITY = "quantity";
    public static final String UNIT_PRICE = "unitPrice";
    public static final String VAT_RATE = "vatRate";
    public static final String SUBTOTAL = "subtotal";
    
    private TaxHeaderHelper() {}
    public static java.util.Map<String, java.util.List<String>> getDefaultMapping() {
        java.util.Map<String, java.util.List<String>> map = new java.util.LinkedHashMap<>();
        map.put(ITEM_NAME, java.util.List.of("mat_hang", "itemName", "item_name"));
        map.put(QUANTITY, java.util.List.of("so_luong", "quantity"));
        map.put(UNIT_PRICE, java.util.List.of("don_gia", "unitPrice", "unit_price"));
        map.put(VAT_RATE, java.util.List.of("phan_tram_vat", "vatRate", "vat_rate"));
        return map;
    }

    
    public static java.math.BigDecimal parseVatRate(String vatStr) {
        if (vatStr == null || vatStr.trim().isEmpty()) return java.math.BigDecimal.ZERO;
        String clean = vatStr.replace("%", "").trim();
        return new java.math.BigDecimal(clean);
    }
}
