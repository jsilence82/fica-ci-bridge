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
@SuppressWarnings("unused") // fields used via Lombok constructor injection; methods are stubs
public class InvoiceController {

    private final InvoiceService invoiceService;

    /** GET /api/invoices?status=OPEN&contractAccount=100000789 */
    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String contractAccount) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /** GET /api/invoices/{billingDocNumber} */
    @GetMapping("/{billingDocNumber}")
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable String billingDocNumber) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
