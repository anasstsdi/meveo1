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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.IncorrectSusbcriptionException;
import org.meveo.admin.exception.UnrolledbackBusinessException;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.User;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.BillingRunStatusEnum;
import org.meveo.model.billing.CategoryInvoiceAgregate;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoiceAgregate;
import org.meveo.model.billing.InvoiceCategory;
import org.meveo.model.billing.InvoiceSubCategory;
import org.meveo.model.billing.InvoiceSubcategoryCountry;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.RatedTransactionStatusEnum;
import org.meveo.model.billing.SubCategoryInvoiceAgregate;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.Tax;
import org.meveo.model.billing.TaxInvoiceAgregate;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.billing.WalletInstance;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.billing.WalletOperationStatusEnum;
import org.meveo.model.catalog.DiscountPlan;
import org.meveo.model.catalog.DiscountPlanItem;
import org.meveo.model.filter.Filter;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.service.api.dto.ConsumptionDTO;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.ValueExpressionWrapper;
import org.meveo.service.catalog.impl.CatMessagesService;
import org.meveo.service.catalog.impl.InvoiceSubCategoryService;
import org.meveo.service.filter.FilterService;
import org.meveo.service.payments.impl.CustomerAccountService;

@Stateless
public class RatedTransactionService extends PersistenceService<RatedTransaction> {
	
	@Inject
	private UserAccountService userAccountService;

	@Inject
	private InvoiceAgregateService invoiceAgregateService;

	@Inject
	private InvoiceSubCategoryService invoiceSubCategoryService;

	@Inject
	private CustomerAccountService customerAccountService;

	@Inject
	private WalletOperationService walletOperationService;
	
	@Inject
	private BillingAccountService billingAccountService;
	
	@Inject
	private CatMessagesService catMessagesService;
	
	@Inject
	private FilterService filterService;
	
	@Inject
	private ResourceBundle resourceMessages;


	private static final BigDecimal HUNDRED = new BigDecimal("100");

	public List<RatedTransaction> getRatedTransactionsInvoiced(UserAccount userAccount) {
		if (userAccount == null || userAccount.getWallet() == null) {
			return null;
		}
		return (List<RatedTransaction>) getEntityManager()
				.createNamedQuery("RatedTransaction.listInvoiced", RatedTransaction.class)
				.setParameter("wallet", userAccount.getWallet()).getResultList();
	}

	@SuppressWarnings("unchecked")
	// FIXME: edward please use Named queries
	public ConsumptionDTO getConsumption(Subscription subscription, String infoType, Integer billingCycle,
			boolean sumarizeConsumption) throws IncorrectSusbcriptionException {

		Date lastBilledDate = null;
		ConsumptionDTO consumptionDTO = new ConsumptionDTO();

		// If billing has been run already, use last billing date plus a day as
		// filtering FROM value
		// Otherwise leave it null, so it wont be included in a query
		if (subscription.getUserAccount().getBillingAccount().getBillingRun() != null) {
			lastBilledDate = subscription.getUserAccount().getBillingAccount().getBillingRun().getEndDate();
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(lastBilledDate);
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			lastBilledDate = calendar.getTime();

		}

		if (sumarizeConsumption) {

			QueryBuilder qb = new QueryBuilder("select sum(amount1WithTax), sum(usageAmount) from "
					+ RatedTransaction.class.getSimpleName());
			qb.addCriterionEntity("subscription", subscription);
			qb.addCriterion("subUsageCode1", "=", infoType, false);
			qb.addCriterionDateRangeFromTruncatedToDay("usageDate", lastBilledDate);
			String baseSql = qb.getSqlString();

			// Summarize invoiced transactions
			String sql = baseSql + " and status='BILLED'";

			Query query = getEntityManager().createQuery(sql);

			for (Entry<String, Object> param : qb.getParams().entrySet()) {
				query.setParameter(param.getKey(), param.getValue());
			}

			Object[] results = (Object[]) query.getSingleResult();

			consumptionDTO.setAmountCharged((BigDecimal) results[0]);
			consumptionDTO.setConsumptionCharged(((Long) results[1]).intValue());

			// Summarize not invoiced transactions
			sql = baseSql + " and status<>'BILLED'";

			query = getEntityManager().createQuery(sql);

			for (Entry<String, Object> param : qb.getParams().entrySet()) {
				query.setParameter(param.getKey(), param.getValue());
			}

			results = (Object[]) query.getSingleResult();

			consumptionDTO.setAmountUncharged((BigDecimal) results[0]);
			consumptionDTO.setConsumptionUncharged(((Long) results[1]).intValue());

		} else {

			QueryBuilder qb = new QueryBuilder(
					"select sum(amount1WithTax), sum(usageAmount), groupingId, case when status='BILLED' then 'true' else 'false' end from "
							+ RatedTransaction.class.getSimpleName());
			qb.addCriterionEntity("subscription", subscription);
			qb.addCriterion("subUsageCode1", "=", infoType, false);
			qb.addCriterionDateRangeFromTruncatedToDay("usageDate", lastBilledDate);
			qb.addSql("groupingId is not null");
			String sql = qb.getSqlString()
					+ " group by groupingId, case when status='BILLED' then 'true' else 'false' end";

			Query query = getEntityManager().createQuery(sql);

			for (Entry<String, Object> param : qb.getParams().entrySet()) {
				query.setParameter(param.getKey(), param.getValue());
			}

			List<Object[]> results = (List<Object[]>) query.getResultList();

			for (Object[] result : results) {

				BigDecimal amount = (BigDecimal) result[0];
				int consumption = ((Long) result[1]).intValue();
				boolean charged = Boolean.parseBoolean((String) result[3]);
				// boolean roaming =
				// RatedTransaction.translateGroupIdToRoaming(groupId);
				// boolean upload =
				// RatedTransaction.translateGroupIdToUpload(groupId);

				if (charged) {

					// if (!roaming && !upload) {
					consumptionDTO.setIncomingNationalConsumptionCharged(consumption);
					// } else if (roaming && !upload) {
					// consumptionDTO.setIncomingRoamingConsumptionCharged(consumption);
					// } else if (!roaming && upload) {
					// consumptionDTO.setOutgoingNationalConsumptionCharged(consumption);
					// } else {
					// consumptionDTO.setOutgoingRoamingConsumptionCharged(consumption);
					// }

					consumptionDTO.setConsumptionCharged(consumptionDTO.getConsumptionCharged() + consumption);
					consumptionDTO.setAmountCharged(consumptionDTO.getAmountCharged().add(amount));

				} else {
					// if (!roaming && !upload) {
					consumptionDTO.setIncomingNationalConsumptionUncharged(consumption);
					// } else if (roaming && !upload) {
					// consumptionDTO.setIncomingRoamingConsumptionUncharged(consumption);
					// } else if (!roaming && upload) {
					// consumptionDTO.setOutgoingNationalConsumptionUncharged(consumption);
					// } else {
					// consumptionDTO.setOutgoingRoamingConsumptionUncharged(consumption);
					// }
					consumptionDTO.setConsumptionUncharged(consumptionDTO.getConsumptionUncharged() + consumption);
					consumptionDTO.setAmountUncharged(consumptionDTO.getAmountUncharged().add(amount));
				}
			}
		}

		return consumptionDTO;

	}

	public void createInvoiceAndAgregates(BillingAccount billingAccount, Invoice invoice,
			Filter ratedTransactionFilter,String orderNumber,Date lastTransactionDate, User currentUser)
			throws BusinessException {
		createInvoiceAndAgregates(billingAccount, invoice,ratedTransactionFilter, null, orderNumber, lastTransactionDate, currentUser, false, false);
	}

    @SuppressWarnings({ "unchecked", "rawtypes", "unused" })
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createInvoiceAndAgregates(BillingAccount billingAccount, Invoice invoice, Filter ratedTransactionFilter, List<RatedTransaction> ratedTransactions,
            String orderNumber, Date lastTransactionDate, User currentUser, boolean isInvoiceAdjustment, boolean isVirtual) throws BusinessException {		
		boolean entreprise = billingAccount.getProvider().isEntreprise();
		int rounding = billingAccount.getProvider().getRounding()==null?2:billingAccount.getProvider().getRounding();
		BigDecimal nonEnterprisePriceWithTax = BigDecimal.ZERO;
		String languageCode = billingAccount.getTradingLanguage().getLanguage().getLanguageCode();
		List<UserAccount> userAccounts = userAccountService.listByBillingAccount(billingAccount);				
		boolean isExonerated = billingAccountService.isExonerated(billingAccount);
        if (ratedTransactionFilter != null) {
            ratedTransactions = (List<RatedTransaction>) filterService.filteredListAsObjects(ratedTransactionFilter, currentUser);
            if(ratedTransactions == null || ratedTransactions.isEmpty()){
            	throw new BusinessException(resourceMessages.getString("error.invoicing.noTransactions"));
            }            	
        }
        for (UserAccount userAccount : userAccounts) {
            WalletInstance wallet = userAccount.getWallet();
            List<Object[]> invoiceSubCats = new ArrayList<>();
	
	            if (ratedTransactionFilter != null || !StringUtils.isBlank(orderNumber) || isVirtual) {

                if (!StringUtils.isBlank(orderNumber)) {
                    List<RatedTransaction> orderRatedTransactions = null;
                    orderRatedTransactions = (List<RatedTransaction>) getEntityManager().createNamedQuery("RatedTransaction.listToInvoiceByOrderNumber", RatedTransaction.class)
                        .setParameter("wallet", wallet).setParameter("orderNumber", orderNumber).getResultList();
                    if (ratedTransactions == null){
                        ratedTransactions = new ArrayList<>();
                    }
                    ratedTransactions.addAll(orderRatedTransactions);
                }
                if (ratedTransactions == null || ratedTransactions.isEmpty()) {
                    continue;
                }
                for (RatedTransaction ratedTransaction : ratedTransactions) {
                	if (ratedTransactionFilter != null){
	                	if(ratedTransaction.getStatus() != RatedTransactionStatusEnum.OPEN){
	                		throw new BusinessException("ratedTransactionFilter should return only opened rated transactions");
	                	}	
	                	if(!ratedTransaction.getWallet().getUserAccount().getBillingAccount().equals(billingAccount)  ){
	                		throw new BusinessException("ratedTransactionFilter should return only rated transaction for billingAccount:"+billingAccount.getCode());
	                	}   
                	}
                    Object[] record = new Object[5];
                    record[0] = ratedTransaction.getInvoiceSubCategory().getId();
                    record[1] = ratedTransaction.getAmountWithoutTax();
                    record[2] = ratedTransaction.getAmountWithTax();
                    record[3] = ratedTransaction.getAmountTax();
                    record[4] = ratedTransaction.getQuantity();
                    boolean recordValid = true;
                    // check status
                    recordValid &= (ratedTransaction.getStatus() == RatedTransactionStatusEnum.OPEN);
                    // wallet
                    recordValid &= ratedTransaction.getWallet().getId() == wallet.getId();
                    // usageDate
                    if (lastTransactionDate != null) {
                        recordValid &= ratedTransaction.getUsageDate().before(lastTransactionDate);
                    }
                    // invoice not set
                    recordValid &= (ratedTransaction.getInvoice() == null);

                    if (recordValid) {
                        ratedTransaction.setStatus(RatedTransactionStatusEnum.BILLED);
                        ratedTransaction.setInvoice(invoice);
                        if (isVirtual) {
                            invoice.getRatedTransactions().add(ratedTransaction);
                        }
                        
                        boolean foundRecordForSameId = false;
                        for (Object[] existingRecord : invoiceSubCats) {
                            if (existingRecord[0].equals(record[0])) {
                                foundRecordForSameId = true;
                                existingRecord[1] = ((BigDecimal) existingRecord[1]).add((BigDecimal) record[1]);
                                existingRecord[2] = ((BigDecimal) existingRecord[2]).add((BigDecimal) record[2]);
                                existingRecord[3] = ((BigDecimal) existingRecord[3]).add((BigDecimal) record[3]);
                                existingRecord[4] = ((BigDecimal) existingRecord[4]).add((BigDecimal) record[4]);
                                break;
                            }
                        }
                        if (!foundRecordForSameId) {
                            invoiceSubCats.add(record);
                        }
                    }
                }
            } else {
                // TODO : use Named queries
                CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
                CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
                Root from = cq.from(RatedTransaction.class);
                Path<Long> invoiceSubCategoryPath = from.get("invoiceSubCategory").get("id");

                Expression<BigDecimal> amountWithoutTax = cb.sum(from.get("amountWithoutTax"));
                Expression<BigDecimal> amountWithTax = cb.sum(from.get("amountWithTax"));
                Expression<BigDecimal> amountTax = cb.sum(from.get("amountTax"));
                Expression<BigDecimal> quantity = cb.sum(from.get("quantity"));

				CriteriaQuery<Object[]> select = cq.multiselect(invoiceSubCategoryPath, amountWithoutTax, amountWithTax,
						amountTax, quantity);
                // Grouping
                cq.groupBy(invoiceSubCategoryPath);
                // Restrictions \
                Predicate pStatus = cb.equal(from.get("status"), RatedTransactionStatusEnum.OPEN);
                if (isInvoiceAdjustment) {
                    pStatus = cb.equal(from.get("status"), RatedTransactionStatusEnum.BILLED);
                }
                Predicate pWallet = cb.equal(from.get("wallet"), wallet);
                Predicate pAmoutWithoutTax = null;
                Predicate pOldTransaction = cb.lessThan(from.get("usageDate"), lastTransactionDate);
                Predicate pdoNotTriggerInvoicing = cb.isFalse(from.get("doNotTriggerInvoicing"));

                Predicate pInvoice = cb.isNull(from.get("invoice"));
                if (isInvoiceAdjustment) {
                    pInvoice = cb.equal(from.get("invoice"), invoice);
                }

                    cq.where(pStatus, pWallet, pOldTransaction, pdoNotTriggerInvoicing, pInvoice);

                invoiceSubCats = getEntityManager().createQuery(cq).getResultList();
            }

            List<InvoiceAgregate> invoiceAgregateSubcatList = new ArrayList<InvoiceAgregate>();

            Map<Long, CategoryInvoiceAgregate> catInvoiceAgregateMap = new HashMap<Long, CategoryInvoiceAgregate>();
            Map<Long, TaxInvoiceAgregate> taxInvoiceAgregateMap = new HashMap<Long, TaxInvoiceAgregate>();

            SubCategoryInvoiceAgregate biggestSubCat = null;
            BigDecimal biggestAmount = new BigDecimal("-100000000");
            
            // No rated transactions for user account, so continue with a next one
            if (invoiceSubCats == null || invoiceSubCats.isEmpty()) {
                continue;
            }
            
            for (Object[] object : invoiceSubCats) {
                log.info("amountWithoutTax=" + object[1] + "amountWithTax =" + object[2] + "amountTax=" + object[3]);
                Long invoiceSubCategoryId = (Long) object[0];
                InvoiceSubCategory invoiceSubCategory = invoiceSubCategoryService.findById(invoiceSubCategoryId);
                List<Tax> taxes = new ArrayList<Tax>();
                for (InvoiceSubcategoryCountry invoicesubcatCountry : invoiceSubCategory.getInvoiceSubcategoryCountries()) {
                    if (invoicesubcatCountry.getTradingCountry().getCountryCode().equalsIgnoreCase(billingAccount.getTradingCountry().getCountryCode())
                            && invoiceSubCategoryService.matchInvoicesubcatCountryExpression(invoicesubcatCountry.getFilterEL(), billingAccount, invoice)) {
    					if(StringUtils.isBlank(invoicesubcatCountry.getTaxCodeEL())){
    						taxes.add(invoicesubcatCountry.getTax());
    					} else {
    						taxes.add(invoiceSubCategoryService.evaluateTaxCodeEL(invoicesubcatCountry.getTaxCodeEL(),userAccount,billingAccount, invoice));
    					}
                    }
                }

                String invSubCatDescTranslated = catMessagesService.getMessageDescription(invoiceSubCategory, languageCode);
                SubCategoryInvoiceAgregate invoiceAgregateSubcat = new SubCategoryInvoiceAgregate();
                invoiceAgregateSubcat.setAuditable(billingAccount.getAuditable());
                invoiceAgregateSubcat.setProvider(billingAccount.getProvider());
                invoiceAgregateSubcat.setInvoice(invoice);
                invoiceAgregateSubcat.setDescription(invSubCatDescTranslated);
                if (!isVirtual) {
                    invoiceAgregateSubcat.setBillingRun(billingAccount.getBillingRun());
                }
                invoiceAgregateSubcat.setWallet(wallet);
                invoiceAgregateSubcat.setAccountingCode(invoiceSubCategory.getAccountingCode());
                fillAgregates(invoiceAgregateSubcat, userAccount);

                invoiceAgregateSubcat.setAmountWithoutTax((BigDecimal) object[1]);
                invoiceAgregateSubcat.setAmountWithTax((BigDecimal) object[2]);
                invoiceAgregateSubcat.setAmountTax((BigDecimal) object[3]);
                invoiceAgregateSubcat.setQuantity((BigDecimal) object[4]);
                invoiceAgregateSubcatList.add(invoiceAgregateSubcat);
                // end aggregate F

                if (!entreprise) {
                    nonEnterprisePriceWithTax = nonEnterprisePriceWithTax.add((BigDecimal) object[2]);
                }

                // start aggregate T
                if (!isExonerated) {
                    for (Tax tax : taxes) {
                        TaxInvoiceAgregate invoiceAgregateTax = null;
                        Long taxId = tax.getId();

                        if (taxInvoiceAgregateMap.containsKey(taxId)) {
                            invoiceAgregateTax = taxInvoiceAgregateMap.get(taxId);
                        } else {
                            invoiceAgregateTax = new TaxInvoiceAgregate();
                            invoiceAgregateTax.setInvoice(invoice);
                            if (!isVirtual){
                                invoiceAgregateTax.setBillingRun(billingAccount.getBillingRun());
                            }
                            invoiceAgregateTax.setTax(tax);
                            invoiceAgregateTax.setTaxPercent(tax.getPercent());
                            invoiceAgregateTax.setAccountingCode(tax.getAccountingCode());

                            taxInvoiceAgregateMap.put(taxId, invoiceAgregateTax);
                        }

                        if (tax.getPercent().compareTo(BigDecimal.ZERO) == 0) {
                            invoiceAgregateTax.addAmountWithoutTax(invoiceAgregateSubcat.getAmountWithoutTax());
                            invoiceAgregateTax.setAmountTax(BigDecimal.ZERO);
                            invoiceAgregateTax.addAmountWithTax(invoiceAgregateSubcat.getAmountWithoutTax());
                        }
                        
                        fillAgregates(invoiceAgregateTax, userAccount);

                        if (invoiceAgregateTax.getId() == null) {
                            if (!isVirtual) {
                                invoiceAgregateService.create(invoiceAgregateTax, currentUser);
                            }
                        }

                        invoiceAgregateSubcat.addSubCategoryTax(tax);
                    }
                }

                invoiceAgregateSubcat.setInvoiceSubCategory(invoiceSubCategory);

                // start aggregate R
                CategoryInvoiceAgregate invoiceAgregateCat = null;
                Long invoiceCategoryId = invoiceSubCategory.getInvoiceCategory().getId();
                if (catInvoiceAgregateMap.containsKey(invoiceCategoryId)) {
                    invoiceAgregateCat = catInvoiceAgregateMap.get(invoiceCategoryId);
                    
                } else {
                    String invCatDescTranslated = catMessagesService.getMessageDescription(invoiceSubCategory.getInvoiceCategory(), languageCode);
                    invoiceAgregateCat = new CategoryInvoiceAgregate();
                    invoiceAgregateCat.setInvoice(invoice);
                    if (!isVirtual){
						invoiceAgregateCat.setBillingRun(billingAccount.getBillingRun());
                    }
                    invoiceAgregateCat.setDescription(invCatDescTranslated);
                    invoiceAgregateCat.setInvoiceCategory(invoiceSubCategory.getInvoiceCategory());
                    catInvoiceAgregateMap.put(invoiceCategoryId, invoiceAgregateCat);
                }
                
                fillAgregates(invoiceAgregateCat, userAccount);
                if (invoiceAgregateCat.getId() == null) {
                    if (!isVirtual){
                        invoiceAgregateService.create(invoiceAgregateCat, currentUser);
                    }
                }

                invoiceAgregateSubcat.setCategoryInvoiceAgregate(invoiceAgregateCat);

                // end agregate R

                // round the amount without Tax
                // compute the largest subcategory

                // first we round the amount without tax

				log.debug("subcat " + invoiceAgregateSubcat.getAccountingCode() + " ht="
						+ invoiceAgregateSubcat.getAmountWithoutTax() + " ->"
                        + invoiceAgregateSubcat.getAmountWithoutTax().setScale(rounding, RoundingMode.HALF_UP));
				invoiceAgregateSubcat.setAmountWithoutTax(invoiceAgregateSubcat.getAmountWithoutTax().setScale(rounding,
						RoundingMode.HALF_UP));
                // add it to taxAggregate and CategoryAggregate
                for (Tax tax : invoiceAgregateSubcat.getSubCategoryTaxes()) {
                    if (tax.getPercent().compareTo(BigDecimal.ZERO) != 0 && !isExonerated) {
                        TaxInvoiceAgregate taxInvoiceAgregate = taxInvoiceAgregateMap.get(tax.getId());
                        taxInvoiceAgregate.addAmountWithoutTax(invoiceAgregateSubcat.getAmountWithoutTax());
						log.info("  tax " + tax.getPercent() + " ht ->"
								+ taxInvoiceAgregate.getAmountWithoutTax());
                    }

                }

				invoiceAgregateSubcat.getCategoryInvoiceAgregate().addAmountWithoutTax(
						invoiceAgregateSubcat.getAmountWithoutTax());
                log.debug("  cat " + invoiceAgregateSubcat.getCategoryInvoiceAgregate().getId() + " ht ->"
                        + invoiceAgregateSubcat.getCategoryInvoiceAgregate().getAmountWithoutTax());
                if (invoiceAgregateSubcat.getAmountWithoutTax().compareTo(biggestAmount) > 0) {
                    biggestAmount = invoiceAgregateSubcat.getAmountWithoutTax();
                    biggestSubCat = invoiceAgregateSubcat;
                }

                if (!isVirtual){
                    invoiceAgregateService.create(invoiceAgregateSubcat, currentUser);
                }
            }

            // compute the tax
            if (!isExonerated) {
                for (Map.Entry<Long, TaxInvoiceAgregate> taxCatMap : taxInvoiceAgregateMap.entrySet()) {
                    TaxInvoiceAgregate taxCat = taxCatMap.getValue();
                    if (taxCat.getTax().getPercent().compareTo(BigDecimal.ZERO) != 0) {
                        // then compute the tax
							taxCat.setAmountTax(taxCat.getAmountWithoutTax().multiply(taxCat.getTaxPercent())
									.divide(new BigDecimal("100")));
                        // then round the tax
                        taxCat.setAmountTax(taxCat.getAmountTax().setScale(rounding, RoundingMode.HALF_UP));

                        // and compute amount with tax
                        /*taxCat.setAmountWithTax(taxCat.getAmountWithoutTax().add(taxCat.getAmountTax())
								.setScale(rounding, RoundingMode.HALF_UP));*/
                        log.debug("  tax2 ht ->" + taxCat.getAmountWithoutTax());
                    }

                }
            }

            for (Map.Entry<Long, CategoryInvoiceAgregate> cat : catInvoiceAgregateMap.entrySet()) {
                CategoryInvoiceAgregate categoryInvoiceAgregate = cat.getValue();
                invoice.addAmountWithoutTax(categoryInvoiceAgregate.getAmountWithoutTax().setScale(rounding, RoundingMode.HALF_UP));
            }
            for (Map.Entry<Long, TaxInvoiceAgregate> tax : taxInvoiceAgregateMap.entrySet()) {
                TaxInvoiceAgregate taxInvoiceAgregate = tax.getValue();
                invoice.addAmountTax(taxInvoiceAgregate.getAmountTax().setScale(rounding, RoundingMode.HALF_UP));
            }
            if (invoice.getAmountWithoutTax() != null) {
                invoice.setAmountWithTax(invoice.getAmountWithoutTax().add(invoice.getAmountTax() == null ? BigDecimal.ZERO : invoice.getAmountTax()));
            }
            BigDecimal balance = BigDecimal.ZERO;
            if (!entreprise && biggestSubCat != null && !isExonerated) {
                BigDecimal delta = nonEnterprisePriceWithTax.subtract(invoice.getAmountWithTax());
                log.debug("delta= " + nonEnterprisePriceWithTax + " - " + invoice.getAmountWithTax() + "=" + delta);
                biggestSubCat.setAmountWithoutTax(biggestSubCat.getAmountWithoutTax().add(delta)
                    .setScale(rounding, RoundingMode.HALF_UP));
                for (Tax tax : biggestSubCat.getSubCategoryTaxes()) {

                    TaxInvoiceAgregate invoiceAgregateT = taxInvoiceAgregateMap.get(tax
                        .getId());
                    log.debug("  tax3 ht ->" + invoiceAgregateT.getAmountWithoutTax());
                    invoiceAgregateT.setAmountWithoutTax(invoiceAgregateT.getAmountWithoutTax().add(delta)
                        .setScale(rounding, RoundingMode.HALF_UP));
                    log.debug("  tax4 ht ->" + invoiceAgregateT.getAmountWithoutTax());

                }
                CategoryInvoiceAgregate invoiceAgregateR = biggestSubCat.getCategoryInvoiceAgregate();
                invoiceAgregateR.setAmountWithoutTax(invoiceAgregateR.getAmountWithoutTax().add(delta)
                    .setScale(rounding, RoundingMode.HALF_UP));

                invoice.setAmountWithoutTax(invoice.getAmountWithoutTax().add(delta).setScale(rounding, RoundingMode.HALF_UP));
                invoice.setAmountWithTax(nonEnterprisePriceWithTax.setScale(rounding, RoundingMode.HALF_UP));

            }
            createInvoiceDiscountAggregates(currentUser, userAccount, invoice, taxInvoiceAgregateMap, isVirtual);

        }
        
        BigDecimal discountAmountWithoutTax = BigDecimal.ZERO;
        BigDecimal discountAmountTax = BigDecimal.ZERO;
        BigDecimal discountAmountWithTax = BigDecimal.ZERO;

        Object[] object = new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO };
        if (!isVirtual) {
            invoiceAgregateService.findTotalAmountsForDiscountAggregates(invoice);
            discountAmountWithoutTax = (BigDecimal) object[0];
            discountAmountTax = (BigDecimal) object[1];
            discountAmountWithTax = (BigDecimal) object[2];
        } else {
            for (InvoiceAgregate invoiceAgregate : invoice.getInvoiceAgregates()) {
                if (invoiceAgregate instanceof SubCategoryInvoiceAgregate && invoiceAgregate.isDiscountAggregate()) {
                    discountAmountWithoutTax.add(invoiceAgregate.getAmountWithoutTax());
                    discountAmountTax.add(invoiceAgregate.getAmountTax());
                    discountAmountWithTax.add(invoiceAgregate.getAmountWithTax());
                }
            }
        }
		
		log.info("discountAmountWithoutTax= {},discountAmountTax={},discountAmountWithTax={}" ,discountAmountWithoutTax,discountAmountTax, discountAmountWithTax);

		invoice.addAmountWithoutTax(discountAmountWithoutTax);
		invoice.addAmountTax(discountAmountTax);
		invoice.addAmountWithTax(discountAmountWithTax);
		BigDecimal netToPay = BigDecimal.ZERO;
		if (entreprise) {
			netToPay = invoice.getAmountWithTax();
		} else {
			BigDecimal balance = BigDecimal.ZERO;
            if (!isVirtual) {
                customerAccountService.customerAccountBalanceDue(null, invoice.getBillingAccount().getCustomerAccount().getCode(), invoice.getDueDate(), invoice.getProvider());

                if (balance == null) {
                    throw new BusinessException("account balance calculation failed");
                }
            }
			netToPay = invoice.getAmountWithTax().add(balance);
		}
		invoice.setNetToPay(netToPay);	
	}

	private void fillAgregates(InvoiceAgregate invoiceAgregate, UserAccount userAccount) {
		invoiceAgregate.setBillingAccount(userAccount.getBillingAccount());
		invoiceAgregate.setUserAccount(userAccount);
		int itemNumber = invoiceAgregate.getItemNumber() != null ? invoiceAgregate.getItemNumber() + 1 : 1;
		invoiceAgregate.setItemNumber(itemNumber);
	}

	public Boolean isBillingAccountBillable(BillingAccount billingAccount,Date lastTransactionDate) {
		long count = 0;
		TypedQuery<Long> q = getEntityManager().createNamedQuery("RatedTransaction.countNotInvoinced", Long.class);
		count = q.setParameter("billingAccount", billingAccount)
				.setParameter("lastTransactionDate", lastTransactionDate).getSingleResult();
		log.debug("isBillingAccountBillable code={},lastTransactionDate={}) : {}",billingAccount.getCode(),lastTransactionDate,count);
		return count > 0 ? true : false;
	}
	
	public Boolean isBillingAccountBillable(BillingAccount billingAccount,String orderNumber) {
		long count = 0;
		TypedQuery<Long> q = getEntityManager().createNamedQuery("RatedTransaction.countListToInvoiceByOrderNumber", Long.class);
		count = q.setParameter("orderNumber", orderNumber).getSingleResult();
		log.debug("isBillingAccountBillable code={},orderNumber={}) : {}",billingAccount.getCode(),orderNumber,count);
		return count > 0 ? true : false;
	}	
	
	/**
	 * This method is only for generating Xml invoice {@link org.meveo.service.billing.impl.XMLInvoiceCreator 
	 * #createXMLInvoice(Long, java.io.File, boolean, boolean) createXMLInvoice}
	 * 
	 * <p>If the provider's displayFreeTransacInInvoice of the current invoice is <tt>false</tt>, RatedTransaction 
	 * with amount=0 don't show up in the XML.</p>
	 * @param wallet
	 * @param invoice
	 * @param invoiceSubCategory
	 * @return
	 */
	public List<RatedTransaction> getRatedTransactionsForXmlInvoice(WalletInstance wallet, Invoice invoice,
			InvoiceSubCategory invoiceSubCategory) {
		long startDate = System.currentTimeMillis();
		QueryBuilder qb = new QueryBuilder(RatedTransaction.class, "c", Arrays.asList("priceplan"),
				invoice.getProvider());
		qb.addCriterionEntity("c.wallet", wallet);
		qb.addCriterionEntity("c.invoiceSubCategory", invoiceSubCategory);
		qb.addCriterionEnum("c.status", RatedTransactionStatusEnum.BILLED);
        qb.addCriterionEntity("c.invoice", invoice);

		if (!invoice.getProvider().isDisplayFreeTransacInInvoice()) {
			qb.addCriterion("c.amountWithoutTax", "<>", BigDecimal.ZERO, false);
		}

		qb.addOrderCriterion("c.usageDate", true);

		@SuppressWarnings("unchecked")
		List<RatedTransaction> ratedTransactions = qb.getQuery(getEntityManager()).getResultList();

		log.debug("getRatedTransactions time: " + (System.currentTimeMillis() - startDate));

		return ratedTransactions;

	}

	public List<RatedTransaction> getRatedTransactions(WalletInstance wallet, Invoice invoice,
			InvoiceSubCategory invoiceSubCategory) {
		long startDate = System.currentTimeMillis();
		QueryBuilder qb = new QueryBuilder(RatedTransaction.class, "c", Arrays.asList("priceplan"),
				invoice.getProvider());
		qb.addCriterionEnum("c.status", RatedTransactionStatusEnum.BILLED);
		qb.addCriterionEntity("c.wallet", wallet);
		qb.addCriterionEntity("c.invoice", invoice);
		qb.addCriterionEntity("c.invoiceSubCategory", invoiceSubCategory);

		qb.addOrderCriterion("c.usageDate", true);

		@SuppressWarnings("unchecked")
		List<RatedTransaction> ratedTransactions = qb.getQuery(getEntityManager()).getResultList();

		log.debug("getRatedTransactions time: " + (System.currentTimeMillis() - startDate));

		return ratedTransactions;

	}

	public int reratedByWalletOperationId(Long id) throws UnrolledbackBusinessException {
		int result = 0;
		List<RatedTransaction> ratedTransactions =  (List<RatedTransaction>) getEntityManager()
				.createNamedQuery("RatedTransaction.listByWalletOperationId", RatedTransaction.class)
				.setParameter("walletOperationId", id).getResultList();
		for(RatedTransaction ratedTransaction:ratedTransactions){
			if(ratedTransaction.getBillingRun()!=null && ratedTransaction.getBillingRun().getStatus()!=BillingRunStatusEnum.CANCELED){
				throw new UnrolledbackBusinessException("A rated transaction "+ratedTransaction.getId()+" forbid rerating of wallet operation "+id);
			}
			ratedTransaction.setStatus(RatedTransactionStatusEnum.RERATED);
			result++;
		}
		return result;
	}


	@SuppressWarnings("unchecked")
	public List<RatedTransaction> getNotBilledRatedTransactions(Long walletOperationId) { 
		QueryBuilder qb = new QueryBuilder("from RatedTransaction c");
		qb.addCriterionEntity("c.walletOperationId", walletOperationId);
		qb.addCriterion("c.status", "!=", RatedTransactionStatusEnum.BILLED, false); 
		try {
			return (List<RatedTransaction>) qb.getQuery(getEntityManager())
					.getResultList();
		} catch (NoResultException e) {
			log.warn("error on get not billed rated transactions ",e);
			return null;
		}

	}



	@SuppressWarnings("unchecked")
	public List<RatedTransaction> getRatedTransactionsByBillingRun(BillingRun BillingRun) { 
		QueryBuilder qb = new QueryBuilder("from RatedTransaction c");
		qb.addCriterionEntity("c.billingRun", BillingRun); 
		try {
			return (List<RatedTransaction>) qb.getQuery(getEntityManager())
					.getResultList();
		} catch (NoResultException e) {
			log.warn("failed to get ratedTransactions ny nillingRun",e);
			return null;
		}

	}
	private void createInvoiceDiscountAggregates(User currentUser,UserAccount userAccount,Invoice invoice,Map<Long, TaxInvoiceAgregate> taxInvoiceAgregateMap, boolean isVirtual){
		try {

			BillingAccount billingAccount=userAccount.getBillingAccount();
			DiscountPlan discountPlan=billingAccount.getDiscountPlan();
			CustomerAccount customerAccount=billingAccount.getCustomerAccount();
			if(discountPlan!=null && discountPlan.isActive()){
				List<DiscountPlanItem> discountPlanItems =discountPlan.getDiscountPlanItems();
				for(DiscountPlanItem discountPlanItem:discountPlanItems){
					if(discountPlanItem.isActive() && matchDiscountPlanItemExpression(discountPlanItem.getExpressionEl(),customerAccount, billingAccount, invoice)){
						if(discountPlanItem.getInvoiceSubCategory()!=null){
							createDiscountAggregate(currentUser,userAccount,userAccount.getWallet(), invoice, discountPlanItem.getInvoiceSubCategory(),discountPlanItem,taxInvoiceAgregateMap, isVirtual);
						}else if(discountPlanItem.getInvoiceCategory()!=null){
							InvoiceCategory invoiceCat=discountPlanItem.getInvoiceCategory();
							for(InvoiceSubCategory invoiceSubCat:invoiceCat.getInvoiceSubCategories()){
								createDiscountAggregate(currentUser,userAccount,userAccount.getWallet(), invoice, invoiceSubCat,discountPlanItem,taxInvoiceAgregateMap, isVirtual);
							}
						}
					}
				}
			}

		} catch (BusinessException e) {
			log.error("Error when trying to create discount aggregates",e);
		}

	}

	private void createDiscountAggregate(User currentUser,UserAccount userAccount,WalletInstance wallet,Invoice invoice,InvoiceSubCategory invoiceSubCat,DiscountPlanItem discountPlanItem,Map<Long, TaxInvoiceAgregate> taxInvoiceAgregateMap, boolean isVirtual) throws BusinessException{
		BillingAccount billingAccount=userAccount.getBillingAccount();
		BigDecimal amount=BigDecimal.ZERO;
        if (!isVirtual) {
            amount = invoiceAgregateService.findTotalAmountByWalletSubCat(wallet, invoiceSubCat, wallet.getProvider(), invoice);
        } else {
            for (InvoiceAgregate invoiceAgregate : invoice.getInvoiceAgregates()) {
                if (invoiceAgregate instanceof SubCategoryInvoiceAgregate && ((SubCategoryInvoiceAgregate) invoiceAgregate).getWallet().equals(wallet)
                        && ((SubCategoryInvoiceAgregate) invoiceAgregate).getInvoiceSubCategory().equals(invoiceSubCat) && !invoiceAgregate.isDiscountAggregate()) {
                    amount.add(invoiceAgregate.getAmountWithoutTax());
                }
            }
        }
		    
		if (amount!=null && !BigDecimal.ZERO.equals(amount)){
			BigDecimal discountAmountWithoutTax=amount.multiply(discountPlanItem.getPercent().divide(HUNDRED)).negate();
			List<Tax> taxes = new ArrayList<Tax>();
			for (InvoiceSubcategoryCountry invoicesubcatCountry : invoiceSubCat
					.getInvoiceSubcategoryCountries()) {
				if ((invoicesubcatCountry.getSellingCountry()==null || 
	            		(billingAccount.getCustomerAccount().getCustomer().getSeller().getTradingCountry()!=null 
	                     && invoicesubcatCountry.getSellingCountry().getCountryCode().equalsIgnoreCase(billingAccount.getCustomerAccount().getCustomer().getSeller().getTradingCountry().getCountryCode())))
	                 && (invoicesubcatCountry.getTradingCountry()==null || invoicesubcatCountry.getTradingCountry().getCountryCode()
						.equalsIgnoreCase(invoice.getBillingAccount().getTradingCountry().getCountryCode()))
						&& invoiceSubCategoryService.matchInvoicesubcatCountryExpression(invoicesubcatCountry.getFilterEL(),billingAccount, invoice)) {
					if(StringUtils.isBlank(invoicesubcatCountry.getTaxCodeEL())){
						taxes.add(invoicesubcatCountry.getTax());
					} else {
						taxes.add(invoiceSubCategoryService.evaluateTaxCodeEL(invoicesubcatCountry.getTaxCodeEL(),userAccount,billingAccount, invoice));
					}
				}
			}
			SubCategoryInvoiceAgregate invoiceAgregateSubcat = new SubCategoryInvoiceAgregate();
			BigDecimal discountAmountTax=BigDecimal.ZERO;
			//TODO do this in the right place (one time by userAccount)			
			boolean isExonerated = billingAccountService.isExonerated(userAccount.getBillingAccount());			
			if(!isExonerated){
				for (Tax tax:taxes) {
					BigDecimal amountTax=discountAmountWithoutTax.multiply(tax.getPercent().divide(HUNDRED));
					discountAmountTax=discountAmountTax.add(amountTax);
					invoiceAgregateSubcat.addSubCategoryTax(tax);
					TaxInvoiceAgregate 	taxInvoiceAgregate= taxInvoiceAgregateMap.get(tax.getId());
					if(taxInvoiceAgregate!=null){
						taxInvoiceAgregate.addAmountTax(amountTax);
						taxInvoiceAgregate.addAmountWithoutTax(discountAmountWithoutTax);
						if (!isVirtual){
						    invoiceAgregateService.update(taxInvoiceAgregate,currentUser);
						}
					}
				}
			}


			BigDecimal discountAmountWithTax=discountAmountWithoutTax.add(discountAmountTax);

			invoiceAgregateSubcat.setInvoice(invoice);
			if (!isVirtual) {
			    invoiceAgregateSubcat.setBillingRun(billingAccount.getBillingRun());
			}
			invoiceAgregateSubcat.setWallet(userAccount.getWallet());
			invoiceAgregateSubcat.setAccountingCode(invoiceSubCat.getAccountingCode());
			fillAgregates(invoiceAgregateSubcat, userAccount);
			invoiceAgregateSubcat.setAmountWithoutTax(discountAmountWithoutTax);
			invoiceAgregateSubcat.setAmountWithTax(discountAmountWithTax);
			invoiceAgregateSubcat.setAmountTax(discountAmountTax);
			invoiceAgregateSubcat.setInvoiceSubCategory(invoiceSubCat);

			invoiceAgregateSubcat.setDiscountAggregate(true);
			invoiceAgregateSubcat.setDiscountPercent(discountPlanItem.getPercent());
			invoiceAgregateSubcat.setDiscountPlanCode(discountPlanItem.getDiscountPlan().getCode());
			invoiceAgregateSubcat.setDiscountPlanItemCode(discountPlanItem.getCode());
            if (!isVirtual) {
                invoiceAgregateService.create(invoiceAgregateSubcat, currentUser);
            }

		}
	}
	


	private boolean matchDiscountPlanItemExpression(String expression,CustomerAccount customerAccount,BillingAccount billingAccount,Invoice invoice) throws BusinessException {
		Boolean result = true;


		if (StringUtils.isBlank(expression) ) {
			return result;
		}
		Map<Object, Object> userMap = new HashMap<Object, Object>();


		if (expression.indexOf("ca") >= 0) {
			userMap.put("ca",customerAccount);
		}
		if (expression.indexOf("ba") >= 0) {
			userMap.put("ba", billingAccount);
		}
		if (expression.indexOf("iv") >= 0) {
			userMap.put("iv", invoice);

		}
		Object res = ValueExpressionWrapper.evaluateExpression(expression, userMap,
				Boolean.class);
		try {
			result = (Boolean) res;
		} catch (Exception e) {
			throw new BusinessException("Expression " + expression
					+ " do not evaluate to boolean but " + res);
		}
		return result;
	}

	public List<RatedTransaction> getListByInvoiceAndSubCategory(Invoice invoice,InvoiceSubCategory invoiceSubCategory) {
		if (invoice == null || invoiceSubCategory == null) {
			return null;
		}
		return (List<RatedTransaction>)getEntityManager().createNamedQuery("RatedTransaction.getListByInvoiceAndSubCategory",RatedTransaction.class)
				.setParameter("invoice", invoice).setParameter("invoiceSubCategory", invoiceSubCategory).getResultList();
	}


    public void createRatedTransaction(Long walletOperationId, User currentUser) throws BusinessException {
        WalletOperation walletOperation = walletOperationService.findById(walletOperationId, currentUser.getProvider());

        createRatedTransaction(walletOperation, false, currentUser);
    }

    /**
     * Create Rated transaction from wallet operation
     * @param walletOperation Wallet operation
     * @param isVirtual Is charge event a virtual operation? If so, no entities should be created/updated/persisted in DB
     * @param currentUser Current user
     * @return Rated transaction
     * @throws BusinessException 
     */
    public RatedTransaction createRatedTransaction(WalletOperation walletOperation, boolean isVirtual, User currentUser) throws BusinessException {

        BigDecimal amountWithTAx = walletOperation.getAmountWithTax();
        BigDecimal amountTax = walletOperation.getAmountTax();
        BigDecimal unitAmountWithTax = walletOperation.getUnitAmountWithTax();
        BigDecimal unitAmountTax = walletOperation.getUnitAmountTax();
        InvoiceSubCategory invoiceSubCategory = walletOperation.getInvoiceSubCategory() != null ? walletOperation.getInvoiceSubCategory() : walletOperation.getChargeInstance()
            .getChargeTemplate().getInvoiceSubCategory();
        /*if (walletOperation.getChargeInstance().getSubscription().getUserAccount().getBillingAccount()
                .getCustomerAccount().getCustomer().getCustomerCategory().getExoneratedFromTaxes()) {
            amountWithTAx = walletOperation.getAmountWithoutTax();
            amountTax = BigDecimal.ZERO;
            unitAmountWithTax = walletOperation.getUnitAmountWithoutTax();
            unitAmountTax = BigDecimal.ZERO;
        }*/
        RatedTransaction ratedTransaction = new RatedTransaction(walletOperation, walletOperation.getOperationDate(), walletOperation.getUnitAmountWithoutTax(),
            unitAmountWithTax, unitAmountTax, walletOperation.getQuantity(), walletOperation.getAmountWithoutTax(), amountWithTAx, amountTax, RatedTransactionStatusEnum.OPEN,
            walletOperation.getProvider(), walletOperation.getWallet(), walletOperation.getWallet().getUserAccount().getBillingAccount(), invoiceSubCategory,
            walletOperation.getParameter1(), walletOperation.getParameter2(), walletOperation.getParameter3(), walletOperation.getOrderNumber(), walletOperation.getRatingUnitDescription(),
            walletOperation.getPriceplan(), walletOperation.getOfferCode(), walletOperation.getEdr());
        ratedTransaction.setDescription(walletOperation.getDescription());
        ratedTransaction.setCode(walletOperation.getCode());
        ratedTransaction.setUnityDescription(walletOperation.getChargeInstance().getChargeTemplate().getInputUnitDescription());

        walletOperation.setStatus(WalletOperationStatusEnum.TREATED);

        if (!isVirtual) {
            create(ratedTransaction, currentUser);
            walletOperation.updateAudit(currentUser);
            walletOperationService.updateNoCheck(walletOperation);
        }
        return ratedTransaction;
    }
	
    public void createRatedTransaction(Long billingAccountId, User currentUser, Date invoicingDate) throws BusinessException {
        BillingAccount billingAccount = billingAccountService.findById(billingAccountId, true);
        List<UserAccount> userAccounts = billingAccount.getUsersAccounts();
        List<WalletOperation> walletOps = new ArrayList<WalletOperation>();
        for (UserAccount ua : userAccounts) {
            walletOps.addAll(walletOperationService.listToInvoiceByUserAccount(invoicingDate, currentUser.getProvider(), ua));
        }
        for (WalletOperation walletOp : walletOps) {
            createRatedTransaction(walletOp, false, currentUser);
        }
    }
	
	@SuppressWarnings("unchecked")
	public List<RatedTransaction> listByInvoice(Invoice invoice) {
		QueryBuilder qb = new QueryBuilder(RatedTransaction.class, "r", null, invoice.getProvider());
		qb.addCriterionEntity("invoice", invoice);

		try {
			return (List<RatedTransaction>) qb.getQuery(getEntityManager()).getResultList();
		} catch (NoResultException e) {
			return null;
		}
	}
	
	public List<RatedTransaction> openRTbySubCat(WalletInstance walletInstance , InvoiceSubCategory invoiceSubCategory ) {
		return openRTbySubCat(walletInstance, invoiceSubCategory, null, null);
	}
	@SuppressWarnings("unchecked")
	public List<RatedTransaction> openRTbySubCat(WalletInstance walletInstance , InvoiceSubCategory invoiceSubCategory ,Date from, Date to) {
		QueryBuilder qb = new QueryBuilder(RatedTransaction.class, "rt", null,walletInstance.getProvider());
		if(invoiceSubCategory != null){
			qb.addCriterionEntity("rt.invoiceSubCategory", invoiceSubCategory);
		}
		qb.addCriterionEntity("rt.wallet", walletInstance);
		qb.addSql("rt.invoice is null");
		qb.addCriterionEnum("rt.status", RatedTransactionStatusEnum.OPEN);
		if(from != null){
			qb.addCriterion("usageDate", ">=", from, false);	
		}
		if(to != null){
			qb.addCriterion("usageDate", "<=", to, false);	
		}
				
		try {
			return (List<RatedTransaction>) qb.getQuery(getEntityManager()).getResultList();
		} catch (NoResultException e) {
			return null;
		}
	}

}