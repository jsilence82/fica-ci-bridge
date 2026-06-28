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
    private String itemNumber;

    private String description;

    private String material;

    @Column(precision = 19, scale = 3)
    private BigDecimal quantity;

    private String quantityUnit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(length = 5)
    private String currency;

    @Column(length = 10)
    private String chargingCategory;
}
