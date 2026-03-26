package com.ficabridge.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentDTO {

    private String paymentDocNumber;
    private String contractAccount;
    private BigDecimal amount;
    private String currency;
    private LocalDate paymentDate;
    private String paymentMethod;
}
