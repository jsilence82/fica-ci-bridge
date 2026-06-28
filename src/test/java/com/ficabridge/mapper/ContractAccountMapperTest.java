package com.ficabridge.mapper;

import com.ficabridge.model.dto.ContractAccountDTO;
import com.ficabridge.model.entity.ContractAccountEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {ContractAccountMapperImpl.class})
class ContractAccountMapperTest {

    @Autowired ContractAccountMapper contractAccountMapper;

    @Test
    void toDto_mapsFieldsAndLeavesInvoicesNull() {
        ContractAccountEntity entity = new ContractAccountEntity();
        entity.setContractAccount("200001");
        entity.setBusinessPartner("100001");

        ContractAccountDTO dto = contractAccountMapper.toDto(entity);

        assertThat(dto.getContractAccount()).isEqualTo("200001");
        assertThat(dto.getBusinessPartner()).isEqualTo("100001");
        assertThat(dto.getInvoices()).isNull();
    }

    @Test
    void toEntity_mapsFields_ignoresId() {
        ContractAccountDTO dto = new ContractAccountDTO();
        dto.setContractAccount("200001");
        dto.setBusinessPartner("100001");

        ContractAccountEntity entity = contractAccountMapper.toEntity(dto);

        assertThat(entity.getContractAccount()).isEqualTo("200001");
        assertThat(entity.getBusinessPartner()).isEqualTo("100001");
        assertThat(entity.getId()).isNull();
    }
}
