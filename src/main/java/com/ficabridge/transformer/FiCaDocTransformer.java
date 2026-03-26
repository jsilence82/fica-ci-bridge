package com.ficabridge.transformer;

import com.ficabridge.model.entity.ContractAccountEntity;
import com.ficabridge.model.idoc.FiCaDocIDoc;
import org.springframework.stereotype.Component;

/**
 * Transforms a FI-CA document IDoc into a ContractAccountEntity.
 */
@Component
public class FiCaDocTransformer {

    /**
     * Transform a parsed FiCaDocIDoc into a ContractAccountEntity ready for persistence.
     * Handles: leading zero stripping and whitespace trimming on VKONT and GPART.
     */
    public ContractAccountEntity transform(FiCaDocIDoc idoc) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
