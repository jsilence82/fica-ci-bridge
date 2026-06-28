package com.ficabridge.service;

import com.ficabridge.exception.ResourceNotFoundException;
import com.ficabridge.mapper.ContractAccountMapper;
import com.ficabridge.mapper.InvoiceMapper;
import com.ficabridge.model.dto.ContractAccountDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.ContractAccountEntity;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.repository.ContractAccountRepository;
import com.ficabridge.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ContractAccountService {

    private final ContractAccountRepository contractAccountRepository;
    private final InvoiceRepository invoiceRepository;
    private final ContractAccountMapper contractAccountMapper;
    private final InvoiceMapper invoiceMapper;

    public ContractAccountDTO getByContractAccount(String contractAccount) {
        ContractAccountEntity entity = contractAccountRepository.findByContractAccount(contractAccount)
                .orElseThrow(() -> new ResourceNotFoundException("ContractAccount not found: " + contractAccount));
        ContractAccountDTO dto = contractAccountMapper.toDto(entity);
        dto.setInvoices(invoiceMapper.toDtoList(invoiceRepository.findByContractAccount(contractAccount)));
        return dto;
    }

    public List<ContractAccountDTO> getAllWithOverdueItems() {
        return contractAccountRepository.findAll().stream()
                .map(ca -> {
                    List<InvoiceEntity> overdue = invoiceRepository.findByContractAccountAndStatus(
                            ca.getContractAccount(), InvoiceStatus.OVERDUE);
                    if (overdue.isEmpty()) {
                        return null;
                    }
                    ContractAccountDTO dto = contractAccountMapper.toDto(ca);
                    dto.setInvoices(invoiceMapper.toDtoList(overdue));
                    return dto;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public ContractAccountEntity save(ContractAccountEntity entity) {
        return contractAccountRepository.save(entity);
    }
}
