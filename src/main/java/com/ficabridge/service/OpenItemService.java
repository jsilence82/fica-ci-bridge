package com.ficabridge.service;

import com.ficabridge.mapper.InvoiceMapper;
import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Reads are @Transactional(readOnly = true) so the mapper can walk each invoice's lazy line items
// with Open-Session-In-View off (see spring.jpa.open-in-view in application.yml).
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OpenItemService {

    /** An "open item" is any receivable not yet cleared — OPEN or OVERDUE. */
    private static final List<InvoiceStatus> OPEN_ITEM_STATUSES =
            List.of(InvoiceStatus.OPEN, InvoiceStatus.OVERDUE);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;

    public Page<InvoiceDTO> getOpenItemsByContractAccount(String contractAccount, Pageable pageable) {
        return invoiceRepository.findByContractAccountAndStatusIn(contractAccount, OPEN_ITEM_STATUSES, pageable)
                .map(invoiceMapper::toDto);
    }

    public Page<InvoiceDTO> getAllOpenItems(Pageable pageable) {
        return invoiceRepository.findByStatusIn(OPEN_ITEM_STATUSES, pageable)
                .map(invoiceMapper::toDto);
    }
}
