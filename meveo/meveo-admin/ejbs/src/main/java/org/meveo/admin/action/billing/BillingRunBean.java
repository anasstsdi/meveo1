/*
 * (C) Copyright 2009-2013 Manaty SARL (http://manaty.net/) and contributors.
 *
 * Licensed under the GNU Public Licence, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/gpl-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.meveo.admin.action.billing;

import java.util.Date;
import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Scope;

import org.jboss.seam.international.status.Messages;
import org.jboss.seam.international.status.builder.BundleKey;
import org.jboss.solder.servlet.http.RequestParam;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.action.admin.CurrentProvider;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.PaginationDataModel;
import org.meveo.admin.utils.ListItemsSelector;
import org.meveo.commons.utils.ParamBean;
import org.meveo.model.billing.BillingProcessTypesEnum;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.BillingRunStatusEnum;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.PostInvoicingReportsDTO;
import org.meveo.model.billing.PreInvoicingReportsDTO;
import org.meveo.model.crm.Provider;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.BillingRunService;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.crm.impl.ProviderService;

@Named
@ViewScoped
public class BillingRunBean extends BaseBean<BillingRun> {

    private static final long serialVersionUID = 1L;

    /**
     * Injected
     * 
     * @{link Invoice} service. Extends {@link PersistenceService}.
     */
    @Inject
    private BillingRunService billingRunService;

    @Inject
    private InvoiceService invoiceService;

    @Inject
    private ProviderService providerService;

    @Inject
    @RequestParam
    private Instance<Boolean> preReport;

    @Inject
    @RequestParam
    private Instance<Boolean> postReport;

    private ListItemsSelector<Invoice> itemSelector;

    @Inject
    private Messages messages;

    private DataModel invoicesModel;

    @Inject
    @CurrentProvider
    private Provider currentProvider;

    /**
     * Constructor. Invokes super constructor and provides class type of this bean for {@link BaseBean}.
     */
    public BillingRunBean() {
        super(BillingRun.class);
    }

    /**
     * Factory method for entity to edit. If objectId param set load that entity from database, otherwise create new.
     * 
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    @Produces
    @Named("billingRun")
    public BillingRun init() {
        try {
            initEntity();
            if (entity.getId() == null) {
                entity.setProcessType(BillingProcessTypesEnum.MANUAL);
            }
            if (entity != null && entity.getId() != null && preReport.get() != null && preReport.get()) {
                PreInvoicingReportsDTO preInvoicingReportsDTO = billingRunService.generatePreInvoicingReports(entity);
                entity.setPreInvoicingReports(preInvoicingReportsDTO);
            } else if (entity != null && entity.getId() != null && postReport.get() != null && postReport.get()) {
                PostInvoicingReportsDTO postInvoicingReportsDTO = billingRunService.generatePostInvoicingReports(entity);
                entity.setPostInvoicingReports(postInvoicingReportsDTO);
            }
            invoicesModel = new ListDataModel(entity.getInvoices());

        } catch (BusinessException e) {
            log.error("Failed to initialize an object", e);
        }

        return entity;
    }

    @Produces
    @Named("billingRunInvoices")
    @ConversationScoped
    public List<Invoice> getBillingRunInvoices() {
        if (entity == null) {
            return null;
        }
        return entity.getInvoices();
    }

    /**
     * Factory method, that is invoked if data model is empty. Invokes BaseBean.list() method that handles all data model loading. Overriding is needed only to put factory name on
     * it.
     * @return 
     * 
     * @see org.meveo.admin.action.BaseBean#list()
     */
    @Produces
    @Named("billingRuns")
    @ConversationScoped
    public PaginationDataModel<BillingRun> list() {
        entities = null;
        return super.list();
    }

    /**
     * @see org.meveo.admin.action.BaseBean#getPersistenceService()
     */
    @Override
    protected IPersistenceService<BillingRun> getPersistenceService() {
        return billingRunService;
    }

    public String lunchRecurringInvoicing() {
        log.info("lunchInvoicing billingRun BillingCycle=#1", entity.getBillingCycle().getCode());
        try {
            ParamBean param = ParamBean.getInstance("meveo-admin.properties");
            String allowManyInvoicing = param.getProperty("billingRun.allowManyInvoicing", "true");
            boolean isAllowed = Boolean.parseBoolean(allowManyInvoicing);
            log.info("lunchInvoicing allowManyInvoicing=#", isAllowed);
            if (billingRunService.isActiveBillingRunsExist(currentProvider) && !isAllowed) {
                messages.error(new BundleKey("messages", "error.invoicing.alreadyLunched"));
                return null;
            }

            entity.setStatus(BillingRunStatusEnum.NEW);
            entity.setProcessDate(new Date());
            billingRunService.create(entity);
            entity.setProvider(entity.getBillingCycle().getProvider());

            return "/pages/billing/invoicing/billingRuns.seam?edit=false";

        } catch (Exception e) {
            e.printStackTrace();
            messages.error(e.getMessage());
        }
        return null;
    }

    public String confirmInvoicing() {
        try {
            // statusMessages.add("facturation confirmee avec succes");
            entity.setStatus(BillingRunStatusEnum.ON_GOING);
            billingRunService.update(entity);
            return "/pages/billing/invoicing/billingRuns.seam?edit=false";

        } catch (Exception e) {
            e.printStackTrace();
            messages.error(e.getMessage());
        }
        return null;
    }

    public String validateInvoicing() {
        try {
            entity.setStatus(BillingRunStatusEnum.VALIDATED);
            billingRunService.update(entity);
            return "/pages/billing/invoicing/billingRuns.seam?edit=false";
        } catch (Exception e) {
            e.printStackTrace();
            messages.error(e.getMessage());
        }
        return null;
    }

    public String cancelInvoicing() {
        try {
            entity.setStatus(BillingRunStatusEnum.CANCELED);
            billingRunService.update(entity);
            return "/pages/billing/invoicing/billingRuns.seam?edit=false";
        } catch (Exception e) {
            e.printStackTrace();
            messages.error(e.getMessage());
        }
        return null;
    }

    public String cancelConfirmedInvoicing() {
        try {
            entity.setStatus(BillingRunStatusEnum.CANCELED);
            billingRunService.cleanBillingRun(entity);

            billingRunService.update(entity);
            return "/pages/billing/invoicing/billingRuns.seam?edit=false";
        } catch (Exception e) {
            e.printStackTrace();
            messages.error(e.getMessage());
        }
        return null;
    }

    public String rerateConfirmedInvoicing() {
        try {
            billingRunService.retateBillingRunTransactions(entity);
            cancelConfirmedInvoicing();
            return "/pages/billing/invoicing/billingRuns.seam?edit=false";
        } catch (Exception e) {
            e.printStackTrace();
            messages.error(e.getMessage());
        }
        return null;
    }

    public String rerateInvoicing() {
        try {
            billingRunService.retateBillingRunTransactions(entity);
            cancelInvoicing();
            return "/pages/billing/invoicing/billingRuns.seam?edit=false";
        } catch (Exception e) {
            e.printStackTrace();
            messages.error(e.getMessage());
        }
        return null;
    }

    public String preInvoicingRepport(long id) {
        try {

            return "/pages/billing/invoicing/preInvoicingReports.seam?edit=false&preReport=true&objectId=" + id;

        } catch (Exception e) {
            e.printStackTrace();
            messages.error(e.getMessage());
        }
        return null;
    }

    public String postInvoicingRepport(long id) {
        try {
            return "/pages/billing/invoicing/postInvoicingReports.seam?edit=false&postReport=true&objectId=" + id;

        } catch (Exception e) {
            e.printStackTrace();
            messages.error(e.getMessage());
        }
        return null;
    }

    public String excludeBillingAccounts() {
        try {
            log.debug("excludeBillingAccounts itemSelector.size()=#0", itemSelector.getSize());
            for (Invoice invoice : itemSelector.getList()) {
                billingRunService.deleteInvoice(invoice);
            }
            messages.info(new BundleKey("messages", "info.invoicing.billingAccountExcluded"));

        } catch (Exception e) {
            log.error("unexpected exception when excluding BillingAccounts!", e);
            messages.error(e.getMessage());
            messages.error(e.getMessage());
        }

        return "/pages/billing/invoicing/postInvoicingReports.seam?edit=false&postReport=true&objectId=" + entity.getId();
    }

    /**
     * Item selector getter. Item selector keeps a state of multiselect checkboxes.
     */
    @Produces
    @Named("itemSelector")
    @ConversationScoped
    public ListItemsSelector<Invoice> getItemSelector() {
        if (itemSelector == null) {
            itemSelector = new ListItemsSelector<Invoice>(false);
        }
        return itemSelector;
    }

    /**
     * Check/uncheck all select boxes.
     */
    public void checkUncheckAll(ValueChangeEvent event) {
        itemSelector.switchMode();
    }

    /**
     * Listener of select changed event.
     */
    public void selectChanged(ValueChangeEvent event) {

        Invoice entity = (Invoice) invoicesModel.getRowData();
        log.debug("selectChanged=#0", entity != null ? entity.getId() : null);
        if (entity != null) {
            itemSelector.check(entity);
        }
        log.debug("selectChanged itemSelector.size()=#0", itemSelector.getSize());
    }

    /**
     * Resets item selector.
     */
    public void resetSelection() {
        if (itemSelector == null) {
            itemSelector = new ListItemsSelector<Invoice>(false);
        } else {
            itemSelector.reset();
        }
    }

    public DataModel getInvoicesModel() {
        return invoicesModel;
    }

    public void setInvoicesModel(DataModel invoicesModel) {
        this.invoicesModel = invoicesModel;
    }

}
