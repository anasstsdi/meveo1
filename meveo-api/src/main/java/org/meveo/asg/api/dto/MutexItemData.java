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
 * <p>Java class for MutexItemData complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MutexItemData">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="MutexId" type="{http://microsoft.com/wsdl/types/}guid"/>
 *         &lt;element name="DefaultServiceId" type="{http://microsoft.com/wsdl/types/}guid"/>
 *         &lt;element name="ServiceIds" type="{}ArrayOfGuid" minOccurs="0"/>
 *         &lt;element name="Optional" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MutexItemData", propOrder = {
    "mutexId",
    "defaultServiceId",
    "serviceIds",
    "optional"
})
public class MutexItemData {

    @XmlElement(name = "MutexId", required = true)
    protected String mutexId;
    @XmlElement(name = "DefaultServiceId", required = true)
    protected String defaultServiceId;
    @XmlElement(name = "ServiceIds")
    protected ArrayOfGuid serviceIds;
    @XmlElement(name = "Optional")
    protected boolean optional;

    /**
     * Gets the value of the mutexId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMutexId() {
        return mutexId;
    }

    /**
     * Sets the value of the mutexId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMutexId(String value) {
        this.mutexId = value;
    }

    /**
     * Gets the value of the defaultServiceId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDefaultServiceId() {
        return defaultServiceId;
    }

    /**
     * Sets the value of the defaultServiceId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDefaultServiceId(String value) {
        this.defaultServiceId = value;
    }

    /**
     * Gets the value of the serviceIds property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfGuid }
     *     
     */
    public ArrayOfGuid getServiceIds() {
        return serviceIds;
    }

    /**
     * Sets the value of the serviceIds property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfGuid }
     *     
     */
    public void setServiceIds(ArrayOfGuid value) {
        this.serviceIds = value;
    }

    /**
     * Gets the value of the optional property.
     * 
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Sets the value of the optional property.
     * 
     */
    public void setOptional(boolean value) {
        this.optional = value;
    }

}
