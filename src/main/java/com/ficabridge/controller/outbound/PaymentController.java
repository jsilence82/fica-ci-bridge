package com.ficabridge.controller.outbound;

import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.service.OpenItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final OpenItemService openItemService;

    /**
     * GET /api/payments
     * Optional query param: contractAccount — filters to open items for a single CA.
     */
    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getOpenItems(
            @RequestParam(required = false) String contractAccount) {

        List<InvoiceDTO> result = (contractAccount != null)
                ? openItemService.getOpenItemsByContractAccount(contractAccount)
                : openItemService.getAllOpenItems();
        return ResponseEntity.ok(result);
    }
}
