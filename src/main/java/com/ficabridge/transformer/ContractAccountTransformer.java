package com.ficabridge.transformer;

import com.ficabridge.model.dto.ContractAccountDTO;
import com.ficabridge.model.odata.ODataContractAccount;
import org.springframework.stereotype.Component;

/**
 * Transforms a FI-CA contract account OData response into a {@link ContractAccountDTO}.
 * The {@code invoices} list on the DTO is populated by the service layer, not here.
 */
@Component
public class ContractAccountTransformer {

    public ContractAccountDTO transform(ODataContractAccount source) {
        ContractAccountDTO dto = new ContractAccountDTO();
        dto.setContractAccount(TransformerUtils.stripLeadingZeros(source.getContractAccount()));
        dto.setBusinessPartner(TransformerUtils.stripLeadingZeros(source.getBusinessPartner()));
        return dto;
    }
}
