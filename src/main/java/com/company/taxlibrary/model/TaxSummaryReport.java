package com.company.taxlibrary.model;

import org.jspecify.annotations.NonNull;
import java.math.BigDecimal;
import java.util.List;

public record TaxSummaryReport(
    @NonNull BigDecimal grandSubtotal,
    @NonNull BigDecimal grandTotalVat,
    @NonNull BigDecimal grandTotalAmount,
    @NonNull List<TaxItemOutput> itemResults,
    @NonNull List<ValidationWarning> validationWarnings,
    long processingTimeMs
) {
    public TaxSummaryReport {
        if (itemResults == null) {
            itemResults = List.of();
        } else {
            itemResults = List.copyOf(itemResults);
        }
        if (validationWarnings == null) {
            validationWarnings = List.of();
        } else {
            validationWarnings = List.copyOf(validationWarnings);
        }
    }

    public BigDecimal getGrandSubtotal() { return grandSubtotal(); }
    public BigDecimal getGrandTotalVat() { return grandTotalVat(); }
    public BigDecimal getGrandTotalAmount() { return grandTotalAmount(); }
    public List<TaxItemOutput> getItemResults() { return itemResults(); }
    public List<ValidationWarning> getValidationWarnings() { return validationWarnings(); }
    public long getProcessingTimeMs() { return processingTimeMs(); }
}