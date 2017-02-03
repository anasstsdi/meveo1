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
package org.meveo.admin.action.catalog;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.international.status.builder.BundleKey;
import org.jboss.solder.servlet.http.RequestParam;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.action.CustomFieldBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.web.interceptor.ActionMethod;
import org.meveo.model.catalog.ChargeTemplate;
import org.meveo.model.catalog.OneShotChargeTemplate;
import org.meveo.model.catalog.PricePlanMatrix;
import org.meveo.model.catalog.ProductChargeTemplate;
import org.meveo.model.catalog.RecurringChargeTemplate;
import org.meveo.model.catalog.UsageChargeTemplate;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.catalog.impl.OneShotChargeTemplateService;
import org.meveo.service.catalog.impl.PricePlanMatrixService;
import org.meveo.service.catalog.impl.ProductChargeTemplateService;
import org.meveo.service.catalog.impl.RecurringChargeTemplateService;
import org.meveo.service.catalog.impl.UsageChargeTemplateService;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.Visibility;

/**
 * Standard backing bean for {@link PricePlanMatrix} (extends {@link BaseBean}
 * that provides almost all common methods to handle entities filtering/sorting
 * in datatable, their create, edit, view, delete operations). It works with
 * Manaty custom JSF components.
 */
@Named
@ViewScoped
public class PricePlanMatrixBean extends CustomFieldBean<PricePlanMatrix> {

	private static final long serialVersionUID = -7046887530976683885L;

	/**
	 * Injected @{link PricePlanMatrix} service. Extends
	 * {@link PersistenceService}.
	 */
	@Inject
	private PricePlanMatrixService pricePlanMatrixService;
	
	@Inject
	private RecurringChargeTemplateService recurringChargeTemplateService;

	@Inject
	private UsageChargeTemplateService usageChargeTemplateService;

	@Inject
	private OneShotChargeTemplateService oneShotChargeTemplateService;
	
	@Inject
	private ProductChargeTemplateService productChargeTemplateService;
	
	@Inject
	@RequestParam
	private Instance<Long> chargeId; 
	
	private String backPage;
	
	private long chargeTemplateId;
	
	private List<Boolean> columnVisibilitylist;

	/**
	 * Constructor. Invokes super constructor and provides class type of this
	 * bean for {@link BaseBean}.
	 */
	public PricePlanMatrixBean() {
		super(PricePlanMatrix.class);
	}

	/**
	 * Factory method for entity to edit. If objectId param set load that entity
	 * from database, otherwise create new.
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */

	public PricePlanMatrix initEntity() {
		PricePlanMatrix obj = super.initEntity();
		if (obj.isTransient()) {
			obj.setMinSubscriptionAgeInMonth(0L);
			obj.setMaxSubscriptionAgeInMonth(9999L);
		}
		if (chargeId.get() != null) {
			RecurringChargeTemplate recurring = recurringChargeTemplateService
					.findById(chargeId.get());
			if (recurring != null) {
				if (getObjectId() == null) {
					obj.setCode(getPricePlanCode(recurring));
					obj.setEventCode(recurring.getCode());
					obj.setDescription(recurring.getDescription());
					obj.setSequence(getNextSequence(recurring));
				}
				backPage = "recurringChargeTemplateDetail";
			} else {
				OneShotChargeTemplate oneShot = oneShotChargeTemplateService
						.findById(chargeId.get());
				if (oneShot != null) {
					if (getObjectId() == null) {
						obj.setCode(getPricePlanCode(oneShot));
						obj.setEventCode(oneShot.getCode());
						obj.setDescription(oneShot.getDescription());
						obj.setSequence(getNextSequence(oneShot));
					}
					backPage = "oneShotChargeTemplateDetail";
				} else {
					UsageChargeTemplate usageCharge = usageChargeTemplateService
							.findById(chargeId.get());
					if (usageCharge != null) {
						if (getObjectId() == null) {
							obj.setCode(getPricePlanCode(usageCharge));
							obj.setEventCode(usageCharge.getCode());
							obj.setDescription(usageCharge.getDescription());
							obj.setSequence(getNextSequence(usageCharge));
						}
						backPage = "usageChargeTemplateDetail";
					} else {
						ProductChargeTemplate productCharge = productChargeTemplateService
								.findById(chargeId.get());
						if (getObjectId() == null) {
							obj.setCode(getPricePlanCode(productCharge));
							obj.setEventCode(productCharge.getCode());
							obj.setDescription(productCharge.getDescription());
							obj.setSequence(getNextSequence(productCharge));
						}
						backPage = "productChargeTemplateDetail";
					}
				}
			}
			chargeTemplateId = chargeId.get();
		}
		return obj;
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<PricePlanMatrix> getPersistenceService() {
		return pricePlanMatrixService;
	}

	@Override
	protected String getListViewName() {
		return "pricePlanMatrixes";
	}

	
	public void onChargeSelect(SelectEvent event) {
		if (event.getObject() instanceof ChargeTemplate) {
			ChargeTemplate chargeTemplate = (ChargeTemplate) event.getObject();
			if (chargeTemplate != null) {
				entity.setEventCode(chargeTemplate.getCode());
				entity.setCode(getPricePlanCode(chargeTemplate));
				entity.setDescription(chargeTemplate.getDescription());
				entity.setSequence(getNextSequence(chargeTemplate));
			}
		}
	}

	@Override
	protected String getDefaultSort() {
		return "code";
	}

	@Override
	protected List<String> getListFieldsToFetch() {
		return Arrays.asList("provider");
	}

	@Override
	protected List<String> getFormFieldsToFetch() {
		return Arrays.asList("provider");
	}

	//show advanced button in search panel
	private boolean advanced=false;
	public boolean getAdvanced(){
		return this.advanced;
	}

	protected void advancedAction(ActionEvent actionEvent){
		this.advanced = !advanced;
		if (filters != null) {
			Iterator<String> iter = filters.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				if (!"eventCode".equals(key) && !"seller".equals(key)
						&& !"code".equals(key) && !"offerTemplate".equals(key)) {
					iter.remove();
				}
			}
		}
	}
	
	
	public LazyDataModel<PricePlanMatrix> getPricePlanMatrixList(
			ChargeTemplate chargeTemplate) { 
			filters.put("eventCode", chargeTemplate.getCode());
			return getLazyDataModel();
		}

	

	public String getPricePlanCode(ChargeTemplate chargetemplate) {
		String pricePlanCode=null;
		try{
		if (chargetemplate != null) { 
		pricePlanCode="PP_"+chargetemplate.getCode()+"_"+getNextSequence(chargetemplate);
		}} catch (Exception e) {
			log.warn("error while getting pricePlan code", e);
			return null;
		}
		return pricePlanCode;
	}

	public Long getNextSequence(ChargeTemplate chargetemplate) {
		long result = 0;
		try {
		if (chargetemplate != null) {
			result = pricePlanMatrixService.getLastPricePlanByCharge(chargetemplate.getCode(), chargetemplate.getProvider()) + 1;
		}
		} catch (Exception e) {
			log.warn("error while getting next sequence", e);
			return null;
		}
		return result;
	}
	
	@Override
	@ActionMethod
	public String saveOrUpdate(boolean killConversation) throws BusinessException {
		if (chargeTemplateId != 0) {
			super.saveOrUpdate(killConversation);
			return getBackCharge();
		} else {
			return super.saveOrUpdate(killConversation);
		}
	}

	@Override
	public String back() {
		if (chargeTemplateId != 0) {
			return getBackCharge();
		} else {
			return super.back();
		}
	}

	public String getBackCharge() {
		String chargeName = null;
		if (backPage.equals("recurringChargeTemplateDetail")) {
			chargeName = "recurringChargeTemplates";
		} else if (backPage.equals("oneShotChargeTemplateDetail")) {
			chargeName = "oneShotChargeTemplates";
		} else if (backPage.equals("productChargeTemplateDetail")) {
			chargeName = "productChargeTemplates";
		} else {
			chargeName = "usageChargeTemplates";
		}
		
		return "/pages/catalog/" + chargeName + "/" + backPage + ".xhtml?objectId=" + chargeTemplateId + "&edit=true&tab=1&faces-redirect=true";
	}
 
	 @ActionMethod
	 public void duplicate(){
			if (entity != null && entity.getId() != null) {
				try {
					pricePlanMatrixService.duplicate(entity, getCurrentUser());
					messages.info(new BundleKey("messages", "save.successful"));
	            } catch (BusinessException e) {
	                log.error("Error encountered persisting price plan matrix entity: #{0}:#{1}", entity.getCode(), e);
	                messages.error(new BundleKey("messages", "save.unsuccessful"));
	            }
			}
		}
	 

	public long getChargeTemplateId() {
		return chargeTemplateId;
	}
  
	/**
	 * initialize the list of table columns to be visible
	 */
	@PostConstruct
	 public void init() {
		columnVisibilitylist = Arrays.asList(true, true, true, true, true, true, false, 
	     		false, false, false, false, false, false, false, 
	     		false, false, false, false, false, false, false, false);
	 }
	 public List<Boolean> getColumnVisibilitylist() {
	     return columnVisibilitylist;
	 }
	 public void onToggle(ToggleEvent e) {
	 	columnVisibilitylist.set((Integer) e.getData(), e.getVisibility() == Visibility.VISIBLE);
	 }
	
}

