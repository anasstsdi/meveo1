package org.meveo.api.dto.account;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.api.dto.BusinessDto;
import org.meveo.model.crm.CustomerCategory;

/**
 * @author Edward P. Legaspi
 **/
@XmlRootElement(name = "CustomerCategory")
@XmlAccessorType(XmlAccessType.FIELD)
public class CustomerCategoryDto extends BusinessDto{

	private static final long serialVersionUID = -8778571285967620018L;

	private String code;
	private String description;
	private boolean exoneratedFromTaxes;
	private String exonerationTaxEl;
	private String exonerationReason;
	

	public CustomerCategoryDto() {

	}

	public CustomerCategoryDto(CustomerCategory e) {
		code = e.getCode();
		description = e.getDescription();
		exoneratedFromTaxes = e.getExoneratedFromTaxes();
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

	@Override
	public String toString() {
		return "CustomerCategoryDto [code=" + code + ", description=" + description + ", exoneratedFromTaxes=" + exoneratedFromTaxes + ", exonerationTaxEl=" + exonerationTaxEl + ", exonerationReason=" + exonerationReason + "]";
	}

	public boolean isExoneratedFromTaxes() {
		return exoneratedFromTaxes;
	}

	public void setExoneratedFromTaxes(boolean exoneratedFromTaxes) {
		this.exoneratedFromTaxes = exoneratedFromTaxes;
	}

	/**
	 * @return the exonerationTaxEl
	 */
	public String getExonerationTaxEl() {
		return exonerationTaxEl;
	}

	/**
	 * @param exonerationTaxEl the exonerationTaxEl to set
	 */
	public void setExonerationTaxEl(String exonerationTaxEl) {
		this.exonerationTaxEl = exonerationTaxEl;
	}

	/**
	 * @return the exonerationReason
	 */
	public String getExonerationReason() {
		return exonerationReason;
	}

	/**
	 * @param exonerationReason the exonerationReason to set
	 */
	public void setExonerationReason(String exonerationReason) {
		this.exonerationReason = exonerationReason;
	}
}
