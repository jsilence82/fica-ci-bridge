package com.ficabridge.controller.outbound;

import com.ficabridge.model.dto.ContractAccountDTO;
import com.ficabridge.service.ContractAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contract-accounts")
@RequiredArgsConstructor
public class ContractAccountController {

    private final ContractAccountService contractAccountService;

    /**
     * GET /api/contract-accounts/overdue — must be declared before /{contractAccount}.
     * Paged via standard {@code page}/{@code size}/{@code sort} params (default size 50, capped at
     * 200); returns a {@link PagedModel} with the same {@code content} + {@code page} shape as the
     * other list endpoints.
     */
    @GetMapping("/overdue")
    public ResponseEntity<PagedModel<ContractAccountDTO>> getOverdue(Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(contractAccountService.getAllWithOverdueItems(pageable)));
    }

    /** GET /api/contract-accounts/{contractAccount} */
    @GetMapping("/{contractAccount}")
    public ResponseEntity<ContractAccountDTO> getContractAccount(@PathVariable String contractAccount) {
        return ResponseEntity.ok(contractAccountService.getByContractAccount(contractAccount));
    }
}
