package com.ficabridge.service;

import com.ficabridge.exception.ResourceNotFoundException;
import com.ficabridge.mapper.InvoiceMapper;
import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Reads are @Transactional(readOnly = true) so the persistence context stays open while the mapper
// walks the lazy InvoiceEntity.lineItems association — Open-Session-In-View is deliberately off
// (see spring.jpa.open-in-view in application.yml), so without this the mapping would throw
// LazyInitializationException.
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;

    public InvoiceDTO getByInvoiceNumber(String invoiceNumber) {
        InvoiceEntity entity = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceNumber));
        return invoiceMapper.toDto(entity);
    }

    public Page<InvoiceDTO> getAll(Pageable pageable) {
        return invoiceRepository.findAll(pageable).map(invoiceMapper::toDto);
    }

    public Page<InvoiceDTO> getByContractAccount(String contractAccount, Pageable pageable) {
        return invoiceRepository.findByContractAccount(contractAccount, pageable).map(invoiceMapper::toDto);
    }

    public Page<InvoiceDTO> getByStatus(InvoiceStatus status, Pageable pageable) {
        return invoiceRepository.findByStatus(status, pageable).map(invoiceMapper::toDto);
    }

    public Page<InvoiceDTO> getByContractAccountAndStatus(String contractAccount, InvoiceStatus status, Pageable pageable) {
        return invoiceRepository.findByContractAccountAndStatus(contractAccount, status, pageable).map(invoiceMapper::toDto);
    }

    @Transactional
    public InvoiceEntity save(InvoiceEntity entity) {
        return invoiceRepository.save(entity);
    }
}
