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
// Generated on: 2011.02.01 at 08:25:37 PM WET 
//


package org.meveo.model.jaxb.customer;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


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
 *         &lt;element ref="{}warningCustomer" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}warningSeller" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}warningCustomerAccount" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "warningCustomer",
    "warningSeller",
    "warningCustomerAccount"
})
@XmlRootElement(name = "warnings")
public class Warnings {

    protected List<WarningCustomer> warningCustomer;
    protected List<WarningSeller> warningSeller;
    protected List<WarningCustomerAccount> warningCustomerAccount;

    /**
     * Gets the value of the warningCustomer property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the warningCustomer property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWarningCustomer().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link WarningCustomer }
     * 
     * 
     */
    public List<WarningCustomer> getWarningCustomer() {
        if (warningCustomer == null) {
            warningCustomer = new ArrayList<WarningCustomer>();
        }
        return this.warningCustomer;
    }

    public List<WarningSeller> getWarningSeller() {
        if (warningSeller == null) {
        	warningSeller = new ArrayList<WarningSeller>();
        }
        return this.warningSeller;
    }
    
    /**
     * Gets the value of the warningCustomerAccount property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the warningCustomerAccount property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWarningCustomerAccount().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link WarningCustomerAccount }
     * 
     * 
     */
    public List<WarningCustomerAccount> getWarningCustomerAccount() {
        if (warningCustomerAccount == null) {
            warningCustomerAccount = new ArrayList<WarningCustomerAccount>();
        }
        return this.warningCustomerAccount;
    }

}
