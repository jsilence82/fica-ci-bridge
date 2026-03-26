package com.ficabridge.controller.inbound;

import com.ficabridge.model.dto.AleAcknowledgementDTO;
import com.ficabridge.model.idoc.BillingIDoc;
import com.ficabridge.service.IdocProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives SAP IDoc XML posts via ALE dispatch.
 * SAP expects an ALE acknowledgement response — absence will trigger retries.
 */
@RestController
@RequestMapping("/api/idoc")
@RequiredArgsConstructor
@SuppressWarnings("unused") // fields used via Lombok constructor injection; methods are stubs
public class IDocInboundController {

    private final IdocProcessingService idocProcessingService;

    @PostMapping(
            value = "/billing",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE
    )
    public ResponseEntity<AleAcknowledgementDTO> receiveBillingIDoc(@RequestBody BillingIDoc billingIDoc) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
