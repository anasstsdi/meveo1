/*
* (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.2-147 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.02.01 at 06:47:58 PM WET 
//


package org.meveo.model.jaxb.account;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.meveo.model.jaxb.customer.CustomFields;
import org.meveo.model.shared.DateUtils;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}subscriptionDate"/>
 *         &lt;element ref="{}description"/>
 *         &lt;element ref="{}externalRef1"/>
 *         &lt;element ref="{}externalRef2"/>
 *         &lt;element ref="{}company"/>
 *         &lt;element ref="{}name"/>
 *         &lt;element ref="{}address"/>
 *         &lt;element ref="{}customFields"/>
 *       &lt;/sequence>
 *       &lt;attribute name="code" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "subscriptionDate",
    "description",
    "externalRef1",
    "externalRef2",
    "company",
    "name",
    "address",
    "customFields"
})
@XmlRootElement(name = "userAccount")
public class UserAccount {

    @XmlElement(required = true)
    protected String subscriptionDate;
    @XmlElement(required = true)
    protected String description;
    @XmlElement(required = true)
    protected String externalRef1;
    @XmlElement(required = true)
    protected String externalRef2;
    @XmlElement(required = true)
    protected String company;
    @XmlElement(required = true)
    protected Name name;
    @XmlElement(required = true)
    protected Address address;
    protected CustomFields customFields;
    @XmlAttribute(name = "code")
    protected String code;
    
    public UserAccount(){}
    
    public UserAccount(org.meveo.model.billing.UserAccount ua,String dateFormat) {
    	if(ua!=null){
	    	subscriptionDate=ua.getSubscriptionDate()==null?"":
	    		DateUtils.formatDateWithPattern(ua.getSubscriptionDate(), dateFormat);
	    	description=ua.getDescription();
	    	externalRef1=ua.getExternalRef1();
	    	externalRef2=ua.getExternalRef2();
	    	name=new Name(ua.getName());
	    	address=new Address(ua.getAddress());
            customFields = CustomFields.toDTO(ua.getCfFields());
	    	code=ua.getCode();
    	}
	}

	/**
     * Gets the value of the subscriptionDate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubscriptionDate() {
        return subscriptionDate;
    }

    /**
     * Sets the value of the subscriptionDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSubscriptionDate(String value) {
        this.subscriptionDate = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the externalRef1 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExternalRef1() {
        return externalRef1;
    }

    /**
     * Sets the value of the externalRef1 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExternalRef1(String value) {
        this.externalRef1 = value;
    }

    /**
     * Gets the value of the externalRef2 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExternalRef2() {
        return externalRef2;
    }

    /**
     * Sets the value of the externalRef2 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExternalRef2(String value) {
        this.externalRef2 = value;
    }

    /**
     * Gets the value of the company property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCompany() {
        return company;
    }

    /**
     * Sets the value of the company property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCompany(String value) {
        this.company = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link Name }
     *     
     */
    public Name getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link Name }
     *     
     */
    public void setName(Name value) {
        this.name = value;
    }

    /**
     * Gets the value of the address property.
     * 
     * @return
     *     possible object is
     *     {@link Address }
     *     
     */
    public Address getAddress() {
        return address;
    }

    /**
     * Sets the value of the address property.
     * 
     * @param value
     *     allowed object is
     *     {@link Address }
     *     
     */
    public void setAddress(Address value) {
        this.address = value;
    }

    /**
     * Gets the value of the code property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the value of the code property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCode(String value) {
        this.code = value;
    }

	public CustomFields getCustomFields() {
		return customFields;
	}

	public void setCustomFields(CustomFields customFields) {
		this.customFields = customFields;
	}

}
