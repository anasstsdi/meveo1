//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.02.24 at 08:41:49 PM CST 
//


package org.meveo.asg.api.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for OfferPricePlanUpdated complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OfferPricePlanUpdated">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="OfferId" type="{http://microsoft.com/wsdl/types/}guid"/>
 *         &lt;element name="PricePlan" type="{}PricePlanEventData" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OfferPricePlanUpdated", propOrder = {
    "offerId",
    "pricePlan"
})
public class OfferPricePlanUpdated {

    @XmlElement(name = "OfferId", required = true)
    protected String offerId;
    @XmlElement(name = "PricePlan")
    protected PricePlanEventData pricePlan;

    /**
     * Gets the value of the offerId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOfferId() {
        return offerId;
    }

    /**
     * Sets the value of the offerId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOfferId(String value) {
        this.offerId = value;
    }

    /**
     * Gets the value of the pricePlan property.
     * 
     * @return
     *     possible object is
     *     {@link PricePlanEventData }
     *     
     */
    public PricePlanEventData getPricePlan() {
        return pricePlan;
    }

    /**
     * Sets the value of the pricePlan property.
     * 
     * @param value
     *     allowed object is
     *     {@link PricePlanEventData }
     *     
     */
    public void setPricePlan(PricePlanEventData value) {
        this.pricePlan = value;
    }

}
