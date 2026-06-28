package com.ficabridge.controller.outbound;

import com.ficabridge.model.dto.ContractAccountDTO;
import com.ficabridge.service.ContractAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contract-accounts")
@RequiredArgsConstructor
public class ContractAccountController {

    private final ContractAccountService contractAccountService;

    /** GET /api/contract-accounts/overdue — must be declared before /{contractAccount} */
    @GetMapping("/overdue")
    public ResponseEntity<List<ContractAccountDTO>> getOverdue() {
        return ResponseEntity.ok(contractAccountService.getAllWithOverdueItems());
    }

    /** GET /api/contract-accounts/{contractAccount} */
    @GetMapping("/{contractAccount}")
    public ResponseEntity<ContractAccountDTO> getContractAccount(@PathVariable String contractAccount) {
        return ResponseEntity.ok(contractAccountService.getByContractAccount(contractAccount));
    }
}
