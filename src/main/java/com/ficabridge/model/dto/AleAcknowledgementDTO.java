package com.ficabridge.model.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * SAP ALE acknowledgement response — must be returned as XML after receiving an IDoc.
 * SAP ALE will retry delivery if it does not receive this response.
 */
@Data
@XmlRootElement(name = "ALEAUD01")
@XmlAccessorType(XmlAccessType.FIELD)
public class AleAcknowledgementDTO {

    @XmlElement(name = "DOCNUM")
    private String docnum;   // IDoc document number being acknowledged

    @XmlElement(name = "STATUS")
    private String status;   // Processing status code

    @XmlElement(name = "MESTYP")
    private String mestyp;   // Original message type
}
