package com.ficabridge.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Column(nullable = false)
    private String lineItemNumber;       // E1INVIO.POSNR

    @Column(nullable = false, length = 10)
    private String conditionType;        // E1INVIO.KSCHL, e.g. ZCI1, ZCI2, ZTAX

    private String chargeType;           // Human-readable label derived from KSCHL

    private String taxCode;              // E1INVIO.MWSKZ

    @Column(precision = 19, scale = 2)
    private BigDecimal taxBaseAmount;    // E1INVIO.HWBAS

    private String taxJurisdictionCode;  // E1INVIO.TXJCD

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;           // E1INVIO.BETRW
}
