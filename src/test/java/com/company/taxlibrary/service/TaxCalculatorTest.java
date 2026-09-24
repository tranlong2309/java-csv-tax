package com.company.taxlibrary.service;

import com.company.taxlibrary.model.TaxItemOutput;
import com.company.taxlibrary.model.TaxSummaryReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaxCalculatorTest {
    @Test
    void calculatesZeroTaxWithoutLosingDecimalPrecision() {
        TaxItemOutput output = TaxCalculator.calculateItem(
                "Zero-rated",
                new BigDecimal("1.2500"),
                new BigDecimal("12.3456"),
                BigDecimal.ZERO);

        assertThat(output.getSubtotal()).isEqualByComparingTo("15.43");
        assertThat(output.getVatAmount()).isEqualByComparingTo("0.00");
        assertThat(output.getTotalAmount()).isEqualByComparingTo("15.43");
    }

    @Test
    void acceptsFractionalVatRateAndRoundsHalfUp() {
        TaxItemOutput output = TaxCalculator.calculateItem(
                "Boundary",
                new BigDecimal("3"),
                new BigDecimal("33333"),
                new BigDecimal("0.1"));

        assertThat(output.getVatRate()).isEqualByComparingTo("10.00");
        assertThat(output.getVatAmount()).isEqualByComparingTo("9999.90");
        assertThat(output.getTotalAmount()).isEqualByComparingTo("109998.90");
    }

    @Test
    void aggregatesGrandTotalsFromItems() {
        TaxItemOutput first = TaxCalculator.calculateItem(
                "First", new BigDecimal("2"), new BigDecimal("100000"), new BigDecimal("10"));
        TaxItemOutput second = TaxCalculator.calculateItem(
                "Second", new BigDecimal("1"), new BigDecimal("15000000"), new BigDecimal("0.1"));

        TaxSummaryReport report = TaxCalculator.calculateGrandTotals(Arrays.asList(first, second));

        assertThat(report.getGrandSubtotal()).isEqualByComparingTo("15200000.00");
        assertThat(report.getGrandTotalVat()).isEqualByComparingTo("1520000.00");
        assertThat(report.getGrandTotalAmount()).isEqualByComparingTo("16720000.00");
        assertThat(report.getItemResults()).containsExactly(first, second);
    }

    @Test
    void rejectsNegativeVatRatesInsteadOfSwallowingTheError() {
        assertThatThrownBy(() -> TaxCalculator.calculateItem(
                "Invalid", BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnsZeroTotalsForEmptyInput() {
        TaxSummaryReport report = TaxCalculator.calculateSummary(Collections.emptyList());

        assertThat(report.getGrandSubtotal()).isEqualByComparingTo("0.00");
        assertThat(report.getGrandTotalVat()).isEqualByComparingTo("0.00");
        assertThat(report.getGrandTotalAmount()).isEqualByComparingTo("0.00");
    }

        @Test
        void supportsLineNumberAndWholePercentageRates() {
        TaxItemOutput output = TaxCalculator.calculateItem(
            42, "Whole-rate", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);

        assertThat(output.getLineNumber()).isEqualTo(42);
        assertThat(output.getVatRate()).isEqualByComparingTo("1.00");
        assertThat(output.isValid()).isTrue();
        assertThat(output.getWarnings()).isEmpty();
        }

        @Test
        void rejectsNullCalculationInputs() {
            assertThat(TaxCalculator.calculateItem(null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)
                .getItemName()).isNull();
        assertThatThrownBy(() -> TaxCalculator.calculateItem("x", null, BigDecimal.ONE, BigDecimal.ONE))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TaxCalculator.calculateItem("x", BigDecimal.ONE, null, BigDecimal.ONE))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TaxCalculator.calculateItem("x", BigDecimal.ONE, BigDecimal.ONE, null))
            .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejectsNullAndInvalidAggregationInputs() {
        assertThatThrownBy(() -> TaxCalculator.calculateGrandTotals(null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TaxCalculator.calculateGrandTotals(Collections.singletonList(null)))
            .isInstanceOf(NullPointerException.class);

        TaxItemOutput invalid = new TaxItemOutput(
            1, "invalid", null, null, null, null, null, null, false,
            Collections.singletonList("INVALID"));
        TaxSummaryReport report = TaxCalculator.calculateGrandTotals(Collections.singletonList(invalid));

        assertThat(report.getGrandSubtotal()).isEqualByComparingTo("0.00");
        assertThat(report.getGrandTotalVat()).isEqualByComparingTo("0.00");
        assertThat(report.getItemResults()).containsExactly(invalid);
        }
}