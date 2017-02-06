package org.meveo.admin.job;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.admin.User;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.crm.impl.CustomFieldInstanceService;
import org.meveo.service.job.Job;

@Startup
@Singleton
@Lock(LockType.READ)
public class BillingRunJob extends Job {

    @Inject
    private BillingRunJobBean billingRunJobBean;
    
    @Inject
    private CustomFieldInstanceService customFieldInstanceService;

    @Override
    protected void execute(JobExecutionResultImpl result, JobInstance jobInstance, User currentUser) throws BusinessException {
        String billingCycle = (String) customFieldInstanceService.getCFValue(jobInstance, "BillingRunJob_billingCycle", currentUser);
        Date lastTransactionDate = (Date) customFieldInstanceService.getCFValue(jobInstance, "BillingRunJob_lastTransactionDate", currentUser);
        Date invoiceDate = (Date) customFieldInstanceService.getCFValue(jobInstance, "BillingRunJob_invoiceDate", currentUser);

        billingRunJobBean.execute(result, jobInstance.getParametres(), billingCycle, invoiceDate, lastTransactionDate, currentUser);
    }

    @Override
    public JobCategoryEnum getJobCategory() {
        return JobCategoryEnum.INVOICING;
    }

    @Override
    public Map<String, CustomFieldTemplate> getCustomFields() {
        Map<String, CustomFieldTemplate> result = new HashMap<String, CustomFieldTemplate>();
        
        CustomFieldTemplate lastTransactionDate = new CustomFieldTemplate();
        lastTransactionDate.setCode("BillingRunJob_lastTransactionDate");
        lastTransactionDate.setAppliesTo("JOB_BillingRunJob");
        lastTransactionDate.setActive(true);
        lastTransactionDate.setDescription("last transaction date");
        lastTransactionDate.setFieldType(CustomFieldTypeEnum.DATE);
        lastTransactionDate.setValueRequired(false);
        result.put("BillingRunJob_lastTransactionDate", lastTransactionDate);

        CustomFieldTemplate invoiceDate = new CustomFieldTemplate();
        invoiceDate.setCode("BillingRunJob_invoiceDate");
        invoiceDate.setAppliesTo("JOB_BillingRunJob");
        invoiceDate.setActive(true);
        invoiceDate.setDescription("invoice date");
        invoiceDate.setFieldType(CustomFieldTypeEnum.DATE);
        invoiceDate.setValueRequired(false);
        result.put("BillingRunJob_invoiceDate", invoiceDate);

        CustomFieldTemplate billingCycle = new CustomFieldTemplate();
        billingCycle.setCode("BillingRunJob_billingCycle");
        billingCycle.setAppliesTo("JOB_BillingRunJob");
        billingCycle.setActive(true);
        billingCycle.setDescription("billing cycle");
        billingCycle.setFieldType(CustomFieldTypeEnum.STRING);
        billingCycle.setValueRequired(true);
        billingCycle.setMaxValue(50L);
        result.put("BillingRunJob_billingCycle", billingCycle);

        return result;
    }

}
