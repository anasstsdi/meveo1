/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.api.dto;

import java.math.BigDecimal;
import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.model.billing.RatedTransaction;

/**
 * @author R.AITYAAZZA
 * 
 */

@XmlRootElement(name = "RatedTransaction")
@XmlAccessorType(XmlAccessType.FIELD)
public class RatedTransactionDto extends BaseDto {

	private static final long serialVersionUID = -7627662294414998797L;
	@XmlElement(required = true)
	private Date usageDate;
	
	private BigDecimal unitAmountWithoutTax;
	private BigDecimal unitAmountWithTax;
	private BigDecimal unitAmountTax;
	private BigDecimal quantity;
	@XmlElement(required = true)
	private BigDecimal amountWithoutTax;
	@XmlElement(required = true)
	private BigDecimal amountWithTax;
	@XmlElement(required = true)
	private BigDecimal amountTax;
	@XmlElement(required = true)
	private String code;
	private String description;
	private String unityDescription;
	private String priceplanCode;
	private boolean doNotTriggerInvoicing = false;

	public RatedTransactionDto(){
	    
	}
	
	public RatedTransactionDto(RatedTransaction ratedTransaction) {
        this.setUsageDate(ratedTransaction.getUsageDate());
        this.setUnitAmountWithoutTax(ratedTransaction.getUnitAmountWithoutTax());
        this.setUnitAmountWithTax(ratedTransaction.getUnitAmountWithTax());
        this.setUnitAmountTax(ratedTransaction.getUnitAmountWithTax());
        this.setQuantity(ratedTransaction.getQuantity());
        this.setAmountWithoutTax(ratedTransaction.getAmountWithoutTax());
        this.setAmountWithTax(ratedTransaction.getAmountWithTax());
        this.setAmountTax(ratedTransaction.getAmountWithTax());
        this.setCode(ratedTransaction.getCode());
        this.setDescription(ratedTransaction.getDescription());
        this.setUnityDescription(ratedTransaction.getUnityDescription());
		if (ratedTransaction.getPriceplan() != null) {
			this.setPriceplanCode(ratedTransaction.getPriceplan().getCode());
		}
        this.setDoNotTriggerInvoicing(ratedTransaction.isDoNotTriggerInvoicing());
    }

    public Date getUsageDate() {
		return usageDate;
	}

	public void setUsageDate(Date usageDate) {
		this.usageDate = usageDate;
	}

	public BigDecimal getUnitAmountWithoutTax() {
		return unitAmountWithoutTax;
	}

	public void setUnitAmountWithoutTax(BigDecimal unitAmountWithoutTax) {
		this.unitAmountWithoutTax = unitAmountWithoutTax;
	}

	public BigDecimal getUnitAmountWithTax() {
		return unitAmountWithTax;
	}

	public void setUnitAmountWithTax(BigDecimal unitAmountWithTax) {
		this.unitAmountWithTax = unitAmountWithTax;
	}

	public BigDecimal getUnitAmountTax() {
		return unitAmountTax;
	}

	public void setUnitAmountTax(BigDecimal unitAmountTax) {
		this.unitAmountTax = unitAmountTax;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getAmountWithoutTax() {
		return amountWithoutTax;
	}

	public void setAmountWithoutTax(BigDecimal amountWithoutTax) {
		this.amountWithoutTax = amountWithoutTax;
	}

	public BigDecimal getAmountWithTax() {
		return amountWithTax;
	}

	public void setAmountWithTax(BigDecimal amountWithTax) {
		this.amountWithTax = amountWithTax;
	}

	public BigDecimal getAmountTax() {
		return amountTax;
	}

	public void setAmountTax(BigDecimal amountTax) {
		this.amountTax = amountTax;
	}

	public boolean isDoNotTriggerInvoicing() {
		return doNotTriggerInvoicing;
	}

	public void setDoNotTriggerInvoicing(boolean doNotTriggerInvoicing) {
		this.doNotTriggerInvoicing = doNotTriggerInvoicing;
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

	public String getUnityDescription() {
		return unityDescription;
	}

	public void setUnityDescription(String unityDescription) {
		this.unityDescription = unityDescription;
	}

	public String getPriceplanCode() {
		return priceplanCode;
	}

	public void setPriceplanCode(String priceplanCode) {
		this.priceplanCode = priceplanCode;
	}

}
