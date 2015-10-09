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
// Generated on: 2011.02.03 at 11:45:33 PM WET 
//


package org.meveo.model.jaxb.subscription;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.meveo.model.jaxb.subscription package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _C3_QNAME = new QName("", "C3");
    private final static QName _Description_QNAME = new QName("", "description");
    private final static QName _EndAgreementDate_QNAME = new QName("", "endAgreementDate");
    private final static QName _AmountWithoutTax_QNAME = new QName("", "amountWithoutTax");
    private final static QName _C1_QNAME = new QName("", "C1");
    private final static QName _Quantity_QNAME = new QName("", "quantity");
    private final static QName _C2_QNAME = new QName("", "C2");
    private final static QName _SubscriptionDate_QNAME = new QName("", "subscriptionDate");
    private final static QName _AmountWithTax_QNAME = new QName("", "amountWithTax");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.meveo.model.jaxb.subscription
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Status }
     * 
     */
    public Status createStatus() {
        return new Status();
    }

    /**
     * Create an instance of {@link Subscriptions }
     * 
     */
    public Subscriptions createSubscriptions() {
        return new Subscriptions();
    }

    /**
     * Create an instance of {@link ErrorSubscription }
     * 
     */
    public ErrorSubscription createErrorSubscription() {
        return new ErrorSubscription();
    }

    /**
     * Create an instance of {@link Errors }
     * 
     */
    public Errors createErrors() {
        return new Errors();
    }

    /**
     * Create an instance of {@link Services }
     * 
     */
    public Services createServices() {
        return new Services();
    }

    /**
     * Create an instance of {@link ErrorServiceInstance }
     * 
     */
    public ErrorServiceInstance createErrorServiceInstance() {
        return new ErrorServiceInstance();
    }

    /**
     * Create an instance of {@link Warnings }
     * 
     */
    public Warnings createWarnings() {
        return new Warnings();
    }

    /**
     * Create an instance of {@link Subscription }
     * 
     */
    public Subscription createSubscription() {
        return new Subscription();
    }

    /**
     * Create an instance of {@link WarningSubscription }
     * 
     */
    public WarningSubscription createWarningSubscription() {
        return new WarningSubscription();
    }

    /**
     * Create an instance of {@link WarningServiceInstance }
     * 
     */
    public WarningServiceInstance createWarningServiceInstance() {
        return new WarningServiceInstance();
    }

    /**
     * Create an instance of {@link ServiceInstance }
     * 
     */
    public ServiceInstance createServiceInstance() {
        return new ServiceInstance();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "C3")
    public JAXBElement<String> createC3(String value) {
        return new JAXBElement<String>(_C3_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "description")
    public JAXBElement<String> createDescription(String value) {
        return new JAXBElement<String>(_Description_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "endAgreementDate")
    public JAXBElement<String> createEndAgreementDate(String value) {
        return new JAXBElement<String>(_EndAgreementDate_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "amountWithoutTax")
    public JAXBElement<String> createAmountWithoutTax(String value) {
        return new JAXBElement<String>(_AmountWithoutTax_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "C1")
    public JAXBElement<String> createC1(String value) {
        return new JAXBElement<String>(_C1_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "quantity")
    public JAXBElement<String> createQuantity(String value) {
        return new JAXBElement<String>(_Quantity_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "C2")
    public JAXBElement<String> createC2(String value) {
        return new JAXBElement<String>(_C2_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "subscriptionDate")
    public JAXBElement<String> createSubscriptionDate(String value) {
        return new JAXBElement<String>(_SubscriptionDate_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "amountWithTax")
    public JAXBElement<String> createAmountWithTax(String value) {
        return new JAXBElement<String>(_AmountWithTax_QNAME, String.class, null, value);
    }

}
