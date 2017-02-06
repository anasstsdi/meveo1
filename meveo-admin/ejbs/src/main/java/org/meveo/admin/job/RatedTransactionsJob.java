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
public class RatedTransactionsJob extends Job {

    @Inject
    private RatedTransactionsJobBean ratedTransactionsJobBean;
    
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
        ratedTransactionsJobBean.execute(result, currentUser,jobInstance);
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
		customFieldNbRuns.setAppliesTo("JOB_RatedTransactionsJob");
		customFieldNbRuns.setActive(true);
		customFieldNbRuns.setDescription(resourceMessages.getString("jobExecution.nbRuns"));
		customFieldNbRuns.setFieldType(CustomFieldTypeEnum.LONG);
		customFieldNbRuns.setValueRequired(false);
		customFieldNbRuns.setDefaultValue("1");
		result.put("nbRuns", customFieldNbRuns);

		CustomFieldTemplate customFieldNbWaiting = new CustomFieldTemplate();
		customFieldNbWaiting.setCode("waitingMillis");
		customFieldNbWaiting.setAppliesTo("JOB_RatedTransactionsJob");
		customFieldNbWaiting.setActive(true);
		customFieldNbWaiting.setDescription(resourceMessages.getString("jobExecution.waitingMillis"));
		customFieldNbWaiting.setFieldType(CustomFieldTypeEnum.LONG);
		customFieldNbWaiting.setDefaultValue("0");
		customFieldNbWaiting.setValueRequired(false);
		result.put("waitingMillis", customFieldNbWaiting);

		return result;
	}
}