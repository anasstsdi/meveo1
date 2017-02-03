package org.meveo.api.dto.catalog;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.StringUtils;
import org.meveo.api.dto.BaseDto;
import org.meveo.api.dto.CustomFieldsDto;
import org.meveo.model.catalog.ServiceChargeTemplateRecurring;
import org.meveo.model.catalog.ServiceChargeTemplateSubscription;
import org.meveo.model.catalog.ServiceChargeTemplateTermination;
import org.meveo.model.catalog.ServiceChargeTemplateUsage;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.model.catalog.WalletTemplate;

/**
 * @author Edward P. Legaspi
 * @since Oct 11, 2013
 **/
@XmlRootElement(name = "ServiceTemplate")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceTemplateDto extends BaseDto {

    private static final long serialVersionUID = -6794700715161690227L;

    @XmlAttribute(required = true)
    private String code;

    @XmlAttribute()
    private String description;
    
    private String longDescription;

    private String invoicingCalendar;

    private ServiceChargeTemplateRecurringsDto serviceChargeTemplateRecurrings;
    private ServiceChargeTemplateSubscriptionsDto serviceChargeTemplateSubscriptions;
    private ServiceChargeTemplateTerminationsDto serviceChargeTemplateTerminations;
    private ServiceChargeTemplateUsagesDto serviceChargeTemplateUsages;

    private CustomFieldsDto customFields = new CustomFieldsDto();

    @Deprecated
    private boolean mandatory;

    /**
     * BusinessServiceModel code.
     */
    private String somCode;
    private String imagePath;
    private String imageBase64;

    public ServiceTemplateDto() {
    }

    public ServiceTemplateDto(ServiceTemplate serviceTemplate, CustomFieldsDto customFieldInstances) {
        code = serviceTemplate.getCode();
        description = serviceTemplate.getDescription();
        longDescription = serviceTemplate.getLongDescription();
        invoicingCalendar = serviceTemplate.getInvoicingCalendar() == null ? null : serviceTemplate.getInvoicingCalendar().getCode();
        imagePath = serviceTemplate.getImagePath();

        if (serviceTemplate.getBusinessServiceModel() != null) {
            somCode = serviceTemplate.getBusinessServiceModel().getCode();
        }

        // set serviceChargeTemplateRecurrings
        if (serviceTemplate.getServiceRecurringCharges().size() > 0) {
            serviceChargeTemplateRecurrings = new ServiceChargeTemplateRecurringsDto();

            for (ServiceChargeTemplateRecurring recCharge : serviceTemplate.getServiceRecurringCharges()) {
                ServiceChargeTemplateRecurringDto serviceChargeTemplateRecurring = new ServiceChargeTemplateRecurringDto();
                serviceChargeTemplateRecurring.setCode(recCharge.getChargeTemplate().getCode());

                for (WalletTemplate wallet : recCharge.getWalletTemplates()) {
                    serviceChargeTemplateRecurring.getWallets().getWallet().add(wallet.getCode());
                }

                serviceChargeTemplateRecurrings.getServiceChargeTemplateRecurring().add(serviceChargeTemplateRecurring);
            }
        }

        // set serviceChargeTemplateSubscriptions
        if (serviceTemplate.getServiceSubscriptionCharges().size() > 0) {
            serviceChargeTemplateSubscriptions = new ServiceChargeTemplateSubscriptionsDto();

            for (ServiceChargeTemplateSubscription subCharge : serviceTemplate.getServiceSubscriptionCharges()) {
                ServiceChargeTemplateSubscriptionDto serviceChargeTemplateSubscription = new ServiceChargeTemplateSubscriptionDto();
                serviceChargeTemplateSubscription.setCode(subCharge.getChargeTemplate().getCode());

                for (WalletTemplate wallet : subCharge.getWalletTemplates()) {
                    serviceChargeTemplateSubscription.getWallets().getWallet().add(wallet.getCode());
                }

                serviceChargeTemplateSubscriptions.getServiceChargeTemplateSubscription().add(serviceChargeTemplateSubscription);
            }
        }

        // set serviceChargeTemplateTerminations
        if (serviceTemplate.getServiceTerminationCharges().size() > 0) {
            serviceChargeTemplateTerminations = new ServiceChargeTemplateTerminationsDto();

            for (ServiceChargeTemplateTermination terminationCharge : serviceTemplate.getServiceTerminationCharges()) {
                ServiceChargeTemplateTerminationDto serviceChargeTemplateTermination = new ServiceChargeTemplateTerminationDto();
                serviceChargeTemplateTermination.setCode(terminationCharge.getChargeTemplate().getCode());

                for (WalletTemplate wallet : terminationCharge.getWalletTemplates()) {
                    serviceChargeTemplateTermination.getWallets().getWallet().add(wallet.getCode());
                }

                serviceChargeTemplateTerminations.getServiceChargeTemplateTermination().add(serviceChargeTemplateTermination);
            }

        }

        // add serviceChargeTemplateUsages

        if (serviceTemplate.getServiceUsageCharges().size() > 0) {
            serviceChargeTemplateUsages = new ServiceChargeTemplateUsagesDto();

            for (ServiceChargeTemplateUsage usageCharge : serviceTemplate.getServiceUsageCharges()) {
                ServiceUsageChargeTemplateDto serviceUsageChargeTemplate = new ServiceUsageChargeTemplateDto();
                serviceUsageChargeTemplate.setCode(usageCharge.getChargeTemplate().getCode());

                if (usageCharge.getCounterTemplate() != null) {
                    serviceUsageChargeTemplate.setCounterTemplate(usageCharge.getCounterTemplate().getCode());
                }

                for (WalletTemplate wallet : usageCharge.getWalletTemplates()) {
                    serviceUsageChargeTemplate.getWallets().getWallet().add(wallet.getCode());
                }

                serviceChargeTemplateUsages.getServiceChargeTemplateUsage().add(serviceUsageChargeTemplate);
            }
        }

        customFields = customFieldInstances;
    }

    public ServiceTemplateDto(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInvoicingCalendar() {
        return invoicingCalendar;
    }

    public void setInvoicingCalendar(String invoicingCalendar) {
        this.invoicingCalendar = invoicingCalendar;
    }

    public ServiceChargeTemplateRecurringsDto getServiceChargeTemplateRecurrings() {
        return serviceChargeTemplateRecurrings;
    }

    public void setServiceChargeTemplateRecurrings(ServiceChargeTemplateRecurringsDto serviceChargeTemplateRecurrings) {
        this.serviceChargeTemplateRecurrings = serviceChargeTemplateRecurrings;
    }

    public ServiceChargeTemplateSubscriptionsDto getServiceChargeTemplateSubscriptions() {
        return serviceChargeTemplateSubscriptions;
    }

    public void setServiceChargeTemplateSubscriptions(ServiceChargeTemplateSubscriptionsDto serviceChargeTemplateSubscriptions) {
        this.serviceChargeTemplateSubscriptions = serviceChargeTemplateSubscriptions;
    }

    public ServiceChargeTemplateTerminationsDto getServiceChargeTemplateTerminations() {
        return serviceChargeTemplateTerminations;
    }

    public void setServiceChargeTemplateTerminations(ServiceChargeTemplateTerminationsDto serviceChargeTemplateTerminations) {
        this.serviceChargeTemplateTerminations = serviceChargeTemplateTerminations;
    }

    public ServiceChargeTemplateUsagesDto getServiceChargeTemplateUsages() {
        return serviceChargeTemplateUsages;
    }

    public void setServiceChargeTemplateUsages(ServiceChargeTemplateUsagesDto serviceChargeTemplateUsages) {
        this.serviceChargeTemplateUsages = serviceChargeTemplateUsages;
    }

    @Override
	public String toString() {
		return "ServiceTemplateDto [code=" + code + ", description=" + description + ", longDescription=" + longDescription + ", invoicingCalendar=" + invoicingCalendar
				+ ", serviceChargeTemplateRecurrings=" + serviceChargeTemplateRecurrings + ", serviceChargeTemplateSubscriptions=" + serviceChargeTemplateSubscriptions
				+ ", serviceChargeTemplateTerminations=" + serviceChargeTemplateTerminations + ", serviceChargeTemplateUsages=" + serviceChargeTemplateUsages + ", customFields="
				+ customFields + ", mandatory=" + mandatory + ", somCode=" + somCode + ", imagePath=" + imagePath + "]";
	}

    public CustomFieldsDto getCustomFields() {
        return customFields;
    }

    public void setCustomFields(CustomFieldsDto customFields) {
        this.customFields = customFields;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getSomCode() {
        return somCode;
    }

    public void setSomCode(String somCode) {
        this.somCode = somCode;
    }

    public boolean isCodeOnly() {
        return StringUtils.isBlank(description) && StringUtils.isBlank(invoicingCalendar) && StringUtils.isBlank(somCode) && serviceChargeTemplateRecurrings == null
                && serviceChargeTemplateSubscriptions == null && serviceChargeTemplateTerminations == null && serviceChargeTemplateUsages == null
                && (customFields == null || customFields.isEmpty());
    }

	public String getLongDescription() {
		return longDescription;
	}

	public void setLongDescription(String longDescription) {
		this.longDescription = longDescription;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public String getImageBase64() {
		return imageBase64;
	}

	public void setImageBase64(String imageBase64) {
		this.imageBase64 = imageBase64;
	}
}