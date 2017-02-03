package org.meveo.admin.action.catalog;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.web.interceptor.ActionMethod;
import org.meveo.model.catalog.DiscountPlan;
import org.meveo.model.catalog.DiscountPlanItem;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.catalog.impl.DiscountPlanItemService;
import org.meveo.service.catalog.impl.DiscountPlanService;
import org.omnifaces.cdi.ViewScoped;

/**
 * @author Edward P. Legaspi
 **/
@Named
@ViewScoped
public class DiscountPlanBean extends BaseBean<DiscountPlan> {

    private static final long serialVersionUID = -2345373648137067066L;

    @Inject
    private DiscountPlanService discountPlanService;

    @Inject
    private DiscountPlanItemService discountPlanItemService;

    private DiscountPlanItem discountPlanItem = new DiscountPlanItem();

    public DiscountPlanBean() {
        super(DiscountPlan.class);
    }

    @Override
    public DiscountPlan initEntity() {
        discountPlanItem.setAccountingCode(getCurrentProvider().getDiscountAccountingCode());

        return super.initEntity();
    }

    @Override
    protected IPersistenceService<DiscountPlan> getPersistenceService() {
        return discountPlanService;
    }

    @Override
    @ActionMethod
    public String saveOrUpdate(boolean killConversation) throws BusinessException {
    	boolean newEntity = (entity.getId() == null);
    	
        String outcome = super.saveOrUpdate(killConversation);

        if (outcome != null) {
            return newEntity ? getEditViewName() : outcome;
        }
        
        return null;
        //return "/pages/catalog/discountPlans/discountPlanDetail?objectId=" + entity.getId() + "&faces-redirect=true&includeViewParams=true";
    }

    public DiscountPlanItem getDiscountPlanItem() {
        return discountPlanItem;
    }

    public void setDiscountPlanItem(DiscountPlanItem discountPlanItem) {
        this.discountPlanItem = discountPlanItem;
    }

    public void saveOrUpdateDiscountPlan() throws BusinessException {
        if (discountPlanItem.getInvoiceCategory() == null && discountPlanItem.getInvoiceSubCategory() == null) {
            messages.error(new BundleKey("messages", "message.discountPlanItem.error.requiredFields"));
            return;
        }

        discountPlanItem.setDiscountPlan(getEntity());

        if (getEntity().getDiscountPlanItems() == null) {
            getEntity().setDiscountPlanItems(new ArrayList<DiscountPlanItem>());
        }

        if (discountPlanItem.getId() != null) {
            discountPlanItemService.update(discountPlanItem, getCurrentUser());
            messages.info(new BundleKey("messages", "update.successful"));
        }

        if (discountPlanItem.isTransient()) {
            try {
                if (getEntity().getDiscountPlanItems().contains(discountPlanItem)) {
                    messages.error(new BundleKey("messages", "discountPlan.discountPlanItem.unique"));
                } else {
                    discountPlanItemService.create(discountPlanItem, getCurrentUser());
                    getEntity().getDiscountPlanItems().add(discountPlanItem);
                    messages.info(new BundleKey("messages", "save.successful"));
                }
            } catch (BusinessException e) {
                log.error("failed to save or update discount plan", e);
                messages.error(new BundleKey("messages", e.getMessage()));
            }

        }

        discountPlanItem = new DiscountPlanItem();
        discountPlanItem.setAccountingCode(getCurrentProvider().getDiscountAccountingCode());
    }

    public void newDiscountPlanItem() {
        discountPlanItem = new DiscountPlanItem();
    }

    @ActionMethod
    public void deleteDiscountPlan(DiscountPlanItem discountPlanItem) {
        try {
            getEntity().getDiscountPlanItems().remove(discountPlanItem);
            discountPlanItemService.remove(discountPlanItem.getId(), getCurrentUser());
            messages.info(new BundleKey("messages", "delete.successful"));

        } catch (Exception e) {
            messages.error(new BundleKey("messages", "error.delete.unexpected"));
        }
    }

}
