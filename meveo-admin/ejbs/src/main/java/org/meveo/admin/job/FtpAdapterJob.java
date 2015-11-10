package org.meveo.admin.job;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.admin.User;
import org.meveo.model.crm.AccountLevelEnum;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.job.Job;

@Startup
@Singleton
public class FtpAdapterJob extends Job {

	@Inject
	FtpAdapterJobBean ftpAdapterJobBean;

	@Inject
	private ResourceBundle resourceMessages;

	@Override
	@Asynchronous
	public void execute(JobInstance jobInstance, User currentUser) {
		super.execute(jobInstance, currentUser);
	}

	@Override
	@Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
	protected void execute(JobExecutionResultImpl result, JobInstance jobInstance, User currentUser) throws BusinessException {
		String distDirectory = "/Users/anasseh/manaty/in";
		String remoteServer = "ec2-54-172-112-179.compute-1.amazonaws.com";
		String remotePort = "21";
		String removeDistantFile = "false";
		String ftpInputDirectory = "/usr/local/jboss-as-7.2.0.docapost/ExportComptable/test";
		String ftpUsername = "billing";
		String ftpPassword = "billing";
		String extention = "xml";
		try {
//			distDirectory = (String) jobInstance.getCFValue("FtpAdapter_distDirectory");
		//remoteServer = (String) jobInstance.getCFValue("FtpAdapter_remoteServer");
//			remotePort = (String) jobInstance.getCFValue("FtpAdapter_remotePort");
//			//removeDistantFile = (String) jobInstance.getCFValue("FtpAdapter_removeDistantFile");
//			ftpInputDirectory = (String) jobInstance.getCFValue("FtpAdapter_ftpInputDirectory");
//			ftpUsername = (String) jobInstance.getCFValue("FtpAdapter_ftpUsername");
//			ftpPassword = (String) jobInstance.getCFValue("FtpAdapter_ftpPassword");

		} catch (Exception e) {
			log.warn("Cant get customFields for " + jobInstance.getJobTemplate(), e);
		}
		ftpAdapterJobBean.execute(result, jobInstance, currentUser, distDirectory, remoteServer, Integer.parseInt(remotePort), "true".equals(removeDistantFile), ftpInputDirectory,extention, ftpUsername, ftpPassword);
	}

	@Override
	public JobCategoryEnum getJobCategory() {
		return JobCategoryEnum.UTILS;
	}

	@Override
	public Map<String, CustomFieldTemplate> getCustomFields() {
		Map<String, CustomFieldTemplate> result = new HashMap<String, CustomFieldTemplate>();

//		CustomFieldTemplate distDirectory = new CustomFieldTemplate();
//		distDirectory.setCode("FtpAdapter_distDirectory");
//		distDirectory.setAccountLevel(AccountLevelEnum.TIMER);
//		distDirectory.setActive(true);
//		distDirectory.setDescription(resourceMessages.getString("FtpAdapter.distDirectory"));
//		distDirectory.setFieldType(CustomFieldTypeEnum.STRING);
//		distDirectory.setValueRequired(false);
//		result.put("FtpAdapter_distDirectory", distDirectory);
//
//		CustomFieldTemplate remoteServer = new CustomFieldTemplate();
//		remoteServer.setCode("FtpAdapter_remoteServer");
//		remoteServer.setAccountLevel(AccountLevelEnum.TIMER);
//		remoteServer.setActive(true);
//		remoteServer.setDescription(resourceMessages.getString("FtpAdapter.remoteServer"));
//		remoteServer.setFieldType(CustomFieldTypeEnum.STRING);
//		remoteServer.setValueRequired(false);
//		result.put("FtpAdapter_remoteServer", remoteServer);
//
//		CustomFieldTemplate remotePort = new CustomFieldTemplate();
//		remotePort.setCode("FtpAdapter_remotePort");
//		remotePort.setAccountLevel(AccountLevelEnum.TIMER);
//		remotePort.setActive(true);
//		remotePort.setDescription(resourceMessages.getString("FtpAdapter.remotePort"));
//		remotePort.setFieldType(CustomFieldTypeEnum.STRING);
//		remotePort.setValueRequired(false);
//		result.put("FtpAdapter_remotePort", remotePort);

//		CustomFieldTemplate removeDistantFile = new CustomFieldTemplate();
//		removeDistantFile.setCode("FtpAdapter_removeDistantFile");
//		removeDistantFile.setAccountLevel(AccountLevelEnum.TIMER);
//		removeDistantFile.setActive(true);
//		removeDistantFile.setDescription(resourceMessages.getString("FtpAdapter.removeDistantFile"));
//		removeDistantFile.setFieldType(CustomFieldTypeEnum.LIST);
//		Map<String, String> removeDistantFileListValues = new HashMap<String, String>();
//		removeDistantFileListValues.put("TRUE", "True");
//		removeDistantFileListValues.put("FALSE", "False");
//		removeDistantFile.setListValues(removeDistantFileListValues);
//		removeDistantFile.setValueRequired(false);
//		result.put("FtpAdapter_removeDistantFile", removeDistantFile);

//		CustomFieldTemplate ftpInputDirectory = new CustomFieldTemplate();
//		ftpInputDirectory.setCode("FtpAdapter_ftpInputDirectory");
//		ftpInputDirectory.setAccountLevel(AccountLevelEnum.TIMER);
//		ftpInputDirectory.setActive(true);
//		ftpInputDirectory.setDescription(resourceMessages.getString("FtpAdapter.ftpInputDirectory"));
//		ftpInputDirectory.setFieldType(CustomFieldTypeEnum.STRING);
//		ftpInputDirectory.setValueRequired(false);
//		result.put("FtpAdapter_ftpInputDirectory", ftpInputDirectory);
//
//		CustomFieldTemplate ftpUsername = new CustomFieldTemplate();
//		ftpUsername.setCode("FtpAdapter_ftpUsername");
//		ftpUsername.setAccountLevel(AccountLevelEnum.TIMER);
//		ftpUsername.setActive(true);
//		ftpUsername.setDescription(resourceMessages.getString("FtpAdapter.ftpUsername"));
//		ftpUsername.setFieldType(CustomFieldTypeEnum.STRING);
//		ftpUsername.setValueRequired(false);
//		result.put("FtpAdapter_ftpUsername", ftpUsername);
//
//		CustomFieldTemplate ftpPassword = new CustomFieldTemplate();
//		ftpPassword.setCode("FtpAdapter_ftpPassword");
//		ftpPassword.setAccountLevel(AccountLevelEnum.TIMER);
//		ftpPassword.setActive(true);
//		ftpPassword.setDescription(resourceMessages.getString("FtpAdapter.ftpPassword"));
//		ftpPassword.setFieldType(CustomFieldTypeEnum.STRING);
//		ftpPassword.setValueRequired(false);
//		result.put("FtpAdapter_ftpPassword", ftpPassword);

		return result;
	}
}
