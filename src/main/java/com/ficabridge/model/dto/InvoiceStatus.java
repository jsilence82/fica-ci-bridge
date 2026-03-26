package com.ficabridge.model.dto;

public enum InvoiceStatus {
    OPEN,           // Not yet paid, not past due
    CLEARED,        // Fully paid / cleared in FI-CA
    PARTIALLY_PAID, // Partial clearing exists
    OVERDUE,        // Past due date, not cleared (derived, not from SAP)
    REVERSED        // Document reversed
}
