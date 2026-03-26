package com.ficabridge.model.idoc;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.List;

/**
 * Root JAXB model for a Convergent Invoicing billing IDoc received via SAP ALE.
 * Segments: EDI_DC40 (header), E1INVDO (billing doc header), E1INVIO (line items).
 */
@Data
@XmlRootElement(name = "IDOC")
@XmlAccessorType(XmlAccessType.FIELD)
public class BillingIDoc {

    @XmlElement(name = "EDI_DC40")
    private IDocHeader header;

    @XmlElement(name = "E1INVDO")
    private BillingDocSegment billingDoc;

    @XmlElement(name = "E1INVIO")
    private List<LineItemSegment> lineItems;

    /**
     * E1INVDO — Convergent Invoicing billing document header segment.
     */
    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class BillingDocSegment {

        @XmlElement(name = "VBELN")
        private String vbeln;   // Billing document number (CI)

        @XmlElement(name = "GPART")
        private String gpart;   // Business partner number — NOT the contract account

        @XmlElement(name = "VKONT")
        private String vkont;   // Contract account number (FI-CA)

        @XmlElement(name = "VTREF")
        private String vtref;   // Contract reference

        @XmlElement(name = "FAEDN")
        private String faedn;   // Due date, SAP format YYYYMMDD

        @XmlElement(name = "BETRW")
        private String betrw;   // Amount in document currency

        @XmlElement(name = "WAERS")
        private String waers;   // Currency key, e.g. USD

        @XmlElement(name = "AUGST")
        private String augst;   // Clearing status — see InvoiceStatus mapping

        @XmlElement(name = "AUGDT")
        private String augdt;   // Clearing date — "00000000" if not cleared

        @XmlElement(name = "OPBEL")
        private String opbel;   // FI-CA document number linking CI doc to open item
    }

    /**
     * E1INVIO — Convergent Invoicing billing document line item segment.
     */
    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LineItemSegment {

        @XmlElement(name = "POSNR")
        private String posnr;   // Line item number

        @XmlElement(name = "KSCHL")
        private String kschl;   // Condition type, e.g. ZCI1, ZCI2, ZTAX

        @XmlElement(name = "BETRW")
        private String betrw;   // Line item amount

        @XmlElement(name = "MWSKZ")
        private String mwskz;   // Tax code

        @XmlElement(name = "HWBAS")
        private String hwbas;   // Tax base amount

        @XmlElement(name = "TXJCD")
        private String txjcd;   // Tax jurisdiction code
    }
}
