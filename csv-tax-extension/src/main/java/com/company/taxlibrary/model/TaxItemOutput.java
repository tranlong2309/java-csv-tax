package com.company.taxlibrary.model;

import org.jspecify.annotations.NonNull;
import java.math.BigDecimal;
import java.util.List;

public record TaxItemOutput(
    int lineNumber,
    @NonNull String itemName,
    @NonNull BigDecimal quantity,
    @NonNull BigDecimal unitPrice,
    @NonNull BigDecimal vatRate,
    @NonNull BigDecimal subtotal,
    @NonNull BigDecimal vatAmount,
    @NonNull BigDecimal totalAmount,
    boolean valid,
    @NonNull List<String> warnings
) {
    public TaxItemOutput {
        if (warnings == null) {
            warnings = List.of();
        } else {
            warnings = List.copyOf(warnings);
        }
    }

    public int getLineNumber() { return lineNumber(); }
    public String getItemName() { return itemName(); }
    public BigDecimal getQuantity() { return quantity(); }
    public BigDecimal getUnitPrice() { return unitPrice(); }
    public BigDecimal getVatRate() { return vatRate(); }
    public BigDecimal getSubtotal() { return subtotal(); }
    public BigDecimal getVatAmount() { return vatAmount(); }
    public BigDecimal getTotalAmount() { return totalAmount(); }
    public boolean isValid() { return valid(); }
    public List<String> getWarnings() { return warnings(); }
}