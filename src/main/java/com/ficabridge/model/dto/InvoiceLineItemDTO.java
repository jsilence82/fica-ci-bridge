package com.ficabridge.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceLineItemDTO {

    private String itemNumber;
    private String description;
    private String material;
    private BigDecimal quantity;
    private String quantityUnit;
    private BigDecimal netAmount;
    private BigDecimal taxAmount;
    private String currency;
    private String chargingCategory;
}
