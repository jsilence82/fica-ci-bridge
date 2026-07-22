package com.ficabridge.controller.outbound;

import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.service.OpenItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final OpenItemService openItemService;

    /**
     * GET /api/payments
     * Optional query param: contractAccount — filters to open items for a single CA.
     * Paged via standard {@code page}/{@code size}/{@code sort} params (default size 50, capped at
     * 200); returns a {@link PagedModel} with the same stable {@code content} + {@code page} shape
     * as GET /api/invoices.
     */
    @GetMapping
    public ResponseEntity<PagedModel<InvoiceDTO>> getOpenItems(
            @RequestParam(required = false) String contractAccount,
            Pageable pageable) {

        Page<InvoiceDTO> result = (contractAccount != null)
                ? openItemService.getOpenItemsByContractAccount(contractAccount, pageable)
                : openItemService.getAllOpenItems(pageable);
        return ResponseEntity.ok(new PagedModel<>(result));
    }
}
