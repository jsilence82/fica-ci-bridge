package com.ficabridge.transformer;

import com.ficabridge.model.dto.ContractAccountDTO;
import com.ficabridge.model.odata.ODataContractAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractAccountTransformerTest {

    private ContractAccountTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new ContractAccountTransformer();
    }

    @Test
    void transform_stripsLeadingZerosFromContractAccountAndBusinessPartner() {
        ODataContractAccount source = contractAccount("0000100200", "0000005678");

        ContractAccountDTO dto = transformer.transform(source);

        assertThat(dto.getContractAccount()).isEqualTo("100200");
        assertThat(dto.getBusinessPartner()).isEqualTo("5678");
    }

    @Test
    void transform_noLeadingZeros_returnsUnchanged() {
        ODataContractAccount source = contractAccount("100200", "5678");

        ContractAccountDTO dto = transformer.transform(source);

        assertThat(dto.getContractAccount()).isEqualTo("100200");
        assertThat(dto.getBusinessPartner()).isEqualTo("5678");
    }

    @Test
    void transform_nullFields_areNull() {
        ODataContractAccount source = contractAccount(null, null);

        ContractAccountDTO dto = transformer.transform(source);

        assertThat(dto.getContractAccount()).isNull();
        assertThat(dto.getBusinessPartner()).isNull();
    }

    @Test
    void transform_allZeroContractAccount_returnsSingleZero() {
        ODataContractAccount source = contractAccount("0000000000", "0000000001");

        ContractAccountDTO dto = transformer.transform(source);

        assertThat(dto.getContractAccount()).isEqualTo("0");
        assertThat(dto.getBusinessPartner()).isEqualTo("1");
    }

    @Test
    void transform_invoicesListIsNull() {
        // Invoices are populated by the service layer, never by the transformer
        ContractAccountDTO dto = transformer.transform(contractAccount("0000000001", "0000000001"));

        assertThat(dto.getInvoices()).isNull();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ODataContractAccount contractAccount(String contractAccount, String businessPartner) {
        ODataContractAccount ca = new ODataContractAccount();
        ca.setContractAccount(contractAccount);
        ca.setBusinessPartner(businessPartner);
        return ca;
    }
}
