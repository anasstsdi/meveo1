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
package org.meveo.model.billing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Type;
import org.meveo.model.AuditableEntity;
import org.meveo.model.CustomFieldEntity;
import org.meveo.model.ICustomFieldEntity;
import org.meveo.model.ObservableEntity;
import org.meveo.model.order.Order;
import org.meveo.model.payments.PaymentMethodEnum;
import org.meveo.model.payments.RecordedInvoice;
import org.meveo.model.quote.Quote;

@Entity
@ObservableEntity
@Table(name = "BILLING_INVOICE", uniqueConstraints = @UniqueConstraint(columnNames = { "PROVIDER_ID", "INVOICE_NUMBER", "INVOICE_TYPE_ID" }))
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "BILLING_INVOICE_SEQ")
@CustomFieldEntity(cftCodePrefix = "INVOICE")
public class Invoice extends AuditableEntity implements ICustomFieldEntity {

	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "BILLING_ACCOUNT_ID")
	private BillingAccount billingAccount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "BILLING_RUN_ID")
	private BillingRun billingRun;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "RECORDED_INVOICE_ID")
	private RecordedInvoice recordedInvoice;

	@OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<InvoiceAgregate> invoiceAgregates = new ArrayList<InvoiceAgregate>();

	@Column(name = "INVOICE_NUMBER", length = 50)
	@Size(max = 50)
	private String invoiceNumber;

	@Column(name = "TEMPORARY_INVOICE_NUMBER", length = 60, unique = true)
	@Size(max = 60)
	private String temporaryInvoiceNumber;

	@Column(name = "PRODUCT_DATE")
	private Date productDate;

	@Column(name = "INVOICE_DATE")
	private Date invoiceDate;

	@Column(name = "DUE_DATE")
	private Date dueDate;

	@Column(name = "AMOUNT", precision = NB_PRECISION, scale = NB_DECIMALS)
	private BigDecimal amount;

	@Column(name = "DISCOUNT", precision = NB_PRECISION, scale = NB_DECIMALS)
	private BigDecimal discount;

	@Column(name = "AMOUNT_WITHOUT_TAX", precision = NB_PRECISION, scale = NB_DECIMALS)
	private BigDecimal amountWithoutTax;

	@Column(name = "AMOUNT_TAX", precision = NB_PRECISION, scale = NB_DECIMALS)
	private BigDecimal amountTax;

	@Column(name = "AMOUNT_WITH_TAX", precision = NB_PRECISION, scale = NB_DECIMALS)
	private BigDecimal amountWithTax;

	@Column(name = "NET_TO_PAY", precision = NB_PRECISION, scale = NB_DECIMALS)
	private BigDecimal netToPay;

	@Column(name = "PAYMENT_METHOD")
	@Enumerated(EnumType.STRING)
	private PaymentMethodEnum paymentMethod;

	@Column(name = "IBAN", length = 255)
	@Size(max = 255)
	private String iban;

	@Column(name = "ALIAS", length = 255)
	@Size(max = 255)
	private String alias;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "TRADING_CURRENCY_ID")
	private TradingCurrency tradingCurrency;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "TRADING_COUNTRY_ID")
	private TradingCountry tradingCountry;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "TRADING_LANGUAGE_ID")
	private TradingLanguage tradingLanguage;

	@OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
	private List<RatedTransaction> ratedTransactions = new ArrayList<RatedTransaction>();

	@Column(name = "COMMENT", length = 1200)
	@Size(max = 1200)
	private String comment;

	@Column(name = "PDF")
	@Basic(fetch = FetchType.LAZY)
	@Lob
	private byte[] pdf;

	@Type(type="numeric_boolean")
    @Column(name = "DETAILED_INVOICE")
	private boolean isDetailedInvoice = true;

	@ManyToOne
	@JoinColumn(name = "INVOICE_ID")
	private Invoice adjustedInvoice;

	@ManyToOne
	@JoinColumn(name = "INVOICE_TYPE_ID")
	private InvoiceType invoiceType;

	@Column(name = "UUID", nullable = false, updatable = false, length = 60)
	@Size(max = 60)
	@NotNull
	private String uuid = UUID.randomUUID().toString();

	@ManyToMany
	@JoinTable(name = "BILLING_LINKED_INVOICES", joinColumns = { @JoinColumn(name = "ID") }, inverseJoinColumns = { @JoinColumn(name = "LINKED_INVOICE_ID") })
	private Set<Invoice> linkedInvoices = new HashSet<>();
	
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "BILLING_INVOICES_ORDERS", joinColumns = @JoinColumn(name = "INVOICE_ID"), inverseJoinColumns = @JoinColumn(name = "ORDER_ID"))	
	private List<Order> orders = new ArrayList<Order>();	
	
    @ManyToOne
    @JoinColumn(name = "QUOTE_ID")
    private Quote quote;

	@Transient
	private Long invoiceAdjustmentCurrentSellerNb;

	@Transient
	private Long invoiceAdjustmentCurrentProviderNb;

	public List<RatedTransaction> getRatedTransactions() {
		return ratedTransactions;
	}

	public void setRatedTransactions(List<RatedTransaction> ratedTransactions) {
		this.ratedTransactions = ratedTransactions;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public Date getProductDate() {
		return productDate;
	}

	public void setProductDate(Date productDate) {
		this.productDate = productDate;
	}

	public Date getInvoiceDate() {
		return invoiceDate;
	}

	public void setInvoiceDate(Date invoiceDate) {
		this.invoiceDate = invoiceDate;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getDiscount() {
		return discount;
	}

	public void setDiscount(BigDecimal discount) {
		this.discount = discount;
	}

	public BigDecimal getAmountWithoutTax() {
		return amountWithoutTax;
	}

	public void setAmountWithoutTax(BigDecimal amountWithoutTax) {
		this.amountWithoutTax = amountWithoutTax;
	}

	public BigDecimal getAmountTax() {
		return amountTax;
	}

	public void setAmountTax(BigDecimal amountTax) {
		this.amountTax = amountTax;
	}

	public BigDecimal getAmountWithTax() {
		return amountWithTax;
	}

	public void setAmountWithTax(BigDecimal amountWithTax) {
		this.amountWithTax = amountWithTax;
	}

	public PaymentMethodEnum getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(PaymentMethodEnum paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public BillingAccount getBillingAccount() {
		return billingAccount;
	}

	public void setBillingAccount(BillingAccount billingAccount) {
		this.billingAccount = billingAccount;
	}

	public BillingRun getBillingRun() {
		return billingRun;
	}

	public void setBillingRun(BillingRun billingRun) {
		this.billingRun = billingRun;
	}

	public List<InvoiceAgregate> getInvoiceAgregates() {
		return invoiceAgregates;
	}

	public void setInvoiceAgregates(List<InvoiceAgregate> invoiceAgregates) {
		this.invoiceAgregates = invoiceAgregates;
	}

	public void addAmountWithTax(BigDecimal amountToAdd) {
		if (amountWithTax == null) {
			amountWithTax = BigDecimal.ZERO;
		}
		if (amountToAdd != null) {
			amountWithTax = amountWithTax.add(amountToAdd);
		}
	}

	public void addAmountWithoutTax(BigDecimal amountToAdd) {
		if (amountWithoutTax == null) {
			amountWithoutTax = BigDecimal.ZERO;
		}
		if (amountToAdd != null) {
			amountWithoutTax = amountWithoutTax.add(amountToAdd);
		}
	}

	public void addAmountTax(BigDecimal amountToAdd) {
		if (amountTax == null) {
			amountTax = BigDecimal.ZERO;
		}
		if (amountToAdd != null) {
			amountTax = amountTax.add(amountToAdd);
		}
	}

	/*
	 * public Blob getPdfBlob() { return pdfBlob; }
	 * 
	 * public void setPdfBlob(Blob pdfBlob) { this.pdfBlob = pdfBlob; }
	 */

	/*
	 * public byte[] getPdf() { byte[] result = null; try { if (pdfBlob != null)
	 * { int size; size = (int) pdfBlob.length(); result = pdfBlob.getBytes(1L,
	 * size); } } catch (SQLException e) {
	 * logger.error("Error while accessing pdf blob field in database. Return null."
	 * , e); } return result; }
	 * 
	 * public void setPdf(byte[] pdf) { this.pdfBlob =
	 * Hibernate.createBlob(pdf); }
	 */

	public byte[] getPdf() {
		return pdf;
	}

	public void setPdf(byte[] pdf) {
		this.pdf = pdf;
	}

	public String getTemporaryInvoiceNumber() {
		return temporaryInvoiceNumber;
	}

	public void setTemporaryInvoiceNumber(String temporaryInvoiceNumber) {
		this.temporaryInvoiceNumber = temporaryInvoiceNumber;
	}

    public String getInvoiceNumberOrTemporaryNumber() {
        if (invoiceNumber != null) {
            return invoiceNumber;
        } else {
            return "[" + temporaryInvoiceNumber + "]";
        }
    }
	
	public TradingCurrency getTradingCurrency() {
		return tradingCurrency;
	}

	public void setTradingCurrency(TradingCurrency tradingCurrency) {
		this.tradingCurrency = tradingCurrency;
	}

	public TradingCountry getTradingCountry() {
		return tradingCountry;
	}

	public void setTradingCountry(TradingCountry tradingCountry) {
		this.tradingCountry = tradingCountry;
	}

	public TradingLanguage getTradingLanguage() {
		return tradingLanguage;
	}

	public void setTradingLanguage(TradingLanguage tradingLanguage) {
		this.tradingLanguage = tradingLanguage;
	}

	public BigDecimal getNetToPay() {
		return netToPay;
	}

	public void setNetToPay(BigDecimal netToPay) {
		this.netToPay = netToPay;
	}

	public RecordedInvoice getRecordedInvoice() {
		return recordedInvoice;
	}

	public void setRecordedInvoice(RecordedInvoice recordedInvoice) {
		this.recordedInvoice = recordedInvoice;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public boolean isDetailedInvoice() {
		return isDetailedInvoice;
	}

	public void setDetailedInvoice(boolean isDetailedInvoice) {
		this.isDetailedInvoice = isDetailedInvoice;
	}

	public Invoice getAdjustedInvoice() {
		return adjustedInvoice;
	}

	public void setAdjustedInvoice(Invoice adjustedInvoice) {
		this.adjustedInvoice = adjustedInvoice;
	}

	public Long getInvoiceAdjustmentCurrentSellerNb() {
		return invoiceAdjustmentCurrentSellerNb;
	}

	public void setInvoiceAdjustmentCurrentSellerNb(Long invoiceAdjustmentCurrentSellerNb) {
		this.invoiceAdjustmentCurrentSellerNb = invoiceAdjustmentCurrentSellerNb;
	}

	public Long getInvoiceAdjustmentCurrentProviderNb() {
		return invoiceAdjustmentCurrentProviderNb;
	}

	public void setInvoiceAdjustmentCurrentProviderNb(Long invoiceAdjustmentCurrentProviderNb) {
		this.invoiceAdjustmentCurrentProviderNb = invoiceAdjustmentCurrentProviderNb;
	}

	@Override
	public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof Invoice)) {
            return false;
        }
        
		Invoice other = (Invoice) obj;
		if (other.getId() == null) {
			return false;
		} else if (!other.getId().equals(this.getId())) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		return id == null ? 0 : id.intValue();
	}

	/**
	 * @return the invoiceType
	 */
	public InvoiceType getInvoiceType() {
		return invoiceType;
	}

	/**
	 * @param invoiceType
	 *            the invoiceType to set
	 */
	public void setInvoiceType(InvoiceType invoiceType) {
		this.invoiceType = invoiceType;
	}
	
	

	/**
	 * @return the orders
	 */
	public List<Order> getOrders() {
		return orders;
	}

	/**
	 * @param orders the orders to set
	 */
	public void setOrders(List<Order> orders) {
		this.orders = orders;
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

	public Set<Invoice> getLinkedInvoices() {
		return linkedInvoices;
	}

	public void setLinkedInvoices(Set<Invoice> linkedInvoices) {
		this.linkedInvoices = linkedInvoices;
	}
	
	public void addInvoiceAggregate(InvoiceAgregate obj) {
		if (!invoiceAgregates.contains(obj)) {
			invoiceAgregates.add(obj);
		}
	}

    public List<SubCategoryInvoiceAgregate> getDiscountAgregates() {
        List<SubCategoryInvoiceAgregate> aggregates = new ArrayList<>();

        for (InvoiceAgregate invoiceAggregate : invoiceAgregates) {
            if (invoiceAggregate instanceof SubCategoryInvoiceAgregate && invoiceAggregate.isDiscountAggregate()) {
                aggregates.add((SubCategoryInvoiceAgregate) invoiceAggregate);
            }
        }
        
        return aggregates;
    }

    public List<RatedTransaction> getRatedTransactionsForCategory(WalletInstance wallet, InvoiceSubCategory invoiceSubCategory) {

        List<RatedTransaction> ratedTransactionsMatched = new ArrayList<>();

        for (RatedTransaction ratedTransaction : ratedTransactions) {           
            if (ratedTransaction.getWallet().equals(wallet) && ratedTransaction.getInvoiceSubCategory().equals(invoiceSubCategory)) {
                ratedTransactionsMatched.add(ratedTransaction);
            }

        }
        return ratedTransactionsMatched;
    }

	/**
	 * @return the quote
	 */
	public Quote getQuote() {
		return quote;
	}

	/**
	 * @param quote the quote to set
	 */
	public void setQuote(Quote quote) {
		this.quote = quote;
	}
    
}