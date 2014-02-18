//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.02.18 at 08:46:55 PM CST 
//


package org.meveo.asg.api.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for UpdateSubscriptionWithCreditLimitRequestBase complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="UpdateSubscriptionWithCreditLimitRequestBase">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="OrganizationId" type="{http://microsoft.com/wsdl/types/}guid"/>
 *         &lt;element name="RequestId" type="{http://microsoft.com/wsdl/types/}guid"/>
 *         &lt;element name="SubscriptionId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ServicesToAdd" type="{}ArrayOfServiceSubscriptionDate" minOccurs="0"/>
 *         &lt;element name="ServicesToTerminate" type="{}ArrayOfServiceSubscriptionDate" minOccurs="0"/>
 *         &lt;element name="CreditLimits" type="{}ArrayOfOrganizationCreditLimit" minOccurs="0"/>
 *         &lt;element name="SubscriptionDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UpdateSubscriptionWithCreditLimitRequestBase", propOrder = {
    "organizationId",
    "requestId",
    "subscriptionId",
    "servicesToAdd",
    "servicesToTerminate",
    "creditLimits",
    "subscriptionDate"
})
@XmlSeeAlso({
    UpdateOrganizationSubscriptionWithCreditLimitRequest.class,
    UpdateUserSubscriptionWithCreditLimitRequest.class
})
public abstract class UpdateSubscriptionWithCreditLimitRequestBase {

    @XmlElement(name = "OrganizationId", required = true)
    protected String organizationId;
    @XmlElement(name = "RequestId", required = true)
    protected String requestId;
    @XmlElement(name = "SubscriptionId")
    protected String subscriptionId;
    @XmlElement(name = "ServicesToAdd")
    protected ArrayOfServiceSubscriptionDate servicesToAdd;
    @XmlElement(name = "ServicesToTerminate")
    protected ArrayOfServiceSubscriptionDate servicesToTerminate;
    @XmlElement(name = "CreditLimits")
    protected ArrayOfOrganizationCreditLimit creditLimits;
    @XmlElement(name = "SubscriptionDate", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar subscriptionDate;

    /**
     * Gets the value of the organizationId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the value of the organizationId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrganizationId(String value) {
        this.organizationId = value;
    }

    /**
     * Gets the value of the requestId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Sets the value of the requestId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRequestId(String value) {
        this.requestId = value;
    }

    /**
     * Gets the value of the subscriptionId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubscriptionId() {
        return subscriptionId;
    }

    /**
     * Sets the value of the subscriptionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSubscriptionId(String value) {
        this.subscriptionId = value;
    }

    /**
     * Gets the value of the servicesToAdd property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfServiceSubscriptionDate }
     *     
     */
    public ArrayOfServiceSubscriptionDate getServicesToAdd() {
        return servicesToAdd;
    }

    /**
     * Sets the value of the servicesToAdd property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfServiceSubscriptionDate }
     *     
     */
    public void setServicesToAdd(ArrayOfServiceSubscriptionDate value) {
        this.servicesToAdd = value;
    }

    /**
     * Gets the value of the servicesToTerminate property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfServiceSubscriptionDate }
     *     
     */
    public ArrayOfServiceSubscriptionDate getServicesToTerminate() {
        return servicesToTerminate;
    }

    /**
     * Sets the value of the servicesToTerminate property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfServiceSubscriptionDate }
     *     
     */
    public void setServicesToTerminate(ArrayOfServiceSubscriptionDate value) {
        this.servicesToTerminate = value;
    }

    /**
     * Gets the value of the creditLimits property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfOrganizationCreditLimit }
     *     
     */
    public ArrayOfOrganizationCreditLimit getCreditLimits() {
        return creditLimits;
    }

    /**
     * Sets the value of the creditLimits property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfOrganizationCreditLimit }
     *     
     */
    public void setCreditLimits(ArrayOfOrganizationCreditLimit value) {
        this.creditLimits = value;
    }

    /**
     * Gets the value of the subscriptionDate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getSubscriptionDate() {
        return subscriptionDate;
    }

    /**
     * Sets the value of the subscriptionDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setSubscriptionDate(XMLGregorianCalendar value) {
        this.subscriptionDate = value;
    }

}
