package org.meveo.api.dto.catalog;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.api.dto.BaseDto;
import org.meveo.model.catalog.BusinessServiceModel;

@XmlRootElement(name = "BusinessServiceModel")
@XmlAccessorType(XmlAccessType.FIELD)
public class BusinessServiceModelDto extends BaseDto {

	private static final long serialVersionUID = -7023791262640948222L;

	@NotNull
	@XmlAttribute(required = true)
	private String code;

	@XmlAttribute
	private String description;

	@NotNull
	@XmlElement(required = true)
	private String serviceTemplateCode;

	private String scriptCode;

	private boolean duplicateService;

	private boolean duplicatePricePlan;

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

	public String getServiceTemplateCode() {
		return serviceTemplateCode;
	}

	public void setServiceTemplateCode(String serviceTemplateCode) {
		this.serviceTemplateCode = serviceTemplateCode;
	}

	public String getScriptCode() {
		return scriptCode;
	}

	public void setScriptCode(String scriptCode) {
		this.scriptCode = scriptCode;
	}

	public boolean isDuplicateService() {
		return duplicateService;
	}

	public void setDuplicateService(boolean duplicateService) {
		this.duplicateService = duplicateService;
	}

	public boolean isDuplicatePricePlan() {
		return duplicatePricePlan;
	}

	public void setDuplicatePricePlan(boolean duplicatePricePlan) {
		this.duplicatePricePlan = duplicatePricePlan;
	}

    /**
     * Convert BusinessServiceModelDto to a BusinessServiceModel instance.
     * 
     * @param dto BusinessServiceModelDto object to convert
     * @param bsmToUpdate BusinessServiceModel to update with values from dto, or if null create a new one
     * @return A new or updated BusinessServiceModel instance
     */
    public static BusinessServiceModel fromDTO(BusinessServiceModelDto dto, BusinessServiceModel bsmToUpdate) {
        BusinessServiceModel bsm = new BusinessServiceModel();
        if (bsmToUpdate != null) {
            bsm = bsmToUpdate;
        }
        bsm.setCode(dto.getCode());
        bsm.setDescription(dto.getDescription());
        bsm.setDuplicatePricePlan(dto.isDuplicatePricePlan());
        bsm.setDuplicateService(dto.isDuplicateService());

        return bsm;
    }
	
	
	@Override
	public String toString() {
		return "BusinessServiceModelDto [code=" + code + ", description=" + description + ", serviceTemplateCode=" + serviceTemplateCode + ", scriptCode=" + scriptCode
				+ ", duplicateService=" + duplicateService + ", duplicatePricePlan=" + duplicatePricePlan + "]";
	}

}
