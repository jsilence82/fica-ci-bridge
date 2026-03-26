package com.ficabridge.repository;

import com.ficabridge.model.entity.ContractAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractAccountRepository extends JpaRepository<ContractAccountEntity, Long> {

    Optional<ContractAccountEntity> findByContractAccount(String contractAccount);

    boolean existsByContractAccount(String contractAccount);
}
