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
import java.util.List;
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

        invoiceRepository.save(invoice("90001001", "200001", "100001",
                InvoiceStatus.OVERDUE, "143.40", "EUR", LocalDate.now().minusDays(10)));
        invoiceRepository.save(invoice("90001002", "200001", "100001",
                InvoiceStatus.OPEN, "98.00", "EUR", LocalDate.now().plusDays(15)));
        invoiceRepository.save(invoice("90002001", "200002", "100002",
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

    // ── GET /api/contract-accounts/overdue (paged) ────────────────────────────

    @Test
    void getOverdue_returnsOnlyAccountsWithOverdueInvoices() {
        Map<String, Object> body = getPaged("/api/contract-accounts/overdue");

        // CA 200001 has an OVERDUE invoice; CA 200002 only has CLEARED — not included
        assertThat(content(body)).hasSize(1);
        assertThat(pageMeta(body).get("totalElements")).isEqualTo(1);
    }

    @Test
    void getOverdue_whenNoOverdueInvoices_returnsEmptyContent() {
        invoiceRepository.deleteAll();

        assertThat(content(getPaged("/api/contract-accounts/overdue"))).isEmpty();
    }

    @Test
    void getOverdue_responseIncludesPagingMetadata() {
        Map<String, Object> page = pageMeta(getPaged("/api/contract-accounts/overdue"));

        assertThat(page.get("number")).isEqualTo(0);
        assertThat(page.get("size")).isEqualTo(50);
    }

    @Test
    void getOverdue_sizeAboveMax_isClampedTo200() {
        assertThat(pageMeta(getPaged("/api/contract-accounts/overdue?size=9999")).get("size"))
                .isEqualTo(200);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPaged(String url) {
        ResponseEntity<Map<String, Object>> response =
                restTemplate.getForEntity(url, (Class<Map<String, Object>>) (Class<?>) Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> content(Map<String, Object> body) {
        return (List<Object>) body.get("content");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> pageMeta(Map<String, Object> body) {
        return (Map<String, Object>) body.get("page");
    }

    private InvoiceEntity invoice(String invoiceNumber,
                                  String contractAccount, String businessPartner,
                                  InvoiceStatus status, String amount, String currency,
                                  LocalDate dueDate) {
        InvoiceEntity e = new InvoiceEntity();
        e.setInvoiceNumber(invoiceNumber);
        e.setContractAccount(contractAccount);
        e.setBusinessPartner(businessPartner);
        e.setStatus(status);
        e.setAmount(new BigDecimal(amount));
        e.setCurrency(currency);
        e.setDueDate(dueDate);
        return e;
    }
}
