package org.meveo.api.dto.billing;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.meveo.api.dto.BusinessDto;
import org.meveo.api.dto.CustomFieldsDto;
import org.meveo.api.dto.account.AccessesDto;

/**
 * @author Edward P. Legaspi
 **/
@XmlRootElement(name = "Subscription")
@XmlType(name = "Subscription")
@XmlAccessorType(XmlAccessType.FIELD)
public class SubscriptionDto extends BusinessDto {

    private static final long serialVersionUID = -6021918810749866648L;

    @XmlAttribute(required = true)
    private String code;

    @XmlAttribute()
    private String description;

    @XmlElement(required = true)
    private String userAccount;

    @XmlElement(required = true)
    private String offerTemplate;

    @XmlElement(required = true)
    private Date subscriptionDate;

    private Date terminationDate;
    
    private Date endAgreementDate;
    
    private String status;

    @XmlElement(required = false)
    private CustomFieldsDto customFields = new CustomFieldsDto();

    @XmlElement(required = false)
    private AccessesDto accesses = new AccessesDto();

    @XmlElement(required = false)
    private ServiceInstancesDto services = new ServiceInstancesDto();

    private String terminationReason;

    public SubscriptionDto() {

    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    public String getOfferTemplate() {
        return offerTemplate;
    }

    public void setOfferTemplate(String offerTemplate) {
        this.offerTemplate = offerTemplate;
    }

    public Date getSubscriptionDate() {
        return subscriptionDate;
    }

    public void setSubscriptionDate(Date subscriptionDate) {
        this.subscriptionDate = subscriptionDate;
    }

    public Date getTerminationDate() {
        return terminationDate;
    }

    public void setTerminationDate(Date terminationDate) {
        this.terminationDate = terminationDate;
    }
    
    public Date getEndAgreementDate() {
        return endAgreementDate;
    }
    
    public void setEndAgreementDate(Date endAgreementDate) {
        this.endAgreementDate = endAgreementDate;
    }

    @Override
    public String toString() {
        return "SubscriptionDto [code=" + code + ", description=" + description + ", userAccount=" + userAccount + ", offerTemplate=" + offerTemplate + ", subscriptionDate="
                + subscriptionDate + ", terminationDate=" + terminationDate + ", status=" + status + ", customFields=" + customFields + ", accesses=" + accesses + ", services="
                + services + ", terminationReason=" + terminationReason + "]";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public AccessesDto getAccesses() {
        return accesses;
    }

    public void setAccesses(AccessesDto accesses) {
        this.accesses = accesses;
    }

    public ServiceInstancesDto getServices() {
        return services;
    }

    public void setServices(ServiceInstancesDto services) {
        this.services = services;
    }

    public CustomFieldsDto getCustomFields() {
        return customFields;
    }

    public void setCustomFields(CustomFieldsDto customFields) {
        this.customFields = customFields;
    }

    public String getTerminationReason() {
        return terminationReason;
    }

    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }

}
