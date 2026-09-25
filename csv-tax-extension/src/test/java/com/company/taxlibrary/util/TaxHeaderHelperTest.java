package com.company.taxlibrary.util;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class TaxHeaderHelperTest {
    @Test
    void parseVatRateHandlesPercentage() {
        assertThat(TaxHeaderHelper.parseVatRate("10%")).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(TaxHeaderHelper.parseVatRate(" 5 % ")).isEqualByComparingTo(new BigDecimal("5"));
    }
    @Test
    void parseVatRateHandlesNullOrEmpty() {
        assertThat(TaxHeaderHelper.parseVatRate(null)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(TaxHeaderHelper.parseVatRate("")).isEqualByComparingTo(BigDecimal.ZERO);
    }
    @Test
    void defaultMappingContainsRequiredFields() {
        java.util.Map<String, java.util.List<String>> map = TaxHeaderHelper.getDefaultMapping();
        assertThat(map).containsKey(TaxHeaderHelper.ITEM_NAME);
        assertThat(map).containsKey(TaxHeaderHelper.QUANTITY);
        assertThat(map).containsKey(TaxHeaderHelper.UNIT_PRICE);
        assertThat(map).containsKey(TaxHeaderHelper.VAT_RATE);
    }
}
