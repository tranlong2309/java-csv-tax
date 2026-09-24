package com.company.taxlibrary.service;

import com.company.taxlibrary.model.TaxItemOutput;
import com.company.taxlibrary.model.TaxSummaryReport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Stateless financial calculator for itemized VAT and aggregate totals. */
public final class TaxCalculator {
    private static final int MONEY_SCALE = 2;
    private static final int INPUT_SCALE = 4;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    private static final BigDecimal RATE_010 = new BigDecimal("0.10");
    private static final BigDecimal RATE_10 = new BigDecimal("10.00");
    private static final java.math.MathContext MATH_CTX = new java.math.MathContext(16, RoundingMode.HALF_UP);

    private TaxCalculator() {
    }

    public static TaxItemOutput calculateItem(
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal vatRate) {
        return calculateItem(0, itemName, quantity, unitPrice, vatRate);
    }

    public static TaxItemOutput calculateItem(
            int lineNumber,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal vatRate) {
        BigDecimal normalizedQuantity = requireValue(quantity, "quantity")
                .setScale(INPUT_SCALE, RoundingMode.HALF_UP);
        BigDecimal normalizedUnitPrice = requireValue(unitPrice, "unitPrice")
                .setScale(INPUT_SCALE, RoundingMode.HALF_UP);
        BigDecimal normalizedVatRate = normalizeVatRate(vatRate);

        BigDecimal subtotal = normalizedQuantity.multiply(normalizedUnitPrice, MATH_CTX)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal vatAmount = subtotal.multiply(normalizedVatRate, MATH_CTX)
                .divide(ONE_HUNDRED, MATH_CTX)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(vatAmount)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return new TaxItemOutput(
                lineNumber,
                itemName,
                normalizedQuantity,
                normalizedUnitPrice,
                normalizedVatRate,
                subtotal,
                vatAmount,
                totalAmount,
                true,
                Collections.emptyList());
    }

    public static TaxSummaryReport calculateGrandTotals(List<TaxItemOutput> itemOutputs) {
        Objects.requireNonNull(itemOutputs, "itemOutputs must not be null");

        BigDecimal grandSubtotal = ZERO_MONEY;
        BigDecimal grandTotalVat = ZERO_MONEY;
        List<TaxItemOutput> results = new ArrayList<>(itemOutputs.size());
        for (TaxItemOutput itemOutput : itemOutputs) {
            TaxItemOutput output = Objects.requireNonNull(itemOutput, "itemOutputs must not contain null");
            results.add(output);
            if (output.isValid()) {
                grandSubtotal = grandSubtotal.add(requireValue(output.getSubtotal(), "subtotal"));
                grandTotalVat = grandTotalVat.add(requireValue(output.getVatAmount(), "vatAmount"));
            }
        }

        grandSubtotal = grandSubtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        grandTotalVat = grandTotalVat.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal grandTotalAmount = grandSubtotal.add(grandTotalVat)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return new TaxSummaryReport(
                grandSubtotal,
                grandTotalVat,
                grandTotalAmount,
                results,
                Collections.emptyList(),
                0L);
    }

    public static TaxSummaryReport calculateSummary(List<TaxItemOutput> itemOutputs) {
        return calculateGrandTotals(itemOutputs);
    }

    private static BigDecimal normalizeVatRate(BigDecimal vatRate) {
        BigDecimal value = requireValue(vatRate, "vatRate");
        if (value.signum() == 0) {
            return ZERO_MONEY;
        }
        if (value.compareTo(RATE_010) == 0 || value.compareTo(RATE_10) == 0) {
            return RATE_10;
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("vatRate must not be negative");
        }
        if (value.compareTo(BigDecimal.ONE) < 0) {
            value = value.multiply(ONE_HUNDRED, MATH_CTX);
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal requireValue(BigDecimal value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " must not be null");
    }
}