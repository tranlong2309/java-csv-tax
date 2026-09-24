package com.company.taxlibrary.util;

import com.company.taxlibrary.config.MetadataConfig;
import com.company.taxlibrary.exception.MetadataConfigException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeaderNormalizerTest {
    @Test
    void resolvesVietnameseHeadersRegardlessOfCaseWhitespaceAndAccents() {
        Map<String, Integer> indexes = HeaderNormalizer.resolveColumnIndexes(Arrays.asList(
                " MẶT HÀNG ", " SỐ_LƯỢNG", " ĐƠN_GIÁ ", " PHẦN_TRĂM_VAT "));

        assertThat(indexes)
                .containsEntry(HeaderNormalizer.ITEM_NAME, 0)
                .containsEntry(HeaderNormalizer.QUANTITY, 1)
                .containsEntry(HeaderNormalizer.UNIT_PRICE, 2)
                .containsEntry(HeaderNormalizer.VAT_RATE, 3)
                .doesNotContainKey(HeaderNormalizer.SUBTOTAL);
    }

    @Test
    void resolvesCustomAliasesFromJsonAndLeavesMissingColumnsAbsent() {
                                MetadataConfig config = MetadataConfig.fromJson(
                                                                "{\"columnMapping\":{"
                                                                                                + "\"itemNameHeader\":[\"Product\"],"
                                                                                                + "\"quantityHeader\":[\"Qty\"],"
                                                                                                + "\"unitPriceHeader\":[\"Price\"],"
                                                                                                + "\"vatRateHeader\":[\"Tax Rate\"]"
                                                                                                + "}}");

        Map<String, Integer> indexes = HeaderNormalizer.resolveColumnIndexes(
                Arrays.asList(" product ", "QTY", "price", "tax rate"), config);

        assertThat(indexes).containsEntry(HeaderNormalizer.ITEM_NAME, 0)
                .containsEntry(HeaderNormalizer.QUANTITY, 1)
                .containsEntry(HeaderNormalizer.UNIT_PRICE, 2)
                .containsEntry(HeaderNormalizer.VAT_RATE, 3)
                .doesNotContainKey(HeaderNormalizer.SUBTOTAL);
    }

    @Test
    void parsesVatValuesAsDecimalRatios() {
        assertThat(HeaderNormalizer.parseVatRate(" 10 ")).isEqualByComparingTo("0.10");
        assertThat(HeaderNormalizer.parseVatRate("0.1")).isEqualByComparingTo("0.1");
        assertThat(HeaderNormalizer.parseVatRate(" 10% ")).isEqualByComparingTo("0.10");
    }

        @Test
        void normalizesNullBomAccentsAndPunctuation() {
                assertThat(HeaderNormalizer.normalize(null)).isEmpty();
                assertThat(HeaderNormalizer.normalize("\uFEFF Đơn Giá ")).isEqualTo("dongia");
                assertThat(HeaderNormalizer.normalizeHeader(" VAT-Rate ")).isEqualTo("vatrate");
        }

        @Test
        void safelyHandlesMissingHeaderCollectionsAndAliases() {
                assertThat(HeaderNormalizer.resolveColumnIndexes((String[]) null)).isEmpty();
                assertThat(HeaderNormalizer.resolveColumnIndexes(new String[]{"quantity"}))
                        .containsEntry(HeaderNormalizer.QUANTITY, 0);
                assertThat(HeaderNormalizer.resolveColumnIndexes(Collections.emptyList())).isEmpty();
                assertThat(HeaderNormalizer.resolveColumnIndexes(null, null)).isEmpty();
                assertThat(HeaderNormalizer.resolveColumnIndexes(Collections.singletonList("quantity"), null))
                        .containsEntry(HeaderNormalizer.QUANTITY, 0);
                assertThat(HeaderNormalizer.findColumnIndex(null, Collections.singletonList("x"))).isNull();
                assertThat(HeaderNormalizer.findColumnIndex(Collections.singletonList("x"), null)).isNull();
                assertThat(HeaderNormalizer.findColumnIndex(Arrays.asList(null, "x"), Collections.singletonList("y")))
                                .isNull();
        }

        @Test
        void parsesZeroOnePercentAndRejectsInvalidVatText() {
                assertThat(HeaderNormalizer.parseVatRate("0")).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(HeaderNormalizer.parseVatRate("1")).isEqualByComparingTo("0.01");
                assertThat(HeaderNormalizer.parseVatRate("0%")).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(HeaderNormalizer.parseVatRate("100%")).isEqualByComparingTo(BigDecimal.ONE);
                assertThatThrownBy(() -> HeaderNormalizer.parseVatRate(null)).isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> HeaderNormalizer.parseVatRate("  ")).isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> HeaderNormalizer.parseVatRate("-1")).isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> HeaderNormalizer.parseVatRate("not-a-rate"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Invalid VAT rate");
        }

        @Test
        void readsMetadataThroughReaderFileAndPath(@TempDir Path tempDir) throws Exception {
                String json = "{\"csvDelimiter\":\";\",\"charset\":\"UTF-8\","
                                + "\"lenientMode\":false,\"columnMapping\":{\"itemNameHeader\":[\"Product\"]}}";
                Path metadataPath = tempDir.resolve("metadata.json");
                Files.write(metadataPath, json.getBytes(StandardCharsets.UTF_8));

                MetadataConfig fromReader = MetadataConfig.fromJson(new StringReader(json));
                MetadataConfig fromFile = MetadataConfig.fromJson(metadataPath.toFile());
                MetadataConfig fromPath = MetadataConfig.fromJson(metadataPath);

                for (MetadataConfig config : Arrays.asList(fromReader, fromFile, fromPath)) {
                        assertThat(config.getCsvDelimiter()).isEqualTo(";");
                        assertThat(config.getCharset()).isEqualTo("UTF-8");
                        assertThat(config.isLenientMode()).isFalse();
                        assertThat(config.getColumnMapping().getItemNameHeader()).containsExactly("Product");
                        assertThat(config.getColumnMapping().getQuantityHeader()).isNotEmpty();
                }
        }

        @Test
        void metadataDefaultsAndInvalidJsonAreExplicit(@TempDir Path tempDir) throws Exception {
                MetadataConfig defaults = MetadataConfig.defaults();
                assertThat(defaults.getCsvDelimiter()).isEqualTo(",");
                assertThat(defaults.getCharset()).isEqualTo("UTF-8");
                assertThat(defaults.isLenientMode()).isTrue();
                assertThat(defaults.getColumnMapping().getSubtotalHeader()).isNotEmpty();
                assertThat(MetadataConfig.fromJson("{\"lenientMode\":false}").isLenientMode()).isFalse();
                assertThatThrownBy(() -> MetadataConfig.fromJson((String) null))
                                .isInstanceOf(MetadataConfigException.class);
                assertThatThrownBy(() -> MetadataConfig.fromJson("not-json"))
                                .isInstanceOf(MetadataConfigException.class);
                assertThatThrownBy(() -> MetadataConfig.fromJson(new StringReader("not-json")))
                        .isInstanceOf(MetadataConfigException.class);
                Path invalidMetadata = tempDir.resolve("invalid-metadata.json");
                Files.write(invalidMetadata, "not-json".getBytes(StandardCharsets.UTF_8));
                assertThatThrownBy(() -> MetadataConfig.fromJson(invalidMetadata.toFile()))
                        .isInstanceOf(MetadataConfigException.class);
                assertThatThrownBy(() -> MetadataConfig.fromJson((StringReader) null))
                                .isInstanceOf(MetadataConfigException.class);
                assertThatThrownBy(() -> MetadataConfig.fromJson((java.io.File) null))
                                .isInstanceOf(MetadataConfigException.class);
                assertThatThrownBy(() -> MetadataConfig.fromJson((Path) null))
                        .isInstanceOf(MetadataConfigException.class);
        }

}