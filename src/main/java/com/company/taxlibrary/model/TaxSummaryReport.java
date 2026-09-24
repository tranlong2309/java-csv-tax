package com.company.taxlibrary.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable aggregate of tax calculation results. */
public final class TaxSummaryReport {
    private final BigDecimal grandSubtotal;
    private final BigDecimal grandTotalVat;
    private final BigDecimal grandTotalAmount;
    private final List<TaxItemOutput> itemResults;
    private final List<ValidationWarning> validationWarnings;
    private final long processingTimeMs;

    public TaxSummaryReport(
            BigDecimal grandSubtotal,
            BigDecimal grandTotalVat,
            BigDecimal grandTotalAmount,
            List<TaxItemOutput> itemResults,
            List<ValidationWarning> validationWarnings,
            long processingTimeMs) {
        this.grandSubtotal = Objects.requireNonNull(grandSubtotal, "grandSubtotal must not be null");
        this.grandTotalVat = Objects.requireNonNull(grandTotalVat, "grandTotalVat must not be null");
        this.grandTotalAmount = Objects.requireNonNull(grandTotalAmount, "grandTotalAmount must not be null");
        this.itemResults = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(itemResults, "itemResults must not be null")));
        this.validationWarnings = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(validationWarnings, "validationWarnings must not be null")));
        this.processingTimeMs = processingTimeMs;
    }

    public BigDecimal getGrandSubtotal() {
        return grandSubtotal;
    }

    public BigDecimal getGrandTotalVat() {
        return grandTotalVat;
    }

    public BigDecimal getGrandTotalAmount() {
        return grandTotalAmount;
    }

    public List<TaxItemOutput> getItemResults() {
        return itemResults;
    }

    public List<ValidationWarning> getValidationWarnings() {
        return validationWarnings;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
}