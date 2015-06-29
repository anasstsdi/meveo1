package org.meveo.admin.job;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.meveo.admin.async.AdvancesImportAsync;
import org.meveo.admin.async.SubListCreator;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.commons.utils.FileUtils;
import org.meveo.commons.utils.ParamBean;
import org.meveo.model.Auditable;
import org.meveo.model.admin.User;
import org.meveo.model.crm.AccountLevelEnum;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.CustomFieldTypeEnum;
import org.meveo.model.crm.Provider;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.TimerEntity;
import org.meveo.service.job.Job;

/**
 * @author Edward P. Legaspi
 **/
@Startup
@Singleton
public class AdvancesImportJob extends Job {

	@Inject
	private ResourceBundle resourceMessages;

	@Inject
	private AdvancesImportAsync advancesImportAsync;

	@SuppressWarnings("unchecked")
	@Override
	protected void execute(JobExecutionResultImpl result, TimerEntity timerEntity, User currentUser) throws BusinessException {
		try {
			Long nbRuns = new Long(1);
			Long waitingMillis = new Long(0);
			int dayOfMonth = 1;
			int dueDateDelay = 0;
			String accountCode = new String();
			try {
				nbRuns = timerEntity.getLongCustomValue("AdvancesImportJob_nbRuns").longValue();
				waitingMillis = timerEntity.getLongCustomValue("AdvancesImportJob_waitingMillis").longValue();

				if (nbRuns == -1) {
					nbRuns = (long) Runtime.getRuntime().availableProcessors();
				}
			} catch (Exception e) {
				log.warn("Cant get customFields for " + timerEntity.getJobName());
			}

			try {
				dayOfMonth = timerEntity.getLongCustomValue("AdvancesImportJob_dayOfMonth").intValue();
			} catch (NullPointerException e) {
				log.warn("Cant get customFields for " + timerEntity.getJobName());
			}

			try {
				accountCode = timerEntity.getLongCustomValue("AdvancesImportJob_accountCode").toString();
			} catch (NullPointerException e) {
				log.error("Cant get customFields for " + timerEntity.getJobName());
			}
			
			try {
				dueDateDelay = timerEntity.getLongCustomValue("AdvancesImportJob_dueDateDelay").intValue();
			} catch (NullPointerException e) {
				log.error("Cant get customFields for " + timerEntity.getJobName());
			}

			Provider provider = currentUser.getProvider();

			ParamBean parambean = ParamBean.getInstance();
			String advancesDir = parambean.getProperty("providers.rootDir", "/tmp/meveo/") + File.separator + provider.getCode() + File.separator + "imports" + File.separator
					+ "advances" + File.separator;

			String inputDir = advancesDir + "input";
			String adrExtension = parambean.getProperty("advances.extensions", "csv");
			ArrayList<String> adrExtensions = new ArrayList<String>();
			adrExtensions.add(adrExtension);

			File f = new File(inputDir);
			if (!f.exists()) {
				f.mkdirs();
			}
			File[] files = FileUtils.getFilesForParsing(inputDir, adrExtensions);
			if (files == null || files.length == 0) {
				return;
			}
			SubListCreator subListCreator = new SubListCreator(Arrays.asList(files), nbRuns.intValue());

			List<Future<String>> futures = new ArrayList<Future<String>>();
			while (subListCreator.isHasNext()) {
				futures.add(advancesImportAsync.launchAndForget((List<File>) subListCreator.getNextWorkSet(), result, timerEntity.getTimerInfo().getParametres(), currentUser,
						dayOfMonth, accountCode, dueDateDelay));
				if (subListCreator.isHasNext()) {
					try {
						Thread.sleep(waitingMillis.longValue());
					} catch (InterruptedException e) {
						log.error("", e);
					}
				}
			}

			// Wait for all async methods to finish
			for (Future<String> future : futures) {
				try {
					future.get();
				} catch (InterruptedException e) {
					// It was cancelled from outside - no interest
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					result.registerError(cause.getMessage());
					log.error("Failed to execute async method", cause);
				}
			}

		} catch (Exception e) {
			log.error("Failed to run advancesImportJob", e);
			result.registerError(e.getMessage());
		}
	}

	@Override
	public JobCategoryEnum getJobCategory() {
		return JobCategoryEnum.RATING;
	}

	@Override
	public List<CustomFieldTemplate> getCustomFields(User currentUser) {
		List<CustomFieldTemplate> result = new ArrayList<CustomFieldTemplate>();

		CustomFieldTemplate customFieldNbRuns = new CustomFieldTemplate();
		customFieldNbRuns.setCode("AdvancesImportJob_nbRuns");
		customFieldNbRuns.setAccountLevel(AccountLevelEnum.TIMER);
		customFieldNbRuns.setActive(true);
		Auditable audit = new Auditable();
		audit.setCreated(new Date());
		audit.setCreator(currentUser);
		customFieldNbRuns.setAuditable(audit);
		customFieldNbRuns.setProvider(currentUser.getProvider());
		customFieldNbRuns.setDescription(resourceMessages.getString("jobExecution.nbRuns"));
		customFieldNbRuns.setFieldType(CustomFieldTypeEnum.LONG);
		customFieldNbRuns.setLongValue(new Long(1));
		customFieldNbRuns.setValueRequired(false);
		result.add(customFieldNbRuns);

		CustomFieldTemplate customFieldNbWaiting = new CustomFieldTemplate();
		customFieldNbWaiting.setCode("AdvancesImportJob_waitingMillis");
		customFieldNbWaiting.setAccountLevel(AccountLevelEnum.TIMER);
		customFieldNbWaiting.setActive(true);
		customFieldNbWaiting.setAuditable(audit);
		customFieldNbWaiting.setProvider(currentUser.getProvider());
		customFieldNbWaiting.setDescription(resourceMessages.getString("jobExecution.waitingMillis"));
		customFieldNbWaiting.setFieldType(CustomFieldTypeEnum.LONG);
		customFieldNbWaiting.setLongValue(new Long(500));
		customFieldNbWaiting.setValueRequired(false);
		result.add(customFieldNbWaiting);

		CustomFieldTemplate dayOfMonth = new CustomFieldTemplate();
		dayOfMonth.setCode("AdvancesImportJob_dayOfMonth");
		dayOfMonth.setAccountLevel(AccountLevelEnum.TIMER);
		dayOfMonth.setActive(true);		
		dayOfMonth.setAuditable(audit);
		dayOfMonth.setProvider(currentUser.getProvider());
		dayOfMonth.setDescription(resourceMessages.getString("timer.dayOfMonth"));
		dayOfMonth.setFieldType(CustomFieldTypeEnum.LONG);
		dayOfMonth.setLongValue(new Long(1));
		dayOfMonth.setValueRequired(true);
		result.add(dayOfMonth);

		CustomFieldTemplate accountCode = new CustomFieldTemplate();
		accountCode.setCode("AdvancesImportJob_accountCode");
		accountCode.setAccountLevel(AccountLevelEnum.TIMER);
		accountCode.setActive(true);
		accountCode.setAuditable(audit);
		accountCode.setProvider(currentUser.getProvider());
		accountCode.setDescription(resourceMessages.getString("accountOperation.accountCode"));
		accountCode.setFieldType(CustomFieldTypeEnum.STRING);
		accountCode.setLongValue(new Long(1));
		accountCode.setValueRequired(true);
		result.add(accountCode);
		
		CustomFieldTemplate dueDateDelay = new CustomFieldTemplate();
		dueDateDelay.setCode("AdvancesImportJob_dueDateDelay");
		dueDateDelay.setAccountLevel(AccountLevelEnum.TIMER);
		dueDateDelay.setActive(true);
		dueDateDelay.setAuditable(audit);
		dueDateDelay.setProvider(currentUser.getProvider());
		dueDateDelay.setDescription(resourceMessages.getString("adr.dueDateDelay"));
		dueDateDelay.setFieldType(CustomFieldTypeEnum.LONG);
		dueDateDelay.setLongValue(new Long(1));
		dueDateDelay.setValueRequired(true);
		result.add(dueDateDelay);

		return result;
	}

}
