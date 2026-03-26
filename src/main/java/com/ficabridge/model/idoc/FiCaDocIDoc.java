package com.ficabridge.model.idoc;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * Root JAXB model for a FI-CA document IDoc received via SAP ALE.
 * Carries contract account details and open item information.
 */
@Data
@XmlRootElement(name = "IDOC")
@XmlAccessorType(XmlAccessType.FIELD)
public class FiCaDocIDoc {

    @XmlElement(name = "EDI_DC40")
    private IDocHeader header;

    @XmlElement(name = "E1FICA01")
    private FiCaDocSegment ficaDoc;

    /**
     * E1FICA01 — FI-CA document segment carrying contract account data.
     */
    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FiCaDocSegment {

        @XmlElement(name = "VKONT")
        private String vkont;   // Contract account number

        @XmlElement(name = "GPART")
        private String gpart;   // Business partner number

        @XmlElement(name = "OPBEL")
        private String opbel;   // FI-CA open item document number

        @XmlElement(name = "AUGST")
        private String augst;   // Clearing status

        @XmlElement(name = "AUGDT")
        private String augdt;   // Clearing date, SAP format YYYYMMDD

        @XmlElement(name = "BETRW")
        private String betrw;   // Amount

        @XmlElement(name = "WAERS")
        private String waers;   // Currency key
    }
}
