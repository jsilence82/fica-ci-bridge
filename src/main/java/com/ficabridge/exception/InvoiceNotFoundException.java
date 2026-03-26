package com.ficabridge.exception;

public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(String billingDocNumber) {
        super("Invoice not found: " + billingDocNumber);
    }
}
