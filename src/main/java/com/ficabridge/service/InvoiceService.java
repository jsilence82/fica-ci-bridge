package com.ficabridge.service;

import com.ficabridge.exception.ResourceNotFoundException;
import com.ficabridge.mapper.InvoiceMapper;
import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;

    public InvoiceDTO getByBillingDocNumber(String billingDocNumber) {
        InvoiceEntity entity = invoiceRepository.findByBillingDocNumber(billingDocNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + billingDocNumber));
        return invoiceMapper.toDto(entity);
    }

    public List<InvoiceDTO> getAll() {
        return invoiceMapper.toDtoList(invoiceRepository.findAll());
    }

    public List<InvoiceDTO> getByContractAccount(String contractAccount) {
        return invoiceMapper.toDtoList(invoiceRepository.findByContractAccount(contractAccount));
    }

    public List<InvoiceDTO> getByStatus(InvoiceStatus status) {
        return invoiceMapper.toDtoList(invoiceRepository.findByStatus(status));
    }

    public List<InvoiceDTO> getByContractAccountAndStatus(String contractAccount, InvoiceStatus status) {
        return invoiceMapper.toDtoList(invoiceRepository.findByContractAccountAndStatus(contractAccount, status));
    }

    public InvoiceEntity save(InvoiceEntity entity) {
        return invoiceRepository.save(entity);
    }

    public boolean existsByIdocDocnum(String idocDocnum) {
        return invoiceRepository.existsByIdocDocnum(idocDocnum);
    }
}
