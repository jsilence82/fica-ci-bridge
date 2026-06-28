package com.ficabridge.integration;

import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.InvoiceEntity;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InvoiceIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private InvoiceRepository invoiceRepository;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        invoiceRepository.save(invoice("IDOC001", "90001001", "200001", "100001",
                InvoiceStatus.OPEN, "143.40", "EUR", LocalDate.now().plusDays(30)));
        invoiceRepository.save(invoice("IDOC002", "90001002", "200001", "100001",
                InvoiceStatus.OVERDUE, "98.00", "EUR", LocalDate.now().minusDays(10)));
        invoiceRepository.save(invoice("IDOC003", "90002001", "200002", "100002",
                InvoiceStatus.CLEARED, "220.00", "EUR", LocalDate.now().minusDays(60)));
    }

    // ── GET /api/invoices ─────────────────────────────────────────────────────

    @Test
    void getInvoices_noFilter_returnsAllSeededInvoices() {
        ResponseEntity<Object[]> response = restTemplate.getForEntity("/api/invoices", Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
    }

    @Test
    void getInvoices_byContractAccount_returnsOnlyMatchingInvoices() {
        ResponseEntity<Object[]> response =
                restTemplate.getForEntity("/api/invoices?contractAccount=200001", Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getInvoices_byStatus_returnsOnlyMatchingStatus() {
        ResponseEntity<Object[]> response =
                restTemplate.getForEntity("/api/invoices?status=OVERDUE", Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getInvoices_byContractAccountAndStatus_returnsIntersection() {
        ResponseEntity<Object[]> response = restTemplate.getForEntity(
                "/api/invoices?contractAccount=200001&status=OPEN", Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getInvoices_noMatchingContractAccount_returnsEmptyArray() {
        ResponseEntity<Object[]> response =
                restTemplate.getForEntity("/api/invoices?contractAccount=UNKNOWN", Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ── GET /api/invoices/{billingDocNumber} ──────────────────────────────────

    @SuppressWarnings("unchecked")
    @Test
    void getInvoice_found_returns200WithCorrectFields() {
        ResponseEntity<Map<String, Object>> response =
                restTemplate.getForEntity("/api/invoices/90001001", (Class<Map<String, Object>>) (Class<?>) Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("billingDocNumber", "90001001")
                .containsEntry("contractAccount", "200001")
                .containsEntry("status", "OPEN")
                .containsEntry("currency", "EUR");
    }

    @SuppressWarnings("unchecked")
    @Test
    void getInvoice_notFound_returns404WithErrorField() {
        ResponseEntity<Map<String, Object>> response =
                restTemplate.getForEntity("/api/invoices/BADID", (Class<Map<String, Object>>) (Class<?>) Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("error");
    }

    // ── GET /api/payments ─────────────────────────────────────────────────────

    @Test
    void getPayments_noFilter_returnsOpenAndOverdueOnly() {
        ResponseEntity<Object[]> response = restTemplate.getForEntity("/api/payments", Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // CLEARED invoice must not be included
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getPayments_byContractAccount_returnsFilteredOpenItems() {
        ResponseEntity<Object[]> response =
                restTemplate.getForEntity("/api/payments?contractAccount=200001", Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getPayments_contractAccountWithNoOpenItems_returnsEmptyArray() {
        ResponseEntity<Object[]> response =
                restTemplate.getForEntity("/api/payments?contractAccount=200002", Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // CA 200002 only has a CLEARED invoice — no open items
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
        return e;
    }
}
