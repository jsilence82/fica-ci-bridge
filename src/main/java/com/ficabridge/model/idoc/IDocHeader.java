package com.ficabridge.model.idoc;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

/**
 * EDI_DC40 — IDoc control record header segment, present in all IDocs.
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class IDocHeader {

    @XmlElement(name = "DOCNUM")
    private String docnum;   // IDoc document number — used for idempotency

    @XmlElement(name = "MESTYP")
    private String mestyp;   // Message type, e.g. INVOIC

    @XmlElement(name = "CREDAT")
    private String credat;   // Created date, SAP format YYYYMMDD
}
