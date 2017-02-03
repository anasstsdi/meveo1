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
package org.meveo.admin.action.billing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import javax.enterprise.inject.Instance;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.international.status.builder.BundleKey;
import org.jboss.solder.servlet.http.RequestParam;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.action.CustomFieldBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.Auditable;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.CategoryInvoiceAgregate;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoiceAgregate;
import org.meveo.model.billing.InvoiceCategory;
import org.meveo.model.billing.InvoiceSubCategory;
import org.meveo.model.billing.InvoiceType;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.RatedTransactionStatusEnum;
import org.meveo.model.billing.SubCategoryInvoiceAgregate;
import org.meveo.model.billing.TaxInvoiceAgregate;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.catalog.ChargeTemplate;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.InvoiceAgregateHandler;
import org.meveo.service.billing.impl.InvoiceAgregateService;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.billing.impl.InvoiceTypeService;
import org.meveo.service.billing.impl.RatedTransactionService;
import org.meveo.service.billing.impl.UserAccountService;
import org.meveo.service.catalog.impl.ChargeTemplateServiceAll;
import org.meveo.service.catalog.impl.InvoiceCategoryService;
import org.meveo.service.catalog.impl.InvoiceSubCategoryService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.LazyDataModel;

/**
 * Standard backing bean for {@link Invoice} (extends {@link BaseBean} that
 * provides almost all common methods to handle entities filtering/sorting in
 * datatable, their create, edit, view, delete operations). It works with Manaty
 * custom JSF components.
 */
@Named
@ViewScoped
public class CreationInvoiceBean extends CustomFieldBean<Invoice> {

	private static final long serialVersionUID = 1L;

	/**
	 * Injected
	 * 
	 * @{link Invoice} service. Extends {@link PersistenceService}.
	 */
	@Inject
	private InvoiceService invoiceService;

	@Inject
	private BillingAccountService billingAccountService;

	@Inject
	private CustomerAccountService customerAccountService;

	@Inject
	private RatedTransactionService ratedTransactionService;

	@Inject
	private InvoiceAgregateService invoiceAgregateService;

	@Inject
	private InvoiceSubCategoryService invoiceSubCategoryService;

	@Inject
	private InvoiceCategoryService invoiceCategoryService;

	@Inject
	private InvoiceTypeService invoiceTypeService;

	@Inject
	private UserAccountService userAccountService;
	
	@Inject
	private ChargeTemplateServiceAll chargeTemplateServiceAll;

	private Invoice invoiceToAdd;
	private Invoice selectedInvoice;
	private InvoiceSubCategory selectedInvoiceSubCategory;
	private BigDecimal quantity;
	private BigDecimal unitAmountWithoutTax;
	private String description;
	private Date usageDate = new Date();
	private RatedTransaction selectedRatedTransaction;
	private List<SelectItem> invoiceCategoriesGUI;
	private ChargeTemplate selectedCharge;

	private boolean includeBalance;

	@Inject
	private  InvoiceAgregateHandler agregateHandler;
	private List<SubCategoryInvoiceAgregate> subCategoryInvoiceAggregates = new ArrayList<SubCategoryInvoiceAgregate>();

	@Inject
	@RequestParam()
	private Instance<String> mode;

	@Inject
	@RequestParam()
	private Instance<Long> linkedInvoiceIdParam;

	private List<CategoryInvoiceAgregate> categoryInvoiceAggregates = new ArrayList<CategoryInvoiceAgregate>();
	private CategoryInvoiceAgregate selectedCategoryInvoiceAgregate;
	private SubCategoryInvoiceAgregate selectedSubCategoryInvoiceAgregateDetaild;
	private SubCategoryInvoiceAgregate selectedSubCategoryInvoiceAgregate;
	private BigDecimal amountWithoutTax;
	private boolean detailled = false;
	private Long rootInvoiceId = null;
	private Invoice rootInvoice;
	private boolean rtxHasImported = false;

	/**
	 * Constructor. Invokes super constructor and provides class type of this
	 * bean for {@link BaseBean}.
	 */
	public CreationInvoiceBean() {
		super(Invoice.class);
	}

	@Override
	public Invoice initEntity() {
		agregateHandler.reset();
		entity = super.initEntity();
		entity.setDueDate(new Date());
		entity.setInvoiceDate(new Date());
		if (entity.isTransient()) {
			if (mode != null && mode.get() != null) {
				setDetailled("detailed".equals(mode.get()));
			}
			if (linkedInvoiceIdParam != null && linkedInvoiceIdParam.get() != null) {
				rootInvoiceId = linkedInvoiceIdParam.get();
				rootInvoice = invoiceService.findById(rootInvoiceId);
				entity.setBillingAccount(rootInvoice.getBillingAccount());
				entity.getLinkedInvoices().add(rootInvoice);
				try {
					entity.setInvoiceType(invoiceTypeService.getDefaultAdjustement(currentUser));
				} catch (BusinessException e) {
					log.error("Cant get DefaultAdjustement Type:", e);
				}
			}
		}
		entity.setAmountWithoutTax(BigDecimal.ZERO);
		entity.setAmountWithTax(BigDecimal.ZERO);
		entity.setAmountTax(BigDecimal.ZERO);
		entity.setNetToPay(BigDecimal.ZERO);
		return entity;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isDetailed() {
		return detailled;
	}

	/**
	 * @param detailled
	 *            the detailled to set
	 */
	public void setDetailled(boolean detailled) {
		this.detailled = detailled;
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<Invoice> getPersistenceService() {
		return invoiceService;
	}

	public void onInvoiceSelect(SelectEvent event) {
		invoiceToAdd = (Invoice) event.getObject();
		if (invoiceToAdd != null && !entity.getLinkedInvoices().contains(invoiceToAdd)) {
			entity.getLinkedInvoices().add(invoiceToAdd);
		}
	}

	public void deleteLinkedInvoice() throws BusinessException {
		entity.getLinkedInvoices().remove(selectedInvoice);
		selectedInvoice = null;

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void deleteAllLinkedInvoice() throws BusinessException {
		entity.setLinkedInvoices(new HashSet());
		selectedInvoice = null;

	}

	public List<RatedTransaction> getRatedTransactions(SubCategoryInvoiceAgregate subCat) {
		if (subCat == null) {
			return null;
		}
		return subCat.getRatedtransactions();
	}

	public void addDetailInvoiceLine() throws BusinessException {
		addDetailedInvoiceLines(selectedInvoiceSubCategory);
	}

	private void addDetailedInvoiceLines(InvoiceSubCategory selectInvoiceSubCat) {
		try {
			if (entity.getBillingAccount() == null) {
				messages.error("BillingAccount is required");
				return;
			}
			if (selectInvoiceSubCat == null) {
				messages.error("Invoice sub category is required.");
				return;
			}
			if (StringUtils.isBlank(description)) {
				messages.error("Description is required.");
				return;
			}
			if (StringUtils.isBlank(quantity)) {
				messages.error("Quantity is required.");
				return;
			}
			if (StringUtils.isBlank(unitAmountWithoutTax)) {
				messages.error("UnitAmountWithoutTax is required.");
				return;
			}
			if (StringUtils.isBlank(selectedCharge)) {
				messages.error("Charge is required.");
				return;
			}
			if (StringUtils.isBlank(usageDate)) {
				messages.error("UsageDate is required.");
				return;
			}

			selectInvoiceSubCat = invoiceSubCategoryService.refreshOrRetrieve(selectInvoiceSubCat);

			RatedTransaction ratedTransaction = new RatedTransaction();
			ratedTransaction.setUsageDate(usageDate);
			ratedTransaction.setUnitAmountWithoutTax(unitAmountWithoutTax);
			ratedTransaction.setQuantity(quantity);
			ratedTransaction.setStatus(RatedTransactionStatusEnum.BILLED);
			ratedTransaction.setWallet(getFreshUA().getWallet());
			ratedTransaction.setBillingAccount(getFreshBA());
			ratedTransaction.setInvoiceSubCategory(selectInvoiceSubCat);
			ratedTransaction.setProvider(getCurrentProvider());
			ratedTransaction.setCode(selectedCharge.getCode());
			ratedTransaction.setDescription(description);
			ratedTransaction.setInvoice(entity);
			ratedTransaction.setInvoiceSubCategory(selectInvoiceSubCat);

			agregateHandler.addRT(ratedTransaction,selectInvoiceSubCat.getDescription(), getFreshUA(), currentUser);
			updateAmountsAndLines(getFreshBA());
		} catch (BusinessException be) {
			messages.error(be.getMessage());
			return;
		} catch (Exception e) {
			messages.error(e.getMessage());
			return;
		}

	}

	/**
	 * Recompute agregates
	 * 
	 * @param agregateHandler
	 * @param billingAccount
	 * @throws BusinessException
	 */
	public void updateAmountsAndLines(BillingAccount billingAccount) throws BusinessException {
		billingAccount = billingAccountService.refreshOrRetrieve(billingAccount);
		subCategoryInvoiceAggregates = new ArrayList<SubCategoryInvoiceAgregate>(agregateHandler.getSubCatInvAgregateMap().values());
		categoryInvoiceAggregates = new ArrayList<CategoryInvoiceAgregate>(agregateHandler.getCatInvAgregateMap().values());
		entity.setAmountWithoutTax(agregateHandler.getInvoiceAmountWithoutTax());
		entity.setAmountTax(agregateHandler.getInvoiceAmountTax());
		entity.setAmountWithTax(agregateHandler.getInvoiceAmountWithTax());

		BigDecimal netToPay = entity.getAmountWithTax();
		if (!getCurrentProvider().isEntreprise() && isIncludeBalance()) {
			BigDecimal balance = customerAccountService.customerAccountBalanceDue(null, billingAccount.getCustomerAccount().getCode(), entity.getDueDate(), entity.getProvider());
			if (balance == null) {
				throw new BusinessException("account balance calculation failed");
			}
			netToPay = entity.getAmountWithTax().add(balance);
		}
		entity.setNetToPay(netToPay);
	}

	/**
	 * Called whene a line is deleted from the dataList detailInvoice
	 * 
	 * @throws BusinessException
	 */
	public void deleteRatedTransactionLine(){
		try{
			agregateHandler.removeRT(selectedRatedTransaction,selectedRatedTransaction.getInvoiceSubCategory().getDescription(), getFreshUA(), currentUser);
			updateAmountsAndLines( getFreshBA());
		}catch(BusinessException be){
			messages.error(be.getMessage());
			return;
		}
	}

	/**
	 * 
	 * @throws BusinessException
	 */
	public void deleteLinkedInvoiceCategoryDetaild(){		
		try{
			for(int i=0 ; i< selectedSubCategoryInvoiceAgregateDetaild.getRatedtransactions().size();i++){
				agregateHandler.removeRT(selectedSubCategoryInvoiceAgregateDetaild.getRatedtransactions().get(i),
						selectedSubCategoryInvoiceAgregateDetaild.getRatedtransactions().get(i).getInvoiceSubCategory().getDescription(),getFreshUA(), currentUser);
				updateAmountsAndLines( getFreshBA());
			}		
		}catch(BusinessException be){
			messages.error(be.getMessage());
			return;
		}
	}
	
	/**
	 * Called whene quantity or unitAmout are changed in the dataList
	 * detailInvoice
	 * 
	 * @param ratedTx
	 * @throws BusinessException
	 */
	public void reComputeAmountWithoutTax(RatedTransaction ratedTx) {
		try{
			agregateHandler.reset();
			for (SubCategoryInvoiceAgregate subcat : subCategoryInvoiceAggregates) {
				for (RatedTransaction rt : subcat.getRatedtransactions()) {
					rt.setAmountWithoutTax(null);
					agregateHandler.addRT(rt,rt.getInvoiceSubCategory().getDescription(), getFreshUA(), currentUser);
					updateAmountsAndLines( ratedTx.getBillingAccount());
				}
			}
		}catch(BusinessException be){
			messages.error(be.getMessage());
			return;
		}
	}
	
	/**
	 * Include original opened ratedTransaction
	 * 
	 * @throws BusinessException
	 */

	public void importOpenedRT(){
		try{
			if(isRtxHasImported()){
				messages.error("Rtx already imported");
				return;
			}
			if (entity.getBillingAccount() == null || entity.getBillingAccount().isTransient()) {
				messages.error("BillingAccount is required.");
				return;
			}
			if (entity.getInvoiceType().getCode().equals(invoiceTypeService.getCommercialCode())) {
				List<RatedTransaction> openedRT = ratedTransactionService.openRTbySubCat(getFreshUA().getWallet(), null);	
				if(openedRT != null && openedRT.isEmpty()){
					messages.error("No opened RatedTransactions");
					return;
				}
				for (RatedTransaction ratedTransaction : openedRT) {
					agregateHandler.addRT(ratedTransaction,ratedTransaction.getInvoiceSubCategory().getDescription(), getFreshUA(), currentUser);
					updateAmountsAndLines(entity.getBillingAccount());
				}
				setRtxHasImported(true);
				
			}
		}catch(BusinessException be){
			messages.error(be.getMessage());
			return;
		}
	}

	/**
	 * 
	 */
	@Override
	public String saveOrUpdate(boolean killConversation){
        try{
			Auditable auditable = new Auditable();
			auditable.setCreator(getCurrentUser());
			auditable.setCreated(new Date());
	
			entity.setBillingAccount(getFreshBA());
			entity.setDetailedInvoice(isDetailed());
	
			entity.setInvoiceNumber(invoiceService.getInvoiceNumber(entity, getCurrentUser()));
			super.saveOrUpdate(false);
			invoiceService.commit();
			entity = invoiceService.refreshOrRetrieve(entity);
			for (Entry<String, TaxInvoiceAgregate> entry : agregateHandler.getTaxInvAgregateMap().entrySet()) {
				TaxInvoiceAgregate taxInvAgr = entry.getValue();
				taxInvAgr.setAuditable(auditable);
				taxInvAgr.setInvoice(entity);
				invoiceAgregateService.create(taxInvAgr, currentUser);
			}
	
			for (Entry<String, CategoryInvoiceAgregate> entry : agregateHandler.getCatInvAgregateMap().entrySet()) {
	
				CategoryInvoiceAgregate catInvAgr = entry.getValue();
				catInvAgr.setAuditable(auditable);
				catInvAgr.setInvoice(entity);
				invoiceAgregateService.create(catInvAgr, currentUser);
			}
	
			for (SubCategoryInvoiceAgregate subcat : subCategoryInvoiceAggregates) {
				subcat.setAuditable(auditable);
				subcat.setInvoice(entity);
				invoiceAgregateService.create(subcat, currentUser);
				for (RatedTransaction rt : subcat.getRatedtransactions()) {
					rt.setInvoice(entity);
					rt.setStatus(RatedTransactionStatusEnum.BILLED);
					if(rt.isTransient()){					
						ratedTransactionService.create(rt, currentUser);
					}else{					
						ratedTransactionService.update(rt, currentUser);
					}
					
				}
			}
	
			for (Invoice invoice : entity.getLinkedInvoices()) {
				invoice.getLinkedInvoices().add(entity);
				invoiceService.update(invoice, currentUser);
			}
	
			try {
				invoiceService.commit();
				invoiceService.generateXmlAndPdfInvoice(entity, getCurrentUser());
			} catch (Exception e) {				
				messages.error("Error generating xml / pdf invoice=" + e.getMessage());
			}
	
			return getListViewName();
        }catch(BusinessException be){
			messages.error(be.getMessage());
			return null;
		}
	}

	/**
	 * Includ a copy from linkedIncoice's RatedTransaction
	 * 
	 * @throws BusinessException
	 */
	public void importFromLinkedInvoices(){
		try{
		if (entity.getBillingAccount() == null || entity.getBillingAccount().isTransient()) {
			messages.error("BillingAccount is required.");
			return;
		}

		if (entity.getLinkedInvoices() != null && entity.getLinkedInvoices().size() <= 0) {
			messages.info(new BundleKey("messages", "message.invoice.addAggregate.linked.null"));
			return;
		}
		for (Invoice invoice : entity.getLinkedInvoices()) {
			invoice = invoiceService.findById(invoice.getId());
			if (isDetailed()) {
				for (RatedTransaction rt : invoice.getRatedTransactions()) {
					RatedTransaction newRT = new RatedTransaction();
					newRT.setUsageDate(rt.getUsageDate());
					newRT.setUnitAmountWithoutTax(rt.getUnitAmountWithoutTax());
					newRT.setQuantity(rt.getQuantity());
					newRT.setStatus(RatedTransactionStatusEnum.BILLED);
					newRT.setWallet(rt.getWallet());
					newRT.setBillingAccount(getFreshBA());
					newRT.setInvoiceSubCategory(rt.getInvoiceSubCategory());
					newRT.setProvider(getCurrentProvider());
					newRT.setCode(rt.getCode());
					newRT.setDescription(rt.getDescription());
					newRT.setInvoice(entity);
					agregateHandler.addRT(newRT,rt.getInvoiceSubCategory().getDescription(), getFreshUA(), currentUser);
					updateAmountsAndLines(getFreshBA());
				}
			} else {
				for (InvoiceAgregate invoiceAgregate : invoice.getInvoiceAgregates()) {
					if (invoiceAgregate instanceof SubCategoryInvoiceAgregate) {
						agregateHandler.addInvoiceSubCategory(((SubCategoryInvoiceAgregate) invoiceAgregate).getInvoiceSubCategory(), getFreshBA(), getFreshUA(),invoiceAgregate.getDescription(),
								invoiceAgregate.getAmountWithoutTax(), currentUser);
						updateAmountsAndLines(getFreshBA());
					}
				}
			}

		}
		}catch(BusinessException be){
			messages.error(be.getMessage());
			return;
		}
		
	}
    
	/**
	 * 
	 * @return
	 */
	public List<SelectItem> getInvoiceCatSubCats() {
		if (invoiceCategoriesGUI != null) {
			return invoiceCategoriesGUI;
		}

		invoiceCategoriesGUI = new ArrayList<>();

		List<InvoiceCategory> invoiceCategories = invoiceCategoryService.list();
		for (InvoiceCategory ic : invoiceCategories) {
			SelectItemGroup selectItemGroup = new SelectItemGroup(ic.getCode());
			List<SelectItem> subCats = new ArrayList<>();
			for (InvoiceSubCategory invoiceSubCategory : ic.getInvoiceSubCategories()) {
				subCats.add(new SelectItem(invoiceSubCategory, invoiceSubCategory.getCode()));
			}
			selectItemGroup.setSelectItems(subCats.toArray(new SelectItem[subCats.size()]));
			invoiceCategoriesGUI.add(selectItemGroup);
		}

		return invoiceCategoriesGUI;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<ChargeTemplate> getCharges() {			
		return chargeTemplateServiceAll.findByInvoiceSubCategory(selectedInvoiceSubCategory);
	}



	public void setIncludeBalance(boolean includeBalance) {
		this.includeBalance = includeBalance;
		try {
			updateAmountsAndLines(getFreshBA());
		} catch (BusinessException be) {
			messages.error(be.getMessage());
			return;
		}
	}

	/**
	 * @return the subCategoryInvoiceAggregates
	 */
	public List<SubCategoryInvoiceAgregate> getSubCategoryInvoiceAggregates() {
		return subCategoryInvoiceAggregates;
	}

	/**
	 * @param subCategoryInvoiceAggregates
	 *            the subCategoryInvoiceAggregates to set
	 */
	public void setSubCategoryInvoiceAggregates(List<SubCategoryInvoiceAgregate> subCategoryInvoiceAggregates) {
		this.subCategoryInvoiceAggregates = subCategoryInvoiceAggregates;
	}

	private BillingAccount getFreshBA() throws BusinessException {
		// TODO singletone this
		if (entity.getBillingAccount() == null || entity.getBillingAccount().isTransient()) {
			throw new BusinessException("BillingAccount is required.");
		}
		return billingAccountService.refreshOrRetrieve(entity.getBillingAccount());
	}

	private UserAccount getFreshUA() throws BusinessException {
		if (getFreshBA().getUsersAccounts() == null || getFreshBA().getUsersAccounts().isEmpty()) {
			throw new BusinessException("BillingAccount with code=" + getFreshBA().getCode() + " has no userAccount.");
		}
		return userAccountService.refreshOrRetrieve(getFreshBA().getUsersAccounts().get(0));
	}

	public List<CategoryInvoiceAgregate> getCategoryInvoiceAggregates() {
		return categoryInvoiceAggregates;
	}

	/**
	 * delete a cat invoice agregate
	 * 
	 * @throws BusinessException
	 */
	public void deleteLinkedInvoiceCategory() throws BusinessException {
		for (SubCategoryInvoiceAgregate subCat : selectedCategoryInvoiceAgregate.getSubCategoryInvoiceAgregates()) {
			agregateHandler.removeInvoiceSubCategory(subCat.getInvoiceSubCategory(), getFreshBA(), getFreshUA(), subCat.getDescription(),subCat.getAmountWithoutTax(), currentUser);
			updateAmountsAndLines(getFreshBA());
		}
		
	}

	/**
	 * delete a sub cat invoice agregate
	 * 
	 * @throws BusinessException
	 */
	public void deleteLinkedInvoiceSubCategory() throws BusinessException {
		agregateHandler.removeInvoiceSubCategory(selectedSubCategoryInvoiceAgregate.getInvoiceSubCategory(), getFreshBA(), getFreshUA(),
				selectedSubCategoryInvoiceAgregate.getDescription(),selectedSubCategoryInvoiceAgregate.getAmountWithoutTax(), currentUser);
		updateAmountsAndLines(getFreshBA());

	}

	public CategoryInvoiceAgregate getSelectedCategoryInvoiceAgregate() {
		return selectedCategoryInvoiceAgregate;
	}

	public void setSelectedCategoryInvoiceAgregate(CategoryInvoiceAgregate selectedCategoryInvoiceAgregate) {
		this.selectedCategoryInvoiceAgregate = selectedCategoryInvoiceAgregate;
	}

	public List<SubCategoryInvoiceAgregate> getSubCategoryInvoiceAggregates(CategoryInvoiceAgregate cat) {
		if (cat == null)
			return null;
		List<SubCategoryInvoiceAgregate> result = new ArrayList<>();
		if (cat.getSubCategoryInvoiceAgregates() == null)
			return result;

		for (SubCategoryInvoiceAgregate subCat : cat.getSubCategoryInvoiceAgregates()) {
			result.add(subCat);
		}

		return result;
	}

	public SubCategoryInvoiceAgregate getSelectedSubCategoryInvoiceAgregate() {
		return selectedSubCategoryInvoiceAgregate;
	}

	public void setSelectedSubCategoryInvoiceAgregate(SubCategoryInvoiceAgregate selectedSubCategoryInvoiceAgregate) {
		this.selectedSubCategoryInvoiceAgregate = selectedSubCategoryInvoiceAgregate;
	}

	public void addAggregatedLine() throws BusinessException {

		if (entity.getBillingAccount() == null || entity.getBillingAccount().isTransient()) {
			messages.error("BillingAccount is required.");
			return;
		}
		
		if (StringUtils.isBlank(description)) {
			messages.error("Description is required.");
			return;
		}

		if (amountWithoutTax == null || selectedInvoiceSubCategory == null) {
			messages.error("AmountWithoutTax and InvoiceSubCategory is required.");
			return;
		}

		selectedInvoiceSubCategory = invoiceSubCategoryService.refreshOrRetrieve(selectedInvoiceSubCategory);		

		agregateHandler.addInvoiceSubCategory(selectedInvoiceSubCategory, getFreshBA(), getFreshUA(),description, amountWithoutTax, currentUser);
		updateAmountsAndLines(getFreshBA());
	}

	/**
	 * Called whene quantity or unitAmout are changed in the dataList
	 * detailInvoice
	 * 
	 * @param ratedTx
	 * @throws BusinessException
	 */
	public void reComputeAmountWithoutTax(SubCategoryInvoiceAgregate invSubCat) throws BusinessException {
		agregateHandler.reset();
		for (CategoryInvoiceAgregate cat : categoryInvoiceAggregates) {
			for (SubCategoryInvoiceAgregate subCate : cat.getSubCategoryInvoiceAgregates()) {				
				InvoiceSubCategory tmp = subCate.getInvoiceSubCategory();
				agregateHandler.addInvoiceSubCategory(tmp, getFreshBA(), getFreshUA(), subCate.getDescription(), subCate.getAmountWithoutTax(), currentUser);
				updateAmountsAndLines(getFreshBA());
			}
		}
		
	}
	
	/**
	 * 
	 * @return
	 * @throws BusinessException
	 */
	public LazyDataModel<Invoice> getInvoicesByTypeAndBA() throws BusinessException {
		if (getEntity().getBillingAccount() != null && !entity.getBillingAccount().isTransient()) {
			BillingAccount ba = billingAccountService.refreshOrRetrieve(entity.getBillingAccount());
			filters.put("billingAccount", ba);
		}
		if (entity.getInvoiceType() != null) {
			InvoiceType selInvoiceType = invoiceTypeService.refreshOrRetrieve(entity.getInvoiceType());
			List<InvoiceType> invoiceTypes = selInvoiceType.getAppliesTo();
			if (invoiceTypes != null && invoiceTypes.size() > 0) {
				StringBuilder invoiceTypeIds = new StringBuilder();
				for (InvoiceType invoiceType : invoiceTypes) {
					invoiceTypeIds.append(invoiceType.getId() + ",");
				}
				invoiceTypeIds.deleteCharAt(invoiceTypeIds.length() - 1);
				filters.put("inList-invoiceType.id", invoiceTypeIds);
			}
		}

		return getLazyDataModel();
	}
	
	@Override
	public String getBackView() {
		// TODO use outcome and params
		if (rootInvoiceId == null) {
			return super.getBackView();
		}
		return "/pages/billing/invoices/invoiceDetail.xhtml?objectId=" + rootInvoiceId + "&cid=" + conversation.getId() + "&edit=true&faces-redirect=true";
	}

	@Override
	public String getBackViewSave() {
		return getBackView();
	}
	
	public boolean isCanAddLinkedInvoice(){
		return true;//entity.getBillingAccount() != null && entity.getInvoiceType() != null;
	}
	
	
	/*###################################################################################################
	  #  Setters and Getters                                                                            #
	  ###################################################################################################*/

	public void handleSelectedInvoiceCatOrSubCat() {		    
		if (selectedInvoiceSubCategory != null) {
			description = selectedInvoiceSubCategory.getDescriptionOrCode();
		}
	}
	
	public void handleSelectedCharge() {		    
		if (selectedCharge != null) {
			description = selectedCharge.getDescriptionOrCode();
		}
	}
	
	public String getPageMode() {
		if (mode != null && !StringUtils.isBlank(mode.get())) {
			return mode.get();
		}

		return "agregated";
	}

	/**
	 * @return the rootInvoiceId
	 */
	public Long getRootInvoiceId() {
		return rootInvoiceId;
	}

	/**
	 * @param rootInvoiceId
	 *            the rootInvoiceId to set
	 */
	public void setRootInvoiceId(Long rootInvoiceId) {
		this.rootInvoiceId = rootInvoiceId;
	}


	/**
	 * @return the selectedSubCategoryInvoiceAgregateDetaild
	 */
	public SubCategoryInvoiceAgregate getSelectedSubCategoryInvoiceAgregateDetaild() {
		return selectedSubCategoryInvoiceAgregateDetaild;
	}

	/**
	 * @param selectedSubCategoryInvoiceAgregateDetaild the selectedSubCategoryInvoiceAgregateDetaild to set
	 */
	public void setSelectedSubCategoryInvoiceAgregateDetaild(SubCategoryInvoiceAgregate selectedSubCategoryInvoiceAgregateDetaild) {
		this.selectedSubCategoryInvoiceAgregateDetaild = selectedSubCategoryInvoiceAgregateDetaild;
	}

	/**
	 * @return the usageDate
	 */
	public Date getUsageDate() {
		return usageDate;
	}

	/**
	 * @param usageDate the usageDate to set
	 */
	public void setUsageDate(Date usageDate) {
		this.usageDate = usageDate;
	}

	/**
	 * @return the selectedCharge
	 */
	public ChargeTemplate getSelectedCharge() {
		return selectedCharge;
	}

	/**
	 * @param selectedCharge the selectedCharge to set
	 */
	public void setSelectedCharge(ChargeTemplate selectedCharge) {
		this.selectedCharge = selectedCharge;
	}

	/**
	 * @return the amountWithoutTax
	 */
	public BigDecimal getAmountWithoutTax() {
		return amountWithoutTax;
	}

	/**
	 * @param amountWithoutTax
	 *            the amountWithoutTax to set
	 */
	public void setAmountWithoutTax(BigDecimal amountWithoutTax) {
		this.amountWithoutTax = amountWithoutTax;
	}
	public Invoice getInvoiceToAdd() {
		return invoiceToAdd;
	}

	public void setInvoiceToAdd(Invoice invoiceToAdd) {		
		if (invoiceToAdd != null && !entity.getLinkedInvoices().contains(invoiceToAdd)) {
			entity.getLinkedInvoices().add(invoiceToAdd);
		}
		this.invoiceToAdd = invoiceToAdd;
	}

	public Invoice getSelectedInvoice() {
		return selectedInvoice;
	}

	public void setSelectedInvoice(Invoice selectedInvoice) {
		this.selectedInvoice = selectedInvoice;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getUnitAmountWithoutTax() {
		return unitAmountWithoutTax;
	}

	public void setUnitAmountWithoutTax(BigDecimal unitAmountWithoutTax) {
		this.unitAmountWithoutTax = unitAmountWithoutTax;
	}

	public List<SelectItem> getInvoiceCategoriesGUI() {
		return invoiceCategoriesGUI;
	}

	public void setInvoiceCategoriesGUI(List<SelectItem> invoiceCategoriesGUI) {
		this.invoiceCategoriesGUI = invoiceCategoriesGUI;
	}

	public InvoiceSubCategory getSelectedInvoiceSubCategory() {
		return selectedInvoiceSubCategory;
	}

	public void setSelectedInvoiceSubCategory(InvoiceSubCategory selectedInvoiceSubCategory) {	
		this.selectedInvoiceSubCategory = selectedInvoiceSubCategory;
	}

	public RatedTransaction getSelectedRatedTransaction() {
		return selectedRatedTransaction;
	}

	public void setSelectedRatedTransaction(RatedTransaction selectedRatedTransaction) {
		this.selectedRatedTransaction = selectedRatedTransaction;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isIncludeBalance() {
		return includeBalance;
	}

	/**
	 * @return the rtxHasImported
	 */
	public boolean isRtxHasImported() {
		return rtxHasImported;
	}

	/**
	 * @param rtxHasImported the rtxHasImported to set
	 */
	public void setRtxHasImported(boolean rtxHasImported) {
		this.rtxHasImported = rtxHasImported;
	}	
	
	
}