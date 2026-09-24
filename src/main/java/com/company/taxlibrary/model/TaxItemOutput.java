package com.company.taxlibrary.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Calculated tax values and validation state for one input row. */
public final class TaxItemOutput {
    private final int lineNumber;
    private final String itemName;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal vatRate;
    private final BigDecimal subtotal;
    private final BigDecimal vatAmount;
    private final BigDecimal totalAmount;
    private final boolean valid;
    private final List<String> warnings;

    public TaxItemOutput(
            int lineNumber,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal vatRate,
            BigDecimal subtotal,
            BigDecimal vatAmount,
            BigDecimal totalAmount,
            boolean valid,
            List<String> warnings) {
        this.lineNumber = lineNumber;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.vatRate = vatRate;
        this.subtotal = subtotal;
        this.vatAmount = vatAmount;
        this.totalAmount = totalAmount;
        this.valid = valid;
        this.warnings = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(warnings, "warnings must not be null")));
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getVatAmount() {
        return vatAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}