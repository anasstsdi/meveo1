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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ChargeBaseData complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ChargeBaseData">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Prices" type="{}ArrayOfChargePriceData" minOccurs="0"/>
 *         &lt;element name="Descriptions" type="{}ArrayOfItemDescriptionData" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChargeBaseData", propOrder = {
    "prices",
    "descriptions"
})
@XmlSeeAlso({
    SubscriptionChargeData.class,
    TerminationChargeData.class,
    RecurringChargeData.class,
    UsageChargeData.class
})
public abstract class ChargeBaseData {

    @XmlElement(name = "Prices")
    protected ArrayOfChargePriceData prices;
    @XmlElement(name = "Descriptions")
    protected ArrayOfItemDescriptionData descriptions;

    /**
     * Gets the value of the prices property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfChargePriceData }
     *     
     */
    public ArrayOfChargePriceData getPrices() {
        return prices;
    }

    /**
     * Sets the value of the prices property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfChargePriceData }
     *     
     */
    public void setPrices(ArrayOfChargePriceData value) {
        this.prices = value;
    }

    /**
     * Gets the value of the descriptions property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfItemDescriptionData }
     *     
     */
    public ArrayOfItemDescriptionData getDescriptions() {
        return descriptions;
    }

    /**
     * Sets the value of the descriptions property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfItemDescriptionData }
     *     
     */
    public void setDescriptions(ArrayOfItemDescriptionData value) {
        this.descriptions = value;
    }

}
