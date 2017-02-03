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
package org.meveo.model.payments;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Type;
import org.meveo.model.AuditableEntity;
import org.meveo.model.CustomFieldEntity;
import org.meveo.model.ICustomFieldEntity;
import org.meveo.model.ObservableEntity;

/**
 * Account Transaction.
 */
@Entity
@ObservableEntity
@Table(name = "AR_ACCOUNT_OPERATION")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TRANSACTION_TYPE")
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "AR_ACCOUNT_OPERATION_SEQ")
@CustomFieldEntity(cftCodePrefix = "ACC_OP")
public class AccountOperation extends AuditableEntity implements ICustomFieldEntity{

	private static final long serialVersionUID = 1L;

	@Column(name = "DUE_DATE")
	@Temporal(TemporalType.DATE)
	private Date dueDate;

	@Column(name = "TRANSACTION_TYPE", insertable = false, updatable = false, length = 31)
	@Size(max = 31)	
	private String type;

	@Column(name = "TRANSACTION_DATE")
	@Temporal(TemporalType.DATE)
	private Date transactionDate;

	@Column(name = "TRANSACTION_CATEGORY")
	@Enumerated(EnumType.STRING)
	private OperationCategoryEnum transactionCategory;

	@Column(name = "REFERENCE", length = 255)
	@Size(max = 255)
	private String reference;

	@Column(name = "ACCOUNT_CODE", length = 255)
    @Size(max = 255)
	private String accountCode;

	@Column(name = "ACCOUNT_CODE_CLIENT_SIDE", length = 255)
    @Size(max = 255)
	private String accountCodeClientSide;

	@Column(name = "AMOUNT", precision = 23, scale = 12)
	private BigDecimal amount;

	@Column(name = "MATCHING_AMOUNT", precision = 23, scale = 12)
	private BigDecimal matchingAmount = BigDecimal.ZERO;

	@Column(name = "UN_MATCHING_AMOUNT", precision = 23, scale = 12)
	private BigDecimal unMatchingAmount = BigDecimal.ZERO;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CUSTOMER_ACCOUNT_ID")
	private CustomerAccount customerAccount;

	@Enumerated(EnumType.STRING)
	@Column(name = "MATCHING_STATUS")
	private MatchingStatusEnum matchingStatus;

	@OneToMany(mappedBy = "accountOperation")
	private List<MatchingAmount> matchingAmounts = new ArrayList<MatchingAmount>();

	@Column(name = "OCC_CODE", length = 255)
    @Size(max = 255)
	private String occCode;

	@Column(name = "OCC_DESCRIPTION", length = 255)
    @Size(max = 255)
	private String occDescription;
	
	
	@Type(type="numeric_boolean")
    @Column(name = "EXCLUDED_FROM_DUNNING")
	private boolean excludedFromDunning;
	
	@Column(name = "ORDER_NUM")   
	private String orderNumber;// order number, '|' will be used as seperator if many orders
	
    @Column(name = "UUID", nullable = false, updatable = false, length = 60)
    @Size(max = 60)
    @NotNull
    private String uuid = UUID.randomUUID().toString();

	@Column(name = "BANK_LOT", length = 255)
	@Size(max = 255)
	private String bankLot;

	@Column(name = "BANK_REFERENCE", length = 255)
	@Size(max = 255)
	private String bankReference;

	@Column(name = "DEPOSIT_DATE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date depositDate;

	@Column(name = "BANK_COLLECTION_DATE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date bankCollectionDate;

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public OperationCategoryEnum getTransactionCategory() {
		return transactionCategory;
	}

	public void setTransactionCategory(OperationCategoryEnum transactionCategory) {
		this.transactionCategory = transactionCategory;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getMatchingAmount() {
		return matchingAmount;
	}

	public void setMatchingAmount(BigDecimal matchingAmount) {
		this.matchingAmount = matchingAmount;
	}

	public Date getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(Date transactionDate) {
		this.transactionDate = transactionDate;
	}

	public BigDecimal getUnMatchingAmount() {
		return unMatchingAmount;
	}

	public void setUnMatchingAmount(BigDecimal unMatchingAmount) {
		this.unMatchingAmount = unMatchingAmount;
	}

	public CustomerAccount getCustomerAccount() {
		return customerAccount;
	}

	public void setCustomerAccount(CustomerAccount customerAccount) {
		this.customerAccount = customerAccount;
	}

	public void setAccountCode(String accountCode) {
		this.accountCode = accountCode;
	}

	public String getAccountCode() {
		return accountCode;
	}

	public String getAccountCodeClientSide() {
		return accountCodeClientSide;
	}

	public void setAccountCodeClientSide(String accountCodeClientSide) {
		this.accountCodeClientSide = accountCodeClientSide;
	}

	public MatchingStatusEnum getMatchingStatus() {
		return matchingStatus;
	}

	public void setMatchingStatus(MatchingStatusEnum matchingStatus) {
		this.matchingStatus = matchingStatus;
	}

	public String getOccCode() {
		return occCode;
	}

	public void setOccCode(String occCode) {
		this.occCode = occCode;
	}

	public String getOccDescription() {
		return occDescription;
	}

	public void setOccDescription(String occDescription) {
		this.occDescription = occDescription;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((occCode == null) ? 0 : occCode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof AccountOperation)) {
            return false;
        }
        
		AccountOperation other = (AccountOperation) obj;
		if (occCode == null) {
			if (other.occCode != null)
				return false;
		} else if (!occCode.equals(other.occCode))
			return false;
		return true;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setMatchingAmounts(List<MatchingAmount> matchingAmounts) {
		this.matchingAmounts = matchingAmounts;
	}

	public List<MatchingAmount> getMatchingAmounts() {
		return matchingAmounts;
	}

	public boolean getExcludedFromDunning() {
		return excludedFromDunning;
	}

	public void setExcludedFromDunning(boolean excludedFromDunning) {
		this.excludedFromDunning = excludedFromDunning;
	}
	
	   @Override
	    public String getUuid() {
	        return uuid;
	    }

	    public void setUuid(String uuid) {
	        this.uuid = uuid;
	    }

	    @Override
	    public String clearUuid() {
	        String oldUuid = uuid;
	        uuid = UUID.randomUUID().toString();
	        return oldUuid;
	    }

		@Override
		public ICustomFieldEntity[] getParentCFEntities() {
			return null;
		}

		/**
		 * @return the orderNumber
		 */
		public String getOrderNumber() {
			return orderNumber;
		}

		/**
		 * @param orderNumber the orderNumber to set
		 */
		public void setOrderNumber(String orderNumber) {
			this.orderNumber = orderNumber;
		}

	public String getBankLot() {
		return bankLot;
	}

	public void setBankLot(String bankLot) {
		this.bankLot = bankLot;
	}

	public String getBankReference() {
		return bankReference;
	}

	public void setBankReference(String bankReference) {
		this.bankReference = bankReference;
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
}
