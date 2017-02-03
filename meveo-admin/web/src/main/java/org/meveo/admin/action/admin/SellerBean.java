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
package org.meveo.admin.action.admin;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.action.CustomFieldBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.web.interceptor.ActionMethod;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.InvoiceType;
import org.meveo.model.billing.InvoiceTypeSellerSequence;
import org.meveo.model.billing.Sequence;
import org.meveo.service.admin.impl.SellerService;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.InvoiceTypeService;
import org.omnifaces.cdi.ViewScoped;
@Named
@ViewScoped
public class SellerBean extends CustomFieldBean<Seller> {

	private static final long serialVersionUID = 1L;

	/**
	 * Injected @{link PricePlanMatrix} service. Extends
	 * {@link PersistenceService}.
	 */
	@Inject
	private SellerService sellerService;
	
	 @Inject
	 private InvoiceTypeService invoiceTypeService;
	 
	 private InvoiceTypeSellerSequence selectedInvoiceTypeSellerSequence;
	 private String prefixEl;
	 private Integer sequenceSize= 9;
	 private Long currentInvoiceNb = 0L; 
	 private String invoiceTypeCode;
	 private boolean editSellerSequence=false;

	/**
	 * Constructor. Invokes super constructor and provides class type of this
	 * bean for {@link BaseBean}.
	 */
	public SellerBean() {
		super(Seller.class);
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<Seller> getPersistenceService() {
		return sellerService;
	}

	@Override
	protected String getListViewName() {
		return "sellers";
	}

	@Override
	protected String getDefaultSort() {
		return "code";
	}

	@Override
	protected List<String> getFormFieldsToFetch() {
		return Arrays.asList("provider");
	}

	@Override
	@ActionMethod
	public String saveOrUpdate(boolean killConversation) throws BusinessException {
		return super.saveOrUpdate(killConversation);
		// prefix must be set
//		if (entity.getCurrentInvoiceNb() != null && StringUtils.isBlank(entity.getInvoicePrefix())) {
//			messages.error(new BundleKey("messages", "message.error.seller.invoicePrefix.required"));
//			return null;
//		} else {
//			return super.saveOrUpdate(killConversation);
//		}
	}

    private Sequence getSequence() {
        Sequence sequence = new Sequence();
        sequence.setPrefixEL(getPrefixEl());
        sequence.setSequenceSize(getSequenceSize());
        sequence.setCurrentInvoiceNb(getCurrentInvoiceNb());
        return sequence;
    }

 public void saveOrUpdateSequence() throws BusinessException{ 
		 if(getCurrentInvoiceNb().longValue()< invoiceTypeService.getMaxCurrentInvoiceNumber(getCurrentProvider(), invoiceTypeCode).longValue()) {
			 messages.error(new BundleKey("messages", "invoice.downgrade.cuurrentNb.error.msg"));
			 return;
		 }
		 InvoiceType invoiceType=invoiceTypeService.findByCode(invoiceTypeCode, getCurrentProvider());
		 if(invoiceType!=null){
			 if(!editSellerSequence){
				 if(entity.isContainsInvoiceTypeSequence(invoiceType)){
					 messages.error(new BundleKey("messages","seller.sellerSequence.unique")); 
				 }else{
					 entity.getInvoiceTypeSequence().add(new InvoiceTypeSellerSequence(invoiceType, entity, getSequence()));
					 messages.info(new BundleKey("messages","save.successful"));	
			         super.saveOrUpdate(false); 
				 }
			 }else{ 
				 selectedInvoiceTypeSellerSequence.setSequence(getSequence());
				 messages.info(new BundleKey("messages","update.successful"));
		         super.saveOrUpdate(false);
			 }
		 }
		 resetSequenceField();	 
	 }	

    public void deleteSellerSequence(InvoiceType invoiceType) {

        for (int i = 0; i < entity.getInvoiceTypeSequence().size(); i++) {
            if (entity.getInvoiceTypeSequence().get(i).getInvoiceType().equals(invoiceType)) {
                entity.getInvoiceTypeSequence().remove(i);
                break;
            }
        }
        messages.info(new BundleKey("messages", "delete.successful"));
	 }

    public void getSequenceSelected(InvoiceTypeSellerSequence invoiceTypeSellerSequence) {
        this.selectedInvoiceTypeSellerSequence = invoiceTypeSellerSequence;
        invoiceTypeCode = invoiceTypeSellerSequence.getInvoiceType().getCode();
        prefixEl = invoiceTypeSellerSequence.getSequence().getPrefixEL();
        sequenceSize = invoiceTypeSellerSequence.getSequence().getSequenceSize();
        currentInvoiceNb = invoiceTypeSellerSequence.getSequence().getCurrentInvoiceNb();
        editSellerSequence = true;
    }

    public void resetSequenceField() {
        this.selectedInvoiceTypeSellerSequence = null;
        invoiceTypeCode = null;
        prefixEl = "";
        sequenceSize = 9;
        currentInvoiceNb = 0L;
        editSellerSequence = false;
    }
    
	public String getPrefixEl() {
		return prefixEl;
	}

	public void setPrefixEl(String prefixEl) {
		this.prefixEl = prefixEl;
	}

	public Integer getSequenceSize() {
		return sequenceSize;
	}

	public void setSequenceSize(Integer sequenceSize) {
		this.sequenceSize = sequenceSize;
	}

	public Long getCurrentInvoiceNb() {
		return currentInvoiceNb;
	}

	public void setCurrentInvoiceNb(Long currentInvoiceNb) {
		this.currentInvoiceNb = currentInvoiceNb;
	}

	public String getInvoiceTypeCode() {
		return invoiceTypeCode;
	}

	public void setInvoiceTypeCode(String invoiceTypeCode) {
		this.invoiceTypeCode = invoiceTypeCode;
	}

	public boolean isEditSellerSequence() {
		return editSellerSequence;
	}

	public void setEditSellerSequence(boolean editSellerSequence) {
		this.editSellerSequence = editSellerSequence;
	}



}