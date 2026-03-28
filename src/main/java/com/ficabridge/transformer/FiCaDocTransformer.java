package com.ficabridge.transformer;

import org.springframework.stereotype.Component;

/**
 * Transforms a FI-CA document OData response into a ContractAccountDTO.
 * Input and output types will be wired in Step 2 / Step 4.
 */
@Component
public class FiCaDocTransformer {
    // transform(ODataFicaDocument) → FicaDocumentDTO  — implemented in Step 4
}
