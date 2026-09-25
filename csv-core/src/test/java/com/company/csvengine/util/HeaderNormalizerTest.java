package com.company.csvengine.util;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HeaderNormalizerTest {
    @Test
    void normalizeStripsBOM() {
        assertThat(HeaderNormalizer.normalize("\uFEFFtest")).isEqualTo("test");
    }
    @Test
    void normalizeRemovesAccentsAndSpecialChars() {
        assertThat(HeaderNormalizer.normalize("Mặt hàng (VNĐ)")).isEqualTo("mathangvnd");
        assertThat(HeaderNormalizer.normalize("Số lượng!")).isEqualTo("soluong");
    }
    @Test
    void normalizeConvertsToLowercase() {
        assertThat(HeaderNormalizer.normalize("Item Name")).isEqualTo("itemname");
    }
    @Test
    void normalizeHandlesNull() {
        assertThat(HeaderNormalizer.normalize(null)).isEmpty();
    }
}
