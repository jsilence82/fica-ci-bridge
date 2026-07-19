package com.ficabridge.sync;

import com.ficabridge.model.dto.InvoiceStatus;

import java.time.LocalDate;

/**
 * A detected clearing-status transition for a cached invoice, keyed by the
 * {@code billingDocNumber} (CI invoicing document) this bridge uses as its natural key —
 * not the underlying FI-CA document number the sync source data is keyed by.
 */
public record DocumentChange(
        String billingDocNumber,
        String contractAccount,
        InvoiceStatus newStatus,
        LocalDate clearingDate) {
}
