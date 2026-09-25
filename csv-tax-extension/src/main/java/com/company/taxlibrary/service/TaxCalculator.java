package com.company.taxlibrary.service;
import com.company.csvengine.calc.RowCalculator;
import com.company.taxlibrary.model.TaxItemOutput;
import com.company.taxlibrary.model.TaxSummaryReport;
import com.company.taxlibrary.util.TaxHeaderHelper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class TaxCalculator implements RowCalculator<TaxItemOutput> {
    @Override
    public TaxItemOutput calculate(Map<String, String> rawRow) {
        String itemName = rawRow.get(TaxHeaderHelper.ITEM_NAME);
        String qtyStr = rawRow.get(TaxHeaderHelper.QUANTITY);
        String unitPriceStr = rawRow.get(TaxHeaderHelper.UNIT_PRICE);
        String vatRateStr = rawRow.get(TaxHeaderHelper.VAT_RATE);
        
        if (itemName == null || qtyStr == null || unitPriceStr == null || vatRateStr == null) {
            throw new IllegalArgumentException("Missing required tax fields");
        }
        
        BigDecimal quantity = new BigDecimal(qtyStr);
        BigDecimal unitPrice = new BigDecimal(unitPriceStr);
        BigDecimal vatRate = TaxHeaderHelper.parseVatRate(vatRateStr);
        
        return calculateItem(-1, itemName, quantity, unitPrice, vatRate);
    }

    public static TaxItemOutput calculateItem(int lineNumber, String itemName, BigDecimal quantity, BigDecimal unitPrice, BigDecimal vatRate) {
        BigDecimal subtotal = quantity.multiply(unitPrice);
        if (vatRate.compareTo(java.math.BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Negative VAT");
    BigDecimal vatAmount = subtotal.multiply(vatRate).divide(new BigDecimal("100"));
        BigDecimal totalAmount = subtotal.add(vatAmount);
        return new TaxItemOutput(lineNumber, itemName, quantity, unitPrice, vatRate, subtotal, vatAmount, totalAmount, true, java.util.List.of());
    }

    
    public static TaxItemOutput calculateItem(String itemName, BigDecimal quantity, BigDecimal unitPrice, BigDecimal vatRate) {
        return calculateItem(-1, itemName, quantity, unitPrice, vatRate);
    }
    public static TaxSummaryReport calculateSummary(List<TaxItemOutput> items) { return calculateGrandTotals(items); }
    public static TaxSummaryReport calculateGrandTotals(List<TaxItemOutput> items) {
        BigDecimal grandSubtotal = BigDecimal.ZERO;
        BigDecimal grandTotalVat = BigDecimal.ZERO;
        BigDecimal grandTotalAmount = BigDecimal.ZERO;

        for (TaxItemOutput item : items) {
            if (item.getSubtotal() != null) grandSubtotal = grandSubtotal.add(item.getSubtotal());
            if (item.getVatAmount() != null) grandTotalVat = grandTotalVat.add(item.getVatAmount());
            if (item.getTotalAmount() != null) grandTotalAmount = grandTotalAmount.add(item.getTotalAmount());
        }
        return new TaxSummaryReport(grandSubtotal, grandTotalVat, grandTotalAmount, items, new java.util.ArrayList<>(), 0);
    }
}
