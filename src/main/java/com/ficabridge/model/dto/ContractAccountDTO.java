package com.ficabridge.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ContractAccountDTO {

    private String contractAccount;
    private String businessPartner;
    private List<InvoiceDTO> invoices;  // Populated by service, not mapped from entity
}
