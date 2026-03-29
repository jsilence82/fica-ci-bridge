package com.ficabridge.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FicaDocumentDTO {

    private String documentNumber;
    private String itemNumber;
    private String contractAccount;
    private String businessPartner;
    private LocalDate documentDate;
    private LocalDate postingDate;
    private LocalDate dueDate;
    private LocalDate clearingDate;
    private InvoiceStatus status;
    private BigDecimal amount;
    private String currency;
    private String conditionType;
    private String documentType;
}
