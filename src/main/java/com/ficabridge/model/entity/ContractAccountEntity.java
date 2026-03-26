package com.ficabridge.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "contract_accounts")
public class ContractAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String contractAccount;  // E1INVDO.VKONT / E1FICA01.VKONT, leading zeros stripped

    @Column(nullable = false)
    private String businessPartner;  // E1INVDO.GPART / E1FICA01.GPART, leading zeros stripped
}
