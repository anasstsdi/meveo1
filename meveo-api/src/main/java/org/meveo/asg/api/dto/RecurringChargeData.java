//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.02.24 at 10:15:44 AM CST 
//


package org.meveo.asg.api.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for RecurringChargeData complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RecurringChargeData">
 *   &lt;complexContent>
 *     &lt;extension base="{}ChargeBaseData">
 *       &lt;sequence>
 *         &lt;element name="SubscriptionProrrata" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="TerminationProrrata" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="ApplyInAdvance" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="BillingPeriod" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="SubscriptionAgeRangeCharges" type="{}ArrayOfSubscriptionAgeRangeChargeData" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RecurringChargeData", propOrder = {
    "subscriptionProrrata",
    "terminationProrrata",
    "applyInAdvance",
    "billingPeriod",
    "subscriptionAgeRangeCharges"
})
public class RecurringChargeData
    extends ChargeBaseData
{

    @XmlElement(name = "SubscriptionProrrata")
    protected boolean subscriptionProrrata;
    @XmlElement(name = "TerminationProrrata")
    protected boolean terminationProrrata;
    @XmlElement(name = "ApplyInAdvance")
    protected boolean applyInAdvance;
    @XmlElement(name = "BillingPeriod")
    protected String billingPeriod;
    @XmlElement(name = "SubscriptionAgeRangeCharges")
    protected ArrayOfSubscriptionAgeRangeChargeData subscriptionAgeRangeCharges;

    /**
     * Gets the value of the subscriptionProrrata property.
     * 
     */
    public boolean isSubscriptionProrrata() {
        return subscriptionProrrata;
    }

    /**
     * Sets the value of the subscriptionProrrata property.
     * 
     */
    public void setSubscriptionProrrata(boolean value) {
        this.subscriptionProrrata = value;
    }

    /**
     * Gets the value of the terminationProrrata property.
     * 
     */
    public boolean isTerminationProrrata() {
        return terminationProrrata;
    }

    /**
     * Sets the value of the terminationProrrata property.
     * 
     */
    public void setTerminationProrrata(boolean value) {
        this.terminationProrrata = value;
    }

    /**
     * Gets the value of the applyInAdvance property.
     * 
     */
    public boolean isApplyInAdvance() {
        return applyInAdvance;
    }

    /**
     * Sets the value of the applyInAdvance property.
     * 
     */
    public void setApplyInAdvance(boolean value) {
        this.applyInAdvance = value;
    }

    /**
     * Gets the value of the billingPeriod property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBillingPeriod() {
        return billingPeriod;
    }

    /**
     * Sets the value of the billingPeriod property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBillingPeriod(String value) {
        this.billingPeriod = value;
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
