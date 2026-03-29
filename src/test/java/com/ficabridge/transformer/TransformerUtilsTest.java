package com.ficabridge.transformer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransformerUtilsTest {

    // ── stripLeadingZeros ────────────────────────────────────────────────────

    @Test
    void stripLeadingZeros_null_returnsNull() {
        assertThat(TransformerUtils.stripLeadingZeros(null)).isNull();
    }

    @Test
    void stripLeadingZeros_emptyString_returnsEmpty() {
        assertThat(TransformerUtils.stripLeadingZeros("")).isEmpty();
    }

    @Test
    void stripLeadingZeros_allZeros_returnsSingleZero() {
        assertThat(TransformerUtils.stripLeadingZeros("000")).isEqualTo("0");
        assertThat(TransformerUtils.stripLeadingZeros("0")).isEqualTo("0");
    }

    @Test
    void stripLeadingZeros_paddedNumber_stripsLeadingZeros() {
        assertThat(TransformerUtils.stripLeadingZeros("0000100200")).isEqualTo("100200");
        assertThat(TransformerUtils.stripLeadingZeros("0000000001")).isEqualTo("1");
    }

    @Test
    void stripLeadingZeros_noLeadingZeros_returnsUnchanged() {
        assertThat(TransformerUtils.stripLeadingZeros("100200")).isEqualTo("100200");
        assertThat(TransformerUtils.stripLeadingZeros("1")).isEqualTo("1");
    }

    // ── parseSapDate ─────────────────────────────────────────────────────────

    @Test
    void parseSapDate_null_returnsNull() {
        assertThat(TransformerUtils.parseSapDate(null)).isNull();
    }

    @Test
    void parseSapDate_emptyString_returnsNull() {
        assertThat(TransformerUtils.parseSapDate("")).isNull();
    }

    @Test
    void parseSapDate_blankString_returnsNull() {
        assertThat(TransformerUtils.parseSapDate("   ")).isNull();
    }

    @Test
    void parseSapDate_zeroDate_returnsNull() {
        assertThat(TransformerUtils.parseSapDate("00000000")).isNull();
    }

    @Test
    void parseSapDate_validDate_returnsLocalDate() {
        assertThat(TransformerUtils.parseSapDate("20240315")).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(TransformerUtils.parseSapDate("20231231")).isEqualTo(LocalDate.of(2023, 12, 31));
        assertThat(TransformerUtils.parseSapDate("20200101")).isEqualTo(LocalDate.of(2020, 1, 1));
    }

    @Test
    void parseSapDate_invalidFormat_throwsIllegalArgument() {
        assertThatThrownBy(() -> TransformerUtils.parseSapDate("2024-03-15"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2024-03-15");
    }

    // ── parseSapAmount ────────────────────────────────────────────────────────

    @Test
    void parseSapAmount_null_returnsNull() {
        assertThat(TransformerUtils.parseSapAmount(null)).isNull();
    }

    @Test
    void parseSapAmount_emptyString_returnsNull() {
        assertThat(TransformerUtils.parseSapAmount("")).isNull();
    }

    @Test
    void parseSapAmount_blankString_returnsNull() {
        assertThat(TransformerUtils.parseSapAmount("   ")).isNull();
    }

    @Test
    void parseSapAmount_positiveAmount_returnsBigDecimal() {
        assertThat(TransformerUtils.parseSapAmount("1234.56")).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    void parseSapAmount_negativeAmount_returnsBigDecimal() {
        assertThat(TransformerUtils.parseSapAmount("-99.00")).isEqualByComparingTo(new BigDecimal("-99.00"));
    }

    @Test
    void parseSapAmount_zero_returnsBigDecimalZero() {
        assertThat(TransformerUtils.parseSapAmount("0.00")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void parseSapAmount_withWhitespace_trimsAndParses() {
        assertThat(TransformerUtils.parseSapAmount("  500.00  ")).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void parseSapAmount_invalidValue_throwsIllegalArgument() {
        assertThatThrownBy(() -> TransformerUtils.parseSapAmount("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("abc");
    }

    // ── trimSapString ─────────────────────────────────────────────────────────

    @Test
    void trimSapString_null_returnsNull() {
        assertThat(TransformerUtils.trimSapString(null)).isNull();
    }

    @Test
    void trimSapString_paddedString_trimsBothEnds() {
        assertThat(TransformerUtils.trimSapString("  DE  ")).isEqualTo("DE");
        assertThat(TransformerUtils.trimSapString("EUR")).isEqualTo("EUR");
    }

    @Test
    void trimSapString_allSpaces_returnsEmpty() {
        assertThat(TransformerUtils.trimSapString("   ")).isEmpty();
    }
}
