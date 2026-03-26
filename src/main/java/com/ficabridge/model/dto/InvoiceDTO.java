package com.ficabridge.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InvoiceDTO {

    private String billingDocNumber;
    private String businessPartner;
    private String contractAccount;
    private String contractReference;
    private LocalDate dueDate;
    private BigDecimal amount;
    private String currency;
    private InvoiceStatus status;
    private LocalDate clearingDate;
    private String ficaDocNumber;
    private String idocDocnum;
}
