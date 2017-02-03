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
package org.meveo.api.dto.payment;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.api.dto.BaseDto;
import org.meveo.api.dto.CustomFieldsDto;
import org.meveo.model.payments.PaymentMethodEnum;

@XmlRootElement(name = "Payment")
@XmlAccessorType(XmlAccessType.FIELD)
public class PaymentDto extends BaseDto {

	private static final long serialVersionUID = 1L;

	private String type;
	private String description;
	private PaymentMethodEnum paymentMethod;
	private String occTemplateCode;
	private BigDecimal amount;
	private String customerAccountCode;
	private String reference;
	private String bankLot;
	private Date depositDate;
	private Date bankCollectionDate;
	private Date dueDate;
	private Date transactionDate;
	private List<String> listOCCReferenceforMatching;
	private boolean isToMatching;
    private String paymentOrder;
    private BigDecimal fees = BigDecimal.ZERO; 
    private String comment;
	
	private CustomFieldsDto customFields = new CustomFieldsDto();

	/**
	 * @return the customFields
	 */
	public CustomFieldsDto getCustomFields() {
		return customFields;
	}

	/**
	 * @param customFields the customFields to set
	 */
	public void setCustomFields(CustomFieldsDto customFields) {
		this.customFields = customFields;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getOccTemplateCode() {
		return occTemplateCode;
	}

	public void setOccTemplateCode(String occTemplateCode) {
		this.occTemplateCode = occTemplateCode;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getCustomerAccountCode() {
		return customerAccountCode;
	}

	public void setCustomerAccountCode(String customerAccountCode) {
		this.customerAccountCode = customerAccountCode;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getBankLot() {
		return bankLot;
	}

	public void setBankLot(String bankLot) {
		this.bankLot = bankLot;
	}

	public Date getDepositDate() {
		return depositDate;
	}

	public void setDepositDate(Date depositDate) {
		this.depositDate = depositDate;
	}

	public Date getBankCollectionDate() {
		return bankCollectionDate;
	}

	public void setBankCollectionDate(Date bankCollectionDate) {
		this.bankCollectionDate = bankCollectionDate;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public Date getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(Date transactionDate) {
		this.transactionDate = transactionDate;
	}

	public List<String> getListOCCReferenceforMatching() {
		return listOCCReferenceforMatching;
	}

	public void setListOCCReferenceforMatching(List<String> listOCCReferenceforMatching) {
		this.listOCCReferenceforMatching = listOCCReferenceforMatching;
	}

	public boolean isToMatching() {
		return isToMatching;
	}

	public void setToMatching(boolean isToMatching) {
		this.isToMatching = isToMatching;
	}

	public PaymentMethodEnum getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(PaymentMethodEnum paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
	
	

	/**
	 * @return the paymentOrder
	 */
	public String getPaymentOrder() {
		return paymentOrder;
	}

	/**
	 * @param paymentOrder the paymentOrder to set
	 */
	public void setPaymentOrder(String paymentOrder) {
		this.paymentOrder = paymentOrder;
	}

	/**
	 * @return the fees
	 */
	public BigDecimal getFees() {
		return fees;
	}

	/**
	 * @param fees the fees to set
	 */
	public void setFees(BigDecimal fees) {
		this.fees = fees;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PaymentDto [type=" + type + ", description=" + description + ", paymentMethod=" + paymentMethod + ", occTemplateCode=" + occTemplateCode + ", amount=" + amount + ", customerAccountCode=" + customerAccountCode + ", reference=" + reference + ", bankLot=" + bankLot + ", depositDate=" + depositDate + ", bankCollectionDate=" + bankCollectionDate + ", dueDate=" + dueDate + ", transactionDate=" + transactionDate + ", listOCCReferenceforMatching="
				+ listOCCReferenceforMatching + ", isToMatching=" + isToMatching + ", paymentOrder=" + paymentOrder + ", fees=" + fees + ", comment=" + comment + ", customFields=" + customFields + "]";
	}

	

}
