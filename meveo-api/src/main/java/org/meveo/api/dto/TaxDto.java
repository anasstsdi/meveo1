package org.meveo.api.dto;

import java.math.BigDecimal;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Edward P. Legaspi
 * @since Oct 11, 2013
 **/
@XmlRootElement(name = "tax")
@XmlAccessorType(XmlAccessType.FIELD)
public class TaxDto extends BaseDto {

	private static final long serialVersionUID = 5184602572648722134L;

	@XmlElement(required = true)
	private String taxId;

	private String name;

	@XmlElement(required = true)
	private List<CountryTaxDto> countryTaxes;

	private String description;
	private String countryCode;
	private String regionCode;
	private BigDecimal percentage;

	public TaxDto() {

	}

	public String getTaxId() {
		return taxId;
	}

	public void setTaxId(String taxId) {
		this.taxId = taxId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<CountryTaxDto> getCountryTaxes() {
		return countryTaxes;
	}

	public void setCountryTaxes(List<CountryTaxDto> countryTaxes) {
		this.countryTaxes = countryTaxes;
	}

	@Override
	public String toString() {
		return "TaxDto [taxId=" + taxId + ", name=" + name + ", countryTaxes="
				+ countryTaxes + "]";
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getRegionCode() {
		return regionCode;
	}

	public void setRegionCode(String regionCode) {
		this.regionCode = regionCode;
	}

	public BigDecimal getPercentage() {
		return percentage;
	}

	public void setPercentage(BigDecimal percentage) {
		this.percentage = percentage;
	}
}
