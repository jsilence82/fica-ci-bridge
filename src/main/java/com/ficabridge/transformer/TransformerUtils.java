package com.ficabridge.transformer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility methods for mapping SAP field conventions to clean Java types.
 * <p>
 * SAP quirks handled here:
 * <ul>
 *   <li>Dates are YYYYMMDD strings; the zero-date "00000000" means null</li>
 *   <li>Numeric IDs (VKONT, GPART, OPBEL) are zero-padded to a fixed width</li>
 *   <li>Amount fields are strings that may include sign, commas, or whitespace</li>
 *   <li>String fields may be padded with trailing spaces</li>
 * </ul>
 */
public final class TransformerUtils {

    private static final DateTimeFormatter SAP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SAP_ZERO_DATE = "00000000";

    private TransformerUtils() {
    }

    /**
     * Strip SAP leading zeros from a padded numeric string.
     * Returns {@code null} for {@code null} input and {@code "0"} for an all-zeros string.
     *
     * <pre>
     *   "0000100200" → "100200"
     *   "0"          → "0"
     *   "000"        → "0"
     *   ""           → ""
     *   null         → null
     * </pre>
     */
    public static String stripLeadingZeros(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return value;
        }
        // replaceFirst removes all leading zeros; if result is empty the string was all zeros
        String stripped = value.replaceFirst("^0+", "");
        return stripped.isEmpty() ? "0" : stripped;
    }

    /**
     * Parse a SAP YYYYMMDD date string into a {@link LocalDate}.
     * Returns {@code null} for {@code null}, blank, or the SAP zero-date {@code "00000000"}.
     *
     * <pre>
     *   "20240315"   → LocalDate.of(2024, 3, 15)
     *   "00000000"   → null
     *   ""           → null
     *   null         → null
     * </pre>
     *
     * @throws IllegalArgumentException if the value is non-null, non-empty, non-zero and unparseable
     */
    public static LocalDate parseSapDate(String value) {
        if (value == null || value.isBlank() || SAP_ZERO_DATE.equals(value.trim())) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), SAP_DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Cannot parse SAP date value: '" + value + "'", ex);
        }
    }

    /**
     * Parse a SAP amount string to {@link BigDecimal}.
     * Handles optional leading/trailing whitespace and sign characters.
     * Returns {@code null} for {@code null} or blank input.
     *
     * <pre>
     *   "1234.56"   → BigDecimal("1234.56")
     *   "-99.00"    → BigDecimal("-99.00")
     *   "0.00"      → BigDecimal("0.00")
     *   ""          → null
     *   null        → null
     * </pre>
     *
     * @throws IllegalArgumentException if the value is non-null, non-blank and not a valid decimal
     */
    public static BigDecimal parseSapAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Cannot parse SAP amount value: '" + value + "'", ex);
        }
    }

    /**
     * Trim whitespace padding from a SAP string field.
     * Returns {@code null} for {@code null} input; returns an empty string if the value is all spaces.
     *
     * <pre>
     *   "  DE  "  → "DE"
     *   "EUR"     → "EUR"
     *   "   "     → ""
     *   null      → null
     * </pre>
     */
    public static String trimSapString(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }
}
