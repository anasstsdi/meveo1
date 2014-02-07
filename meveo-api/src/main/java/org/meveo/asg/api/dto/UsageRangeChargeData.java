//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.10.30 at 08:09:14 AM CST 
//


package org.meveo.asg.api.dto;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for UsageRangeChargeData complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="UsageRangeChargeData">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Min" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *         &lt;element name="Max" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *         &lt;element name="Prices" type="{}ArrayOfChargePriceData" minOccurs="0"/>
 *         &lt;element name="SubscriptionAgeRangeCharges" type="{}ArrayOfSubscriptionAgeRangeChargeData" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UsageRangeChargeData", propOrder = {
    "min",
    "max",
    "prices",
    "subscriptionAgeRangeCharges"
})
public class UsageRangeChargeData {

    @XmlElement(name = "Min", required = true, nillable = true)
    protected BigDecimal min;
    @XmlElement(name = "Max", required = true, nillable = true)
    protected BigDecimal max;
    @XmlElement(name = "Prices")
    protected ArrayOfChargePriceData prices;
    @XmlElement(name = "SubscriptionAgeRangeCharges")
    protected ArrayOfSubscriptionAgeRangeChargeData subscriptionAgeRangeCharges;

    /**
     * Gets the value of the min property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getMin() {
        return min;
    }

    /**
     * Sets the value of the min property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setMin(BigDecimal value) {
        this.min = value;
    }

    /**
     * Gets the value of the max property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getMax() {
        return max;
    }

    /**
     * Sets the value of the max property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setMax(BigDecimal value) {
        this.max = value;
    }

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
     * Gets the value of the subscriptionAgeRangeCharges property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfSubscriptionAgeRangeChargeData }
     *     
     */
    public ArrayOfSubscriptionAgeRangeChargeData getSubscriptionAgeRangeCharges() {
        return subscriptionAgeRangeCharges;
    }

    /**
     * Sets the value of the subscriptionAgeRangeCharges property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfSubscriptionAgeRangeChargeData }
     *     
     */
    public void setSubscriptionAgeRangeCharges(ArrayOfSubscriptionAgeRangeChargeData value) {
        this.subscriptionAgeRangeCharges = value;
    }

}
