package org.meveo.admin.job;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.model.admin.User;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.job.Job;

@Startup
@Singleton
@Lock(LockType.READ)
public class XMLInvoiceGenerationJob extends Job {

	@Inject
	private XMLInvoiceGenerationJobBean xmlInvoiceGenerationJobBean;

	@Inject
	private ResourceBundle resourceMessages;


	@Override
	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void execute(JobInstance jobInstance, User currentUser) {
		super.execute(jobInstance, currentUser);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NEVER)
	protected void execute(JobExecutionResultImpl result, JobInstance jobInstance, User currentUser) throws BusinessException {
		xmlInvoiceGenerationJobBean.execute(result, jobInstance.getParametres(), currentUser, jobInstance);
	}

	@Override
	public JobCategoryEnum getJobCategory() {
		return JobCategoryEnum.INVOICING;
	}

	@Override
	public Map<String, CustomFieldTemplate> getCustomFields() {
        Map<String, CustomFieldTemplate> result = new HashMap<String, CustomFieldTemplate>();

		CustomFieldTemplate customFieldNbRuns = new CustomFieldTemplate();
		customFieldNbRuns.setCode("nbRuns");
		customFieldNbRuns.setAppliesTo("JOB_XMLInvoiceGenerationJob");
		customFieldNbRuns.setActive(true);
		customFieldNbRuns.setDescription(resourceMessages.getString("jobExecution.nbRuns"));
		customFieldNbRuns.setFieldType(CustomFieldTypeEnum.LONG);
		customFieldNbRuns.setValueRequired(false);
		customFieldNbRuns.setDefaultValue("1");
		result.put("nbRuns", customFieldNbRuns);

		CustomFieldTemplate customFieldNbWaiting = new CustomFieldTemplate();
		customFieldNbWaiting.setCode("waitingMillis");
		customFieldNbWaiting.setAppliesTo("JOB_XMLInvoiceGenerationJob");
		customFieldNbWaiting.setActive(true);
		customFieldNbWaiting.setDescription(resourceMessages.getString("jobExecution.waitingMillis"));
		customFieldNbWaiting.setFieldType(CustomFieldTypeEnum.LONG);
		customFieldNbWaiting.setValueRequired(false);
		customFieldNbWaiting.setDefaultValue("0");
		result.put("waitingMillis", customFieldNbWaiting);

		return result;
	}
}