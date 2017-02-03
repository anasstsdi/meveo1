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
package org.meveo.service.billing.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.meveo.admin.async.InvoicingAsync;
import org.meveo.admin.async.RatedTxInvoicingAsync;
import org.meveo.admin.async.SubListCreator;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.model.admin.User;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.BillingCycle;
import org.meveo.model.billing.BillingProcessTypesEnum;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.BillingRunStatusEnum;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoiceAgregate;
import org.meveo.model.billing.PostInvoicingReportsDTO;
import org.meveo.model.billing.PreInvoicingReportsDTO;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.RatedTransactionStatusEnum;
import org.meveo.model.billing.RejectedBillingAccount;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.billing.WalletOperationStatusEnum;
import org.meveo.model.crm.Provider;
import org.meveo.model.payments.PaymentMethodEnum;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.base.PersistenceService;

@Stateless
public class BillingRunService extends PersistenceService<BillingRun> {

	@Inject
	private WalletOperationService walletOperationService;

	@Inject
	private BillingAccountService billingAccountService;

	@EJB
	private InvoiceService invoiceService;
	
	@Inject
	private RatedTxInvoicingAsync ratedTxInvoicingAsync;
	
	@Inject
	private RatedTransactionService ratedTransactionService;

	@Inject
	private ResourceBundle resourceMessages;

	@Inject
	private InvoicingAsync invoicingAsync;

	public PreInvoicingReportsDTO generatePreInvoicingReports(BillingRun billingRun) throws BusinessException {
		log.debug("start generatePreInvoicingReports.......");

		PreInvoicingReportsDTO preInvoicingReportsDTO = new PreInvoicingReportsDTO();

		preInvoicingReportsDTO.setBillingCycleCode(billingRun.getBillingCycle() != null ? billingRun.getBillingCycle()
				.getCode() : null);
		preInvoicingReportsDTO.setBillingAccountNumber(billingRun.getBillingAccountNumber());
		preInvoicingReportsDTO.setLastTransactionDate(billingRun.getLastTransactionDate());
		preInvoicingReportsDTO.setInvoiceDate(billingRun.getInvoiceDate());
		preInvoicingReportsDTO.setBillableBillingAccountNumber(billingRun.getBillableBillingAcountNumber());
		preInvoicingReportsDTO.setAmoutWitountTax(billingRun.getPrAmountWithoutTax());

		BillingCycle billingCycle = billingRun.getBillingCycle();

		Date startDate = billingRun.getStartDate();
		Date endDate = billingRun.getEndDate();
		endDate = endDate != null ? endDate : new Date();
		List<BillingAccount> billingAccounts = new ArrayList<BillingAccount>();

		if (billingCycle != null) {
			billingAccounts = billingAccountService.findBillingAccounts(billingCycle, startDate, endDate,billingRun.getProvider());
		} else {
			String[] baIds = billingRun.getSelectedBillingAccounts().split(",");
			for (String id : Arrays.asList(baIds)) {
				Long baId = Long.valueOf(id);
				billingAccounts.add(billingAccountService.findById(baId));
			}
		}

		log.debug("BA in PreInvoicingReport: {}", billingAccounts.size());
		Integer checkBANumber = 0;
		Integer directDebitBANumber = 0;
		Integer tipBANumber = 0;
		Integer wiretransferBANumber = 0;
		Integer creditDebitCardBANumber = 0;

		Integer checkBillableBANumber = 0;
		Integer directDebitBillableBANumber = 0;
		Integer tipBillableBANumber = 0;
		Integer wiretransferBillableBANumber = 0;
		Integer creditDebitCardBillableBANumber = 0;

		BigDecimal checkBillableBAAmountHT = BigDecimal.ZERO;
		BigDecimal directDebitBillableBAAmountHT = BigDecimal.ZERO;
		BigDecimal tipBillableBAAmountHT = BigDecimal.ZERO;
		BigDecimal wiretransferBillableBAAmountHT = BigDecimal.ZERO;
		BigDecimal creditDebitCardBillableBAAmountHT = BigDecimal.ZERO;

		for (BillingAccount billingAccount : billingAccounts) {
		    PaymentMethodEnum paymentMethod= billingAccount.getPaymentMethod();
		    if(paymentMethod==null){
		        paymentMethod=billingAccount.getCustomerAccount().getPaymentMethod();
		    }
			switch (paymentMethod) {
			case CHECK:
				checkBANumber++;
				break;
			case DIRECTDEBIT:
				directDebitBANumber++;
				break;
			case TIP:
				tipBANumber++;
				break;
			case WIRETRANSFER:
				wiretransferBANumber++;
				break;
				
			case CARD:
				creditDebitCardBANumber++;
				break;	
				
			default:
				break;
			}

		}

		for (BillingAccount billingAccount : billingRun.getBillableBillingAccounts()) {
            PaymentMethodEnum paymentMethod= billingAccount.getPaymentMethod();
            if(paymentMethod==null){
                paymentMethod=billingAccount.getCustomerAccount().getPaymentMethod();
            }
            switch (paymentMethod) {
			case CHECK:
				checkBillableBANumber++;
				checkBillableBAAmountHT = checkBillableBAAmountHT.add(billingAccount.getBrAmountWithoutTax());
				break;
			case DIRECTDEBIT:
				directDebitBillableBANumber++;
				directDebitBillableBAAmountHT = directDebitBillableBAAmountHT.add(billingAccount
						.getBrAmountWithoutTax());
				break;
			case TIP:
				tipBillableBANumber++;
				tipBillableBAAmountHT = tipBillableBAAmountHT.add(billingAccount.getBrAmountWithoutTax());
				break;
			case WIRETRANSFER:
				wiretransferBillableBANumber++;
				wiretransferBillableBAAmountHT = wiretransferBillableBAAmountHT.add(billingAccount
						.getBrAmountWithoutTax());
				break;
			
			case CARD:
				creditDebitCardBillableBANumber++;
				creditDebitCardBillableBAAmountHT = creditDebitCardBillableBAAmountHT.add(billingAccount.getBrAmountWithoutTax());

			default:
				break;
			}
		}

		preInvoicingReportsDTO.setCheckBANumber(checkBANumber);
		preInvoicingReportsDTO.setCheckBillableBAAmountHT(round(checkBillableBAAmountHT, 2));
		preInvoicingReportsDTO.setCheckBillableBANumber(checkBillableBANumber);
		preInvoicingReportsDTO.setDirectDebitBANumber(directDebitBANumber);
		preInvoicingReportsDTO.setDirectDebitBillableBAAmountHT(round(directDebitBillableBAAmountHT, 2));
		preInvoicingReportsDTO.setDirectDebitBillableBANumber(directDebitBillableBANumber);
		preInvoicingReportsDTO.setTipBANumber(tipBANumber);
		preInvoicingReportsDTO.setTipBillableBAAmountHT(round(tipBillableBAAmountHT, 2));
		preInvoicingReportsDTO.setTipBillableBANumber(tipBillableBANumber);
		preInvoicingReportsDTO.setWiretransferBANumber(wiretransferBANumber);
		preInvoicingReportsDTO.setWiretransferBillableBAAmountHT(round(wiretransferBillableBAAmountHT, 2));
		preInvoicingReportsDTO.setWiretransferBillableBANumber(wiretransferBillableBANumber);
		preInvoicingReportsDTO.setCreditDebitCardBANumber(creditDebitCardBANumber);
		preInvoicingReportsDTO.setCreditDebitCardBillableBAAmountHT(round(creditDebitCardBillableBAAmountHT, 2));
		preInvoicingReportsDTO.setCreditDebitCardBillableBANumber(creditDebitCardBillableBANumber);

		return preInvoicingReportsDTO;
	}

	public PostInvoicingReportsDTO generatePostInvoicingReports(BillingRun billingRun) throws BusinessException {
		log.info("generatePostInvoicingReports billingRun=" + billingRun.getId());
		PostInvoicingReportsDTO postInvoicingReportsDTO = new PostInvoicingReportsDTO();

		BigDecimal globalAmountHT = BigDecimal.ZERO;
		BigDecimal globalAmountTTC = BigDecimal.ZERO;

		Integer positiveInvoicesNumber = 0;
		BigDecimal positiveInvoicesAmountHT = BigDecimal.ZERO;
		BigDecimal positiveInvoicesAmount = BigDecimal.ZERO;
		BigDecimal positiveInvoicesTaxAmount = BigDecimal.ZERO;

		Integer negativeInvoicesNumber = 0;
		BigDecimal negativeInvoicesAmountHT = BigDecimal.ZERO;
		BigDecimal negativeInvoicesTaxAmount = BigDecimal.ZERO;
		BigDecimal negativeInvoicesAmount = BigDecimal.ZERO;

		Integer emptyInvoicesNumber = 0;
		Integer electronicInvoicesNumber = 0;

		Integer checkInvoicesNumber = 0;
		Integer directDebitInvoicesNumber = 0;
		Integer tipInvoicesNumber = 0;
		Integer wiretransferInvoicesNumber = 0;
		Integer creditDebitCardInvoicesNumber = 0;

		BigDecimal checkAmuontHT = BigDecimal.ZERO;
		BigDecimal directDebitAmuontHT = BigDecimal.ZERO;
		BigDecimal tipAmuontHT = BigDecimal.ZERO;
		BigDecimal wiretransferAmuontHT = BigDecimal.ZERO;
		BigDecimal creditDebitCardAmountHT = BigDecimal.ZERO;

		BigDecimal checkAmuont = BigDecimal.ZERO;
		BigDecimal directDebitAmuont = BigDecimal.ZERO;
		BigDecimal tipAmuont = BigDecimal.ZERO;
		BigDecimal wiretransferAmuont = BigDecimal.ZERO;
		BigDecimal creditDebitCardAmount = BigDecimal.ZERO;

		for (Invoice invoice : billingRun.getInvoices()) {

			if (invoice.getAmountWithoutTax() != null && invoice.getAmountWithTax() != null) {
				switch (invoice.getPaymentMethod()) {
				case CHECK:
					checkInvoicesNumber++;
					checkAmuontHT = checkAmuontHT.add(invoice.getAmountWithoutTax());
					checkAmuont = checkAmuont.add(invoice.getAmountWithTax());
					break;
				case DIRECTDEBIT:
					directDebitInvoicesNumber++;
					directDebitAmuontHT = directDebitAmuontHT.add(invoice.getAmountWithoutTax());
					directDebitAmuont = directDebitAmuont.add(invoice.getAmountWithTax());
					break;
				case TIP:
					tipInvoicesNumber++;
					tipAmuontHT = tipAmuontHT.add(invoice.getAmountWithoutTax());
					tipAmuont = tipAmuont.add(invoice.getAmountWithTax());
					break;
				case WIRETRANSFER:
					wiretransferInvoicesNumber++;
					wiretransferAmuontHT = wiretransferAmuontHT.add(invoice.getAmountWithoutTax());
					wiretransferAmuont = wiretransferAmuont.add(invoice.getAmountWithTax());
					break;
				case CARD:
					creditDebitCardInvoicesNumber++;
					creditDebitCardAmountHT = creditDebitCardAmountHT.add(invoice.getAmountWithoutTax());
					creditDebitCardAmount = creditDebitCardAmount.add(invoice.getAmountWithTax());
					break;
					
				default:
					break;
				}
			}

			if (invoice.getAmountWithoutTax() != null && invoice.getAmountWithoutTax().compareTo(BigDecimal.ZERO) > 0) {
				positiveInvoicesNumber++;
				positiveInvoicesAmountHT = positiveInvoicesAmountHT.add(invoice.getAmountWithoutTax());
				positiveInvoicesTaxAmount = positiveInvoicesTaxAmount.add(invoice.getAmountTax() == null ? BigDecimal.ZERO : invoice.getAmountTax());
				positiveInvoicesAmount = positiveInvoicesAmount.add(invoice.getAmountWithTax());
			} else if (invoice.getAmountWithoutTax() == null
					|| invoice.getAmountWithoutTax().compareTo(BigDecimal.ZERO) == 0) {
				emptyInvoicesNumber++;
			} else {
				negativeInvoicesNumber++;
				negativeInvoicesAmountHT = negativeInvoicesAmountHT.add(invoice.getAmountWithoutTax());
				negativeInvoicesTaxAmount = negativeInvoicesTaxAmount.add(invoice.getAmountTax());
				negativeInvoicesAmount = negativeInvoicesAmount.add(invoice.getAmountWithTax());
			}

			if (invoice.getBillingAccount().getElectronicBilling()) {
				electronicInvoicesNumber++;
			}

			if (invoice.getAmountWithoutTax() != null && invoice.getAmountWithTax() != null) {
				globalAmountHT = globalAmountHT.add(invoice.getAmountWithoutTax());
				globalAmountTTC = globalAmountTTC.add(invoice.getAmountWithTax());
			}

		}

		postInvoicingReportsDTO.setInvoicesNumber(billingRun.getInvoiceNumber());
		postInvoicingReportsDTO.setCheckAmuont(checkAmuont);
		postInvoicingReportsDTO.setCheckAmuontHT(checkAmuontHT);
		postInvoicingReportsDTO.setCheckInvoicesNumber(checkInvoicesNumber);
		postInvoicingReportsDTO.setDirectDebitAmuont(directDebitAmuont);
		postInvoicingReportsDTO.setDirectDebitAmuontHT(directDebitAmuontHT);
		postInvoicingReportsDTO.setDirectDebitInvoicesNumber(directDebitInvoicesNumber);
		postInvoicingReportsDTO.setElectronicInvoicesNumber(electronicInvoicesNumber);
		postInvoicingReportsDTO.setEmptyInvoicesNumber(emptyInvoicesNumber);

		postInvoicingReportsDTO.setPositiveInvoicesAmountHT(positiveInvoicesAmountHT);
		postInvoicingReportsDTO.setPositiveInvoicesAmount(positiveInvoicesAmount);
		postInvoicingReportsDTO.setPositiveInvoicesTaxAmount(positiveInvoicesTaxAmount);
		postInvoicingReportsDTO.setPositiveInvoicesNumber(positiveInvoicesNumber);

		postInvoicingReportsDTO.setNegativeInvoicesAmountHT(negativeInvoicesAmountHT);
		postInvoicingReportsDTO.setNegativeInvoicesAmount(negativeInvoicesAmount);
		postInvoicingReportsDTO.setNegativeInvoicesTaxAmount(negativeInvoicesTaxAmount);
		postInvoicingReportsDTO.setNegativeInvoicesNumber(negativeInvoicesNumber);

		postInvoicingReportsDTO.setTipAmuont(tipAmuont);
		postInvoicingReportsDTO.setTipAmuontHT(tipAmuontHT);
		postInvoicingReportsDTO.setTipInvoicesNumber(tipInvoicesNumber);
		postInvoicingReportsDTO.setWiretransferAmuont(wiretransferAmuont);
		postInvoicingReportsDTO.setWiretransferAmuontHT(wiretransferAmuontHT);
		postInvoicingReportsDTO.setWiretransferInvoicesNumber(wiretransferInvoicesNumber);
		
		postInvoicingReportsDTO.setCreditDebitCardAmount(creditDebitCardAmount);
		postInvoicingReportsDTO.setCreditDebitCardAmountHT(creditDebitCardAmountHT);
		postInvoicingReportsDTO.setCreditDebitCardInvoicesNumber(creditDebitCardInvoicesNumber);
		postInvoicingReportsDTO.setGlobalAmount(globalAmountHT);

		return postInvoicingReportsDTO;
	}

	public static BigDecimal round(BigDecimal amount, int decimal) {
		if (amount == null) {
			return null;
		}
		amount = amount.setScale(decimal, RoundingMode.HALF_UP);

		return amount;
	}

	public void cancel(BillingRun billingRun, User currentUser) throws BusinessException {
		billingRun.setStatus(BillingRunStatusEnum.CANCELED);
		update(billingRun, currentUser);
	}
	
	@SuppressWarnings("unchecked")
	public void cleanBillingRun(BillingRun billingRun) {
		Query queryTrans = getEntityManager()
				.createQuery(
						"update "
								+ RatedTransaction.class.getName()
								+ " set invoice=null,invoiceAgregateF=null,invoiceAgregateR=null,invoiceAgregateT=null,status=:status where billingRun=:billingRun");
		queryTrans.setParameter("billingRun", billingRun);
		queryTrans.setParameter("status", RatedTransactionStatusEnum.OPEN);
		queryTrans.executeUpdate();
		
		Query queryAgregate = getEntityManager()
				.createQuery(
						"from "
								+ InvoiceAgregate.class.getName()+" where billingRun=:billingRun");
		queryAgregate.setParameter("billingRun", billingRun);
		List<InvoiceAgregate> invoiceAgregates=(List<InvoiceAgregate>)queryAgregate.getResultList();
		for(InvoiceAgregate invoiceAgregate:invoiceAgregates){
			
			getEntityManager().remove(invoiceAgregate);
		}
		getEntityManager().flush();

		Query queryInvoices = getEntityManager().createQuery(
				"delete from " + Invoice.class.getName() + " where billingRun=:billingRun");
		queryInvoices.setParameter("billingRun", billingRun);
		queryInvoices.executeUpdate();

		Query queryBA = getEntityManager().createQuery(
				"update " + BillingAccount.class.getName() + " set billingRun=null where billingRun=:billingRun");
		queryBA.setParameter("billingRun", billingRun);
		queryBA.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	public boolean isActiveBillingRunsExist(Provider provider) {
		QueryBuilder qb = new QueryBuilder(BillingRun.class, "c");
		qb.startOrClause();
		qb.addCriterionEnum("c.status", BillingRunStatusEnum.NEW);
		qb.addCriterionEnum("c.status", BillingRunStatusEnum.PREVALIDATED);
		qb.addCriterionEnum("c.status", BillingRunStatusEnum.POSTINVOICED);
		qb.addCriterionEnum("c.status", BillingRunStatusEnum.PREINVOICED);
		qb.endOrClause();
		qb.addCriterionEntity("c.provider", provider);
		List<BillingRun> billingRuns = qb.getQuery(getEntityManager()).getResultList();

		return billingRuns != null && billingRuns.size() > 0 ? true : false;
	}

	public void retateBillingRunTransactions(BillingRun billingRun, User currentUser) throws BusinessException {
		for (RatedTransaction ratedTransaction : billingRun.getRatedTransactions()) {
			WalletOperation walletOperation = walletOperationService.findById(ratedTransaction.getWalletOperationId());
			walletOperation.setStatus(WalletOperationStatusEnum.TO_RERATE);
			walletOperationService.update(walletOperation, currentUser);
		}
	}

	public List<BillingRun> getbillingRuns(Provider provider, BillingRunStatusEnum... status) {
		return getBillingRuns(provider,null,status);
	}
	@SuppressWarnings("unchecked")
	public List<BillingRun> getBillingRuns(Provider provider,String code, BillingRunStatusEnum... status){
		
		BillingRunStatusEnum bRStatus;
		log.debug("getbillingRuns for provider " + provider == null ? "null" : provider.getCode());
		QueryBuilder qb = new QueryBuilder(BillingRun.class, "c", null, provider);
		
		if (code != null) {
			qb.addCriterion("c.billingCycle.code", "=", code, false);
		}

		qb.startOrClause();
		if (status != null) {
			for (int i = 0; i < status.length; i++) {
				bRStatus = status[i];
				qb.addCriterionEnum("c.status", bRStatus);
			}
		}
		qb.endOrClause();

		List<BillingRun> billingRuns = qb.getQuery(getEntityManager()).getResultList();

		for (BillingRun br : billingRuns) {
			getEntityManager().refresh(br);
		}

		return billingRuns;
	}

	public List<BillingRun> getValidatedBillingRuns() {
		return getValidatedBillingRuns(getCurrentProvider());
	}

	public List<BillingRun> getValidatedBillingRuns(Provider provider) {
		return getValidatedBillingRuns(getEntityManager(), provider);
	}

	@SuppressWarnings("unchecked")
	public List<BillingRun> getValidatedBillingRuns(EntityManager em, Provider provider) {
		QueryBuilder qb = new QueryBuilder(BillingRun.class, "c", null, provider);
		qb.addCriterionEnum("c.status", BillingRunStatusEnum.VALIDATED);
		qb.addBooleanCriterion("c.xmlInvoiceGenerated", false);
		List<BillingRun> billingRuns = qb.getQuery(em).getResultList();

		return billingRuns;

	}

	public BillingRun getBillingRunById(long id, Provider provider) {
		return getBillingRunById(getEntityManager(), id, provider);
	}

	public BillingRun getBillingRunById(EntityManager em, long id, Provider provider) {
		QueryBuilder qb = new QueryBuilder(BillingRun.class, "b");
		qb.addCriterionEntity("provider", provider);
		qb.addCriterion("id", "=", id, true);

		try {
			return (BillingRun) qb.getQuery(em).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<BillingAccount> getBillingAccounts(BillingRun billingRun) {
		List<BillingAccount> result = null;
		BillingCycle billingCycle = billingRun.getBillingCycle();

		log.debug("getBillingAccounts for billingRun {}", billingRun.getId());

		Object[] ratedTransactionsAmounts = null;
		if (billingCycle != null) {
			Date startDate = billingRun.getStartDate();
			Date endDate = billingRun.getEndDate();

			if (startDate != null && endDate == null) {
				endDate = new Date();
			}

			if (startDate != null) {
				ratedTransactionsAmounts = (Object[]) getEntityManager()
						.createNamedQuery("RatedTransaction.sumbillingRunByCycle")
						.setParameter("status", RatedTransactionStatusEnum.OPEN)
						.setParameter("billingCycle", billingCycle).setParameter("startDate", startDate)
						.setParameter("endDate", endDate)
						.setParameter("lastTransactionDate", billingRun.getLastTransactionDate())
						.setParameter("provider", billingRun.getProvider())
						.getSingleResult();
			} else {
				ratedTransactionsAmounts = (Object[]) getEntityManager()
						.createNamedQuery("RatedTransaction.sumbillingRunByCycleNoDate")
						.setParameter("status", RatedTransactionStatusEnum.OPEN)
						.setParameter("billingCycle", billingCycle)
                        .setParameter("lastTransactionDate", billingRun.getLastTransactionDate())
                        .setParameter("provider", billingRun.getProvider())
                        .getSingleResult();
			}

			result = billingAccountService.findBillingAccounts(billingCycle, startDate, endDate,billingRun.getProvider());
		} else {
			result = new ArrayList<BillingAccount>();
			String[] baIds = billingRun.getSelectedBillingAccounts().split(",");

			for (String id : Arrays.asList(baIds)) {
				Long baId = Long.valueOf(id);
				result.add(billingAccountService.findById(baId));
			}

			ratedTransactionsAmounts = (Object[]) getEntityManager()
					.createNamedQuery("RatedTransaction.sumbillingRunByList")
					.setParameter("status", RatedTransactionStatusEnum.OPEN)
					.setParameter("billingAccountList", result)
					.setParameter("lastTransactionDate", billingRun.getLastTransactionDate())
					.getSingleResult();
		}

		if (ratedTransactionsAmounts != null) {
			billingRun.setPrAmountWithoutTax((BigDecimal) ratedTransactionsAmounts[0]);
			billingRun.setPrAmountWithTax((BigDecimal) ratedTransactionsAmounts[1]);
			billingRun.setPrAmountTax((BigDecimal) ratedTransactionsAmounts[2]);
		} else {
            billingRun.setPrAmountWithoutTax(BigDecimal.ZERO);
            billingRun.setPrAmountWithTax(BigDecimal.ZERO);
            billingRun.setPrAmountTax(BigDecimal.ZERO);
		}

		updateNoCheck(billingRun);

		return result;
	}

	@SuppressWarnings("unchecked")
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void createAgregatesAndInvoice(Long billingRunId,Date lastTransactionDate, User currentUser,long nbRuns,long waitingMillis) throws BusinessException {
		List<BillingAccount> billingAccounts = getEntityManager()
				.createNamedQuery("BillingAccount.listByBillingRunId", BillingAccount.class)
				.setParameter("billingRunId", billingRunId)
                .getResultList();
    	SubListCreator subListCreator;
		try {
			subListCreator = new SubListCreator(billingAccounts,(int) nbRuns);
		} catch (Exception e1) {
			throw new BusinessException("cannot create  agregates and invoice with nbRuns="+nbRuns);
		}
    	List<Future<String>> asyncReturns =  new ArrayList<Future<String>>();
		while (subListCreator.isHasNext()) {
			asyncReturns.add(ratedTxInvoicingAsync.launchAndForget((List<BillingAccount>) subListCreator.getNextWorkSet(), billingRunId, currentUser));
			try {
				Thread.sleep(waitingMillis);
			} catch (InterruptedException e) {
				log.error("Failed to create agregates and invoice waiting for thread",e);
				throw new BusinessException(e);
			} 
		}
		for(Future<String> futureItsNow : asyncReturns){
			 try {
				futureItsNow.get();
			} catch (InterruptedException | ExecutionException e) {
				log.error("Failed to create agregates and invoice getting future",e);
				throw new BusinessException(e);
			}	
		}
	
	}



	public BillingRun launchExceptionalInvoicing(List<Long> billingAccountIds, Date invoiceDate,
			Date lastTransactionDate,BillingProcessTypesEnum processType,User currentUser) throws BusinessException{
		log.info("launchExceptionelInvoicing...");
		Provider currentProvider = currentUser.getProvider();

		ParamBean param = ParamBean.getInstance();
		String allowManyInvoicing = param.getProperty("billingRun.allowManyInvoicing", "true");
		boolean isAllowed = Boolean.parseBoolean(allowManyInvoicing);
		log.info("launchInvoicing allowManyInvoicing=#", isAllowed);
		if (isActiveBillingRunsExist(currentProvider) && !isAllowed) {
			throw new BusinessException(resourceMessages.getString("error.invoicing.alreadyLunched"));				
		}
		
		BillingRun billingRun = new BillingRun();
		billingRun.setStatus(BillingRunStatusEnum.NEW);
		billingRun.setProcessDate(new Date());
		billingRun.setProcessType(processType);
		billingRun.setProvider(currentProvider);
		String selectedBillingAccounts = "";
		String sep = "";
		boolean isBillable = false;

		if(lastTransactionDate == null){
			lastTransactionDate = new Date();
		}

		BillingAccount currentBA = null;
		for (Long baId : billingAccountIds) {
			currentBA = billingAccountService.findById(baId);
			if(currentBA == null){
				throw new BusinessException("BillingAccount whit id="+baId+" does not exists");
			}
			selectedBillingAccounts = selectedBillingAccounts + sep + baId;
			sep = ",";
			if (!isBillable && ratedTransactionService.isBillingAccountBillable(currentBA, lastTransactionDate)) {
				isBillable = true;
			}
		}

		if (!isBillable) {
			throw new BusinessException(resourceMessages.getString("error.invoicing.noTransactions"));				
		}
		log.debug("selectedBillingAccounts=" + selectedBillingAccounts);
		billingRun.setSelectedBillingAccounts(selectedBillingAccounts);

		billingRun.setInvoiceDate(invoiceDate);
		billingRun.setLastTransactionDate(lastTransactionDate);
		create(billingRun, currentUser);
		commit();
		return billingRun;
	}
	
	private void incrementInvoiceDatesAndValidate(BillingRun billingRun,User currentUser) throws BusinessException{
		log.debug("incrementInvoiceDatesAndValidate");
		for (Invoice invoice : billingRun.getInvoices()) {			
			invoice.setInvoiceNumber(invoiceService.getInvoiceNumber(invoice, currentUser));
			invoice.setPdf(null);
			BillingAccount billingAccount = invoice.getBillingAccount();
			Date initCalendarDate = billingAccount.getSubscriptionDate();
			if(initCalendarDate==null){
				initCalendarDate=billingAccount.getAuditable().getCreated();
			}
			Date nextCalendarDate = billingAccount.getBillingCycle().getNextCalendarDate(initCalendarDate);
			billingAccount.setNextInvoiceDate(nextCalendarDate);
			billingAccount.updateAudit(currentUser);			
			invoiceService.update(invoice, currentUser);
		}
		billingRun.setStatus(BillingRunStatusEnum.VALIDATED);
		billingRun.updateAudit(currentUser);
		update(billingRun,currentUser);
	}
	
	@SuppressWarnings("unchecked")
	public void validate(Long billingRunId,User currentUser,long nbRuns,long waitingMillis) throws Exception{
		BillingRun billingRun = findById(billingRunId);
		if(billingRun == null){
			throw  new BusinessException("Cant find BillingRun with id:"+billingRunId);
		}
		log.debug("validate, billingRun status={}",billingRun.getStatus());
		if (BillingRunStatusEnum.NEW.equals(billingRun.getStatus())) {
			refreshOrRetrieve(billingRun);
			List<BillingAccount> billingAccounts = getBillingAccounts(billingRun);
			log.info("Nb billingAccounts to process={}",
					(billingAccounts != null ? billingAccounts.size() : 0));

			if (billingAccounts != null && billingAccounts.size() > 0) {
				int billableBA = 0;
				SubListCreator subListCreator = new SubListCreator(billingAccounts,(int)nbRuns);
				List<Future<Integer>> asyncReturns = new ArrayList<Future<Integer>>();
				while (subListCreator.isHasNext()) {
					Future<Integer> count = invoicingAsync.launchAndForget((List<BillingAccount>) subListCreator.getNextWorkSet(), billingRun, currentUser);
					asyncReturns.add(count);
					try {
						Thread.sleep(waitingMillis);
					} catch (InterruptedException e) {
						log.error("", e);
					} 
				}

				for(Future<Integer> futureItsNow : asyncReturns){
					billableBA+= futureItsNow.get().intValue();	
				}

				log.info("Total billableBA:"+billableBA);

				updateBillingRun(billingRun.getId(),currentUser,billingAccounts.size(),billableBA,BillingRunStatusEnum.PREINVOICED,new Date());

				if (billingRun.getProcessType() == BillingProcessTypesEnum.AUTOMATIC
						|| billingRun.getProvider().isAutomaticInvoicing()) {
					createAgregatesAndInvoice(billingRun.getId(),billingRun.getLastTransactionDate(), currentUser,nbRuns,waitingMillis);										
					updateBillingRun(billingRun.getId(),currentUser,null,null,BillingRunStatusEnum.POSTINVOICED,null);
				}
			}
		} else if (BillingRunStatusEnum.PREVALIDATED.equals(billingRun.getStatus())) {
			createAgregatesAndInvoice(billingRun.getId(),billingRun.getLastTransactionDate(), currentUser,nbRuns,waitingMillis);								
			updateBillingRun(billingRun.getId(),currentUser,null,null,BillingRunStatusEnum.POSTINVOICED,null);
		} else if (BillingRunStatusEnum.POSTVALIDATED.equals(billingRun.getStatus())) {
			incrementInvoiceDatesAndValidate(billingRun, currentUser);
		}
	}
	
	public void forceValidate(Long billingRunId,User currentUser) throws BusinessException{
		BillingRun billingRun = findById(billingRunId);
		if(billingRun == null){
			throw  new BusinessException("Cant find BillingRun with id:"+billingRunId);
		}
		log.debug("forceValidate, billingRun status={}",billingRun.getStatus());
		switch(billingRun.getStatus()){
		case POSTINVOICED:
		case POSTVALIDATED:
			incrementInvoiceDatesAndValidate(billingRun, currentUser);
			break;
		case PREINVOICED:
		case PREVALIDATED:
			createAgregatesAndInvoice(billingRun.getId(),billingRun.getLastTransactionDate(), currentUser,1,0);								
			updateBillingRun(billingRun.getId(),currentUser,1,0,BillingRunStatusEnum.POSTINVOICED,null);
			break;
		case VALIDATED:
		case CANCELED:
		case NEW:
		default:
			throw new BusinessException("BillingRun with status NEW,VALIDATED or CANCELED cannot be validated");
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void updateBillingRun(Long billingRunId ,User currentUser,Integer sizeBA,Integer billableBA,BillingRunStatusEnum status,Date dateStatus) throws BusinessException {
		BillingRun billingRun = findById(billingRunId, currentUser.getProvider());
		if(billingRun == null){
			throw  new BusinessException("Cant find BillingRun with id:"+billingRunId);
		}
		if(sizeBA != null){
			billingRun.setBillingAccountNumber(sizeBA);
		}
		if(billableBA != null){
			billingRun.setBillableBillingAcountNumber(billableBA);
		}
		if(dateStatus != null){
			billingRun.setProcessDate(dateStatus);
		}
		billingRun.setStatus(status);
		billingRun.updateAudit(currentUser);
		updateNoCheck(billingRun);
	}

	public boolean launchInvoicingRejectedBA(BillingRun br, User currentUser) throws BusinessException {
		boolean result = false;
		BillingRun billingRun = new BillingRun();
		billingRun.setStatus(BillingRunStatusEnum.NEW);
		billingRun.setProcessDate(new Date());
		BillingCycle billingCycle = br.getBillingCycle();
		if (billingCycle != null && billingCycle.getInvoiceDateProductionDelay() != null) {
			billingRun.setInvoiceDate(DateUtils.addDaysToDate(billingRun.getProcessDate(), billingCycle.getInvoiceDateProductionDelay()));
		} else {
			billingRun.setInvoiceDate(br.getProcessDate());
		}
		if (billingCycle != null && billingCycle.getTransactionDateDelay() != null) {
			billingRun.setLastTransactionDate(DateUtils.addDaysToDate(billingRun.getProcessDate(), billingCycle.getTransactionDateDelay()));
		} else {
			billingRun.setLastTransactionDate(billingRun.getProcessDate());
		}
		billingRun.setProcessType(br.getProcessType());
		billingRun.setProvider(br.getProvider());
		String selectedBillingAccounts = "";
		String sep = "";
		for (RejectedBillingAccount ba : br.getRejectedBillingAccounts()) {
			selectedBillingAccounts = selectedBillingAccounts + sep + ba.getId();
			sep = ",";
			if (!result && ratedTransactionService.isBillingAccountBillable(ba.getBillingAccount(), billingRun.getLastTransactionDate())) {
				result = true;
				break;
			}
		}
		if (result) {
			log.debug("selectedBillingAccounts=" + selectedBillingAccounts);
			billingRun.setSelectedBillingAccounts(selectedBillingAccounts);
			create(billingRun, currentUser);
		}
		return result;
	}

}
