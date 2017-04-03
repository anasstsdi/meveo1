package org.meveo.api.catalog;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseApi;
import org.meveo.api.dto.catalog.DiscountPlanItemDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.User;
import org.meveo.model.billing.InvoiceCategory;
import org.meveo.model.billing.InvoiceSubCategory;
import org.meveo.model.catalog.DiscountPlan;
import org.meveo.model.catalog.DiscountPlanItem;
import org.meveo.model.crm.Provider;
import org.meveo.service.catalog.impl.DiscountPlanItemService;
import org.meveo.service.catalog.impl.DiscountPlanService;
import org.meveo.service.catalog.impl.InvoiceCategoryService;
import org.meveo.service.catalog.impl.InvoiceSubCategoryService;

/**
 * 
 * @author Tyshan　Shi(tyshan@manaty.net)
 * @date Aug 1, 2016 9:46:32 PM
 *
 */
@Stateless
public class DiscountPlanItemApi extends BaseApi {
	
	@Inject
	private DiscountPlanService discountPlanService;

    @Inject
    private DiscountPlanItemService discountPlanItemService;
    
    @Inject
    private InvoiceCategoryService invoiceCategoryService;
    
    @Inject
    private InvoiceSubCategoryService invoiceSubCategoryService;

    /**
     * creates a discount plan item
     * 
     * @param postData

     * @throws MeveoApiException
     * @throws BusinessException 
     */
    public void create(DiscountPlanItemDto postData, User currentUser) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("discountPlanItemCode");
        }
        if(StringUtils.isBlank(postData.getDiscountPlanCode())){
        	missingParameters.add("discountPlanCode");
        }
        if(StringUtils.isBlank(postData.getInvoiceCategoryCode())){
        	missingParameters.add("invoiceCategoryCode");
        }
        if(postData.getPercent()==null){
        	missingParameters.add("percent");
        }
        
        handleMissingParameters();
        
        DiscountPlanItem discountPlanItem=discountPlanItemService.findByCode(postData.getCode(), currentUser.getProvider());
        if(discountPlanItem!=null){
            throw new EntityAlreadyExistsException(DiscountPlanItem.class, postData.getCode());
        }
        discountPlanItem=fromDto(postData,null,currentUser);
        discountPlanItemService.create(discountPlanItem, currentUser);
    }

    /**
     * updates the description of an existing discount plan item
     * 
     * @param postData

     * @throws MeveoApiException
     * @throws BusinessException 
     */
    public void update(DiscountPlanItemDto postData, User currentUser) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("discountPlanItemCode");
        }
        handleMissingParameters();
                
        DiscountPlanItem discountPlanItem=discountPlanItemService.findByCode(postData.getCode(), currentUser.getProvider());
        
        if(discountPlanItem==null){
            throw new EntityDoesNotExistsException(DiscountPlanItem.class, postData.getCode());
        }
        discountPlanItem=fromDto(postData,discountPlanItem,currentUser);

        discountPlanItemService.update(discountPlanItem, currentUser);
    }

    /**
     * find a discount plan item by code
     * 
     * @param discountPlanCode
     * @return
     * @throws MeveoApiException
     */
    public DiscountPlanItemDto find(String discountPlanItemCode, Provider provider) throws MeveoApiException {

        if (StringUtils.isBlank(discountPlanItemCode)) {
            missingParameters.add("discountPlanItemCode");
            handleMissingParameters();
        }

        DiscountPlanItem discountPlanItem = discountPlanItemService.findByCode(discountPlanItemCode, provider);
        if (discountPlanItem == null) {
            throw new EntityDoesNotExistsException(DiscountPlanItem.class, discountPlanItemCode);
        }

       return new DiscountPlanItemDto(discountPlanItem);
    }

    /**
     * delete a discount plan item by code
     * 
     * @param discountPlanItemCode
     * @throws MeveoApiException
     */
    public void remove(String discountPlanItemCode, Provider provider) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(discountPlanItemCode)) {
            missingParameters.add("discountPlanItemCode");
            handleMissingParameters();
        }

        DiscountPlanItem discountPlanItem = discountPlanItemService.findByCode(discountPlanItemCode, provider);
        if (discountPlanItem == null) {
            throw new EntityDoesNotExistsException(DiscountPlanItem.class, discountPlanItemCode);
        }
        discountPlanItemService.remove(discountPlanItem);
    }

    /**
     * create if the the discount plan item is not existed, updates if exists
     * 
     * @param postData

     * @throws MeveoApiException
     * @throws BusinessException 
     */
    public void createOrUpdate(DiscountPlanItemDto postData, User currentUser) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("discountPlanItemCode");
            handleMissingParameters();
        }
        if (discountPlanItemService.findByCode(postData.getCode(), currentUser.getProvider()) == null) {
            create(postData, currentUser);
        } else {
            update(postData, currentUser);
        }
    }

    /**
     * retrieves all discount plan item of the user
     * 
     * @return
     * @throws MeveoApiException
     */
    public List<DiscountPlanItemDto> list() throws MeveoApiException {
    	List<DiscountPlanItemDto> discountPlanItemDtos = new ArrayList<DiscountPlanItemDto>();
        List<DiscountPlanItem> discountPlanItems = discountPlanItemService.list();
        if (discountPlanItems != null && !discountPlanItems.isEmpty()) {
            DiscountPlanItemDto dpid=null;
            for (DiscountPlanItem dpi : discountPlanItems) {
                dpid = new DiscountPlanItemDto(dpi);
                discountPlanItemDtos.add(dpid);
            }
        }
        return discountPlanItemDtos;
    }
    public DiscountPlanItem fromDto(DiscountPlanItemDto dto, DiscountPlanItem entity, User currentUser)throws MeveoApiException{
    	DiscountPlanItem discountPlanItem=null;
    	if(entity==null){
    		discountPlanItem=new DiscountPlanItem();
    		discountPlanItem.setCode(dto.getCode());
    	}else{
    		discountPlanItem=entity;
    	}
        
        if(!StringUtils.isBlank(dto.getDiscountPlanCode())){
        	DiscountPlan discountPlan=discountPlanService.findByCode(dto.getDiscountPlanCode(), currentUser.getProvider());
        	if(discountPlan==null){
        		throw new EntityDoesNotExistsException(DiscountPlan.class, dto.getDiscountPlanCode());
        	}
        	if(discountPlanItem.getDiscountPlan()!=null&&discountPlan!=discountPlanItem.getDiscountPlan()){
        		throw new MeveoApiException("Parent discountPlan "+discountPlanItem.getDiscountPlan().getCode()+" of item "+dto.getCode()+" NOT match with DTO discountPlan "+dto.getDiscountPlanCode());
        	}
        	discountPlanItem.setDiscountPlan(discountPlan);
        }
        
        if(!StringUtils.isBlank(dto.getInvoiceCategoryCode())){
        	InvoiceCategory invoiceCategory=invoiceCategoryService.findByCode(dto.getInvoiceCategoryCode(), currentUser.getProvider());
        	if(invoiceCategory==null){
        		throw new EntityDoesNotExistsException(InvoiceCategory.class, dto.getInvoiceCategoryCode());
        	}
        	discountPlanItem.setInvoiceCategory(invoiceCategory);
        }
        
        if(!StringUtils.isBlank(dto.getInvoiceSubCategoryCode())){
    		InvoiceSubCategory invoiceSubCategory=invoiceSubCategoryService.findByCode(dto.getInvoiceSubCategoryCode());
    		discountPlanItem.setInvoiceSubCategory(invoiceSubCategory);
    	}
        if(dto.getPercent()!=null){
        	discountPlanItem.setPercent(dto.getPercent());
        }
        if(dto.getAccountingCode()!=null){
        	discountPlanItem.setAccountingCode(dto.getAccountingCode());
        }
        if(dto.getExpressionEl()!=null){
        	discountPlanItem.setExpressionEl(dto.getExpressionEl());
        }
    	return discountPlanItem;
    }
}
