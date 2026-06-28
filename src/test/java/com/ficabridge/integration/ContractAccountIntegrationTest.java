package com.ficabridge.integration;

import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.ContractAccountEntity;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.repository.ContractAccountRepository;
import com.ficabridge.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContractAccountIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ContractAccountRepository contractAccountRepository;
    @Autowired private InvoiceRepository invoiceRepository;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        contractAccountRepository.deleteAll();

        ContractAccountEntity ca1 = new ContractAccountEntity();
        ca1.setContractAccount("200001");
        ca1.setBusinessPartner("100001");
        contractAccountRepository.save(ca1);

        ContractAccountEntity ca2 = new ContractAccountEntity();
        ca2.setContractAccount("200002");
        ca2.setBusinessPartner("100002");
        contractAccountRepository.save(ca2);

        invoiceRepository.save(invoice("IDOC001", "90001001", "200001", "100001",
                InvoiceStatus.OVERDUE, "143.40", "EUR", LocalDate.now().minusDays(10)));
        invoiceRepository.save(invoice("IDOC002", "90001002", "200001", "100001",
                InvoiceStatus.OPEN, "98.00", "EUR", LocalDate.now().plusDays(15)));
        invoiceRepository.save(invoice("IDOC003", "90002001", "200002", "100002",
                InvoiceStatus.CLEARED, "220.00", "EUR", LocalDate.now().minusDays(60)));
    }

    // ── GET /api/contract-accounts/{contractAccount} ──────────────────────────

    @SuppressWarnings("unchecked")
    @Test
    void getContractAccount_found_returns200WithInvoices() {
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
                "/api/contract-accounts/200001",
                (Class<Map<String, Object>>) (Class<?>) Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("contractAccount", "200001")
                .containsEntry("businessPartner", "100001");
    }

    @SuppressWarnings("unchecked")
    @Test
    void getContractAccount_notFound_returns404() {
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
                "/api/contract-accounts/UNKNOWN",
                (Class<Map<String, Object>>) (Class<?>) Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("error");
    }

    // ── GET /api/contract-accounts/overdue ────────────────────────────────────

    @Test
    void getOverdue_returnsOnlyAccountsWithOverdueInvoices() {
        ResponseEntity<Object[]> response =
                restTemplate.getForEntity("/api/contract-accounts/overdue", Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // CA 200001 has an OVERDUE invoice; CA 200002 only has CLEARED — not included
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getOverdue_whenNoOverdueInvoices_returnsEmptyArray() {
        invoiceRepository.deleteAll();

        ResponseEntity<Object[]> response =
                restTemplate.getForEntity("/api/contract-accounts/overdue", Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private InvoiceEntity invoice(String idocDocnum, String billingDocNumber,
                                  String contractAccount, String businessPartner,
                                  InvoiceStatus status, String amount, String currency,
                                  LocalDate dueDate) {
        InvoiceEntity e = new InvoiceEntity();
        e.setIdocDocnum(idocDocnum);
        e.setBillingDocNumber(billingDocNumber);
        e.setContractAccount(contractAccount);
        e.setBusinessPartner(businessPartner);
        e.setStatus(status);
        e.setAmount(new BigDecimal(amount));
        e.setCurrency(currency);
        e.setDueDate(dueDate);
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }
}
