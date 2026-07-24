package com.ficabridge.controller.outbound;

import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * GET /api/invoices
     * Optional query params: contractAccount, status (combinable).
     * Paged via standard Spring Data {@code page}/{@code size}/{@code sort} params; page size
     * defaults to 50 and is capped at 200 (see spring.data.web.pageable.* in application.yml).
     * Returns a {@link PagedModel} so the JSON carries stable {@code content} + {@code page}
     * metadata rather than the version-unstable raw {@code Page} serialization.
     */
    @GetMapping
    public ResponseEntity<PagedModel<InvoiceDTO>> getInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String contractAccount,
            Pageable pageable) {

        Page<InvoiceDTO> result;
        if (contractAccount != null && status != null) {
            result = invoiceService.getByContractAccountAndStatus(contractAccount, status, pageable);
        } else if (contractAccount != null) {
            result = invoiceService.getByContractAccount(contractAccount, pageable);
        } else if (status != null) {
            result = invoiceService.getByStatus(status, pageable);
        } else {
            result = invoiceService.getAll(pageable);
        }
        return ResponseEntity.ok(new PagedModel<>(result));
    }

    /** GET /api/invoices/{invoiceNumber} */
    @GetMapping("/{invoiceNumber}")
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(invoiceService.getByInvoiceNumber(invoiceNumber));
    }
}
