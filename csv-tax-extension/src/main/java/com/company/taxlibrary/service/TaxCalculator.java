package com.company.taxlibrary.service;
import com.company.csvengine.calc.RecordCalculator;
import com.company.taxlibrary.model.TaxItemOutput;
import com.company.taxlibrary.model.TaxSummaryReport;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class TaxCalculator implements RecordCalculator<TaxItemOutput> {
    @Override
    public TaxItemOutput calculate(Map<String, String> rawRow) {
        String itemName = rawRow.get("itemName");
        String qtyStr = rawRow.get("quantity");
        String unitPriceStr = rawRow.get("unitPrice");
        String vatRateStr = rawRow.get("vatRate");
        
        if (itemName == null || qtyStr == null || unitPriceStr == null || vatRateStr == null) {
            throw new IllegalArgumentException("Missing required tax fields");
        }
        
        BigDecimal quantity = new BigDecimal(qtyStr);
        BigDecimal unitPrice = new BigDecimal(unitPriceStr);
        BigDecimal vatRate = new BigDecimal(vatRateStr.replace("%", "").trim());
        
        return calculateItem(1, itemName, quantity, unitPrice, vatRate);
    }

    public static TaxItemOutput calculateItem(int lineNumber, String itemName, BigDecimal quantity, BigDecimal unitPrice, BigDecimal vatRate) {
        BigDecimal subtotal = quantity.multiply(unitPrice);
        BigDecimal vatAmount = subtotal.multiply(vatRate).divide(new BigDecimal("100"));
        BigDecimal totalAmount = subtotal.add(vatAmount);
        return new TaxItemOutput(itemName, quantity, unitPrice, vatRate, subtotal, vatAmount, totalAmount);
    }

    public static TaxSummaryReport calculateGrandTotals(List<TaxItemOutput> items) {
        BigDecimal grandSubtotal = BigDecimal.ZERO;
        BigDecimal grandTotalVat = BigDecimal.ZERO;
        BigDecimal grandTotalAmount = BigDecimal.ZERO;

        for (TaxItemOutput item : items) {
            grandSubtotal = grandSubtotal.add(item.getSubtotal());
            grandTotalVat = grandTotalVat.add(item.getVatAmount());
            grandTotalAmount = grandTotalAmount.add(item.getTotalAmount());
        }
        return new TaxSummaryReport(grandSubtotal, grandTotalVat, grandTotalAmount, items, new java.util.ArrayList<>(), 0);
    }
}
