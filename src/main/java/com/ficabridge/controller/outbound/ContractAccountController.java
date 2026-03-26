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
@SuppressWarnings("unused") // fields used via Lombok constructor injection; methods are stubs
public class ContractAccountController {

    private final ContractAccountService contractAccountService;

    /** GET /api/contract-accounts/{contractAccount} */
    @GetMapping("/{contractAccount}")
    public ResponseEntity<ContractAccountDTO> getContractAccount(@PathVariable String contractAccount) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /** GET /api/contract-accounts/overdue */
    @GetMapping("/overdue")
    public ResponseEntity<List<ContractAccountDTO>> getOverdue() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
