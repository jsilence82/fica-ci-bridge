package com.ficabridge.controller.outbound;

import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * GET /api/invoices
     * Optional query params: contractAccount, status (combinable).
     */
    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String contractAccount) {

        List<InvoiceDTO> result;
        if (contractAccount != null && status != null) {
            result = invoiceService.getByContractAccountAndStatus(contractAccount, status);
        } else if (contractAccount != null) {
            result = invoiceService.getByContractAccount(contractAccount);
        } else if (status != null) {
            result = invoiceService.getByStatus(status);
        } else {
            result = invoiceService.getAll();
        }
        return ResponseEntity.ok(result);
    }

    /** GET /api/invoices/{billingDocNumber} */
    @GetMapping("/{billingDocNumber}")
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable String billingDocNumber) {
        return ResponseEntity.ok(invoiceService.getByBillingDocNumber(billingDocNumber));
    }
}
