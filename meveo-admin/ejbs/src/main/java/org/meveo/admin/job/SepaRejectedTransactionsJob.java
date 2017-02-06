package org.meveo.admin.job;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.admin.sepa.PaynumFile;
import org.meveo.commons.utils.FileUtils;
import org.meveo.commons.utils.ImportFileFiltre;
import org.meveo.commons.utils.ParamBean;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.admin.User;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.Provider;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.job.Job;
import org.meveo.service.payments.impl.DDRequestItemService;

@Startup
@Singleton
@Lock(LockType.READ)
public class SepaRejectedTransactionsJob extends Job {

    @Inject
    private DDRequestItemService sepaService;
    @Inject
    private PaynumFile paynumFile;

    ParamBean param = ParamBean.getInstance();

    String importDir =  param.getProperty("providers.rootDir", "/tmp/meveo");

    @Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
    @Override
    protected void execute(JobExecutionResultImpl result, JobInstance jobInstance, User currentUser) throws BusinessException {
        Provider provider = currentUser.getProvider();
        String fileFormat = (String) customFieldInstanceService.getCFValue(jobInstance, "fileFormat", currentUser);
        String defaultPrefix = "PAYNUM".equalsIgnoreCase(fileFormat) ? "*":"Pain002_";
        String defaultExtension =  "PAYNUM".equalsIgnoreCase(fileFormat) ? "csv":"xml";
        
       
        String dirIN = importDir + File.separator + provider.getCode() + File.separator + "rejectedSepaTransactions" + File.separator + "input";
        log.info("dirIN=" + dirIN);
        String dirOK = importDir + File.separator + provider.getCode() + File.separator + "rejectedSepaTransactions" + File.separator + "output";
        String dirKO = importDir + File.separator + provider.getCode() + File.separator + "rejectedSepaTransactions" + File.separator + "reject";
        String prefix = param.getProperty(fileFormat.toLowerCase()+"RejectedTransactionsJob.file.prefix", defaultPrefix );
        String ext = param.getProperty(fileFormat.toLowerCase()+"RejectedTransactionsJob.file.extension", defaultExtension);

        try {

            File dir = new File(dirIN);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            List<File> files = getFilesToProcess(dir, prefix, ext);
            int numberOfFiles = files.size();
            log.info("InputFiles job " + numberOfFiles + " to import");
            result.setNbItemsToProcess(numberOfFiles);
            for (File file : files) {
                File currentFile = null;
                try {
                    log.info("InputFiles job " + file.getName() + " in progres");
                    currentFile = FileUtils.addExtension(file, ".processing");
                    if("PAYNUM".equalsIgnoreCase(fileFormat)){
                    	paynumFile.processRejectFile(currentFile, file.getName(), currentUser);
                    }else{
                    	sepaService.processRejectFile(currentFile, file.getName(), currentUser);
                    }
                    
                    FileUtils.moveFile(dirOK, currentFile, file.getName());
                    log.info("InputFiles job " + file.getName() + " done");
                    result.registerSucces();
                    
                } catch (Exception e) {
                    result.registerError(e.getMessage());
                    log.error("InputFiles job " + file.getName() + " failed", e);
                    FileUtils.moveFile(dirKO, currentFile, file.getName());
                } finally {
                    if (currentFile != null) {
                        currentFile.delete();
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to sepa reject transaction", e);
        }
    }

    // public Collection<Timer> getTimers() {
    // return this.timerService.getTimers();
    // }

    @Override
    public JobCategoryEnum getJobCategory() {
        return JobCategoryEnum.ACCOUNT_RECEIVABLES;
    }
    
	private List<File> getFilesToProcess(File dir, String prefix, String ext) {
		List<File> files = new ArrayList<File>();
		ImportFileFiltre filtre = new ImportFileFiltre(prefix, ext);
		File[] listFile = dir.listFiles(filtre);
		if (listFile == null) {
			return files;
		}
		for (File file : listFile) {
			if (file.isFile()) {
				files.add(file);
			}
		}
		return files;
	}
	
	   @Override
		public Map<String, CustomFieldTemplate> getCustomFields() {
	        Map<String, CustomFieldTemplate> result = new HashMap<String, CustomFieldTemplate>();		
			CustomFieldTemplate formatTransfo = new CustomFieldTemplate();
			formatTransfo.setCode("fileFormat");
			formatTransfo.setAppliesTo("JOB_SepaRejectedTransactionsJob");
			formatTransfo.setActive(true);
			formatTransfo.setDefaultValue("SEPA");
			formatTransfo.setDescription("File format");
			formatTransfo.setFieldType(CustomFieldTypeEnum.LIST);
			formatTransfo.setValueRequired(false);
			Map<String,String> listValues = new HashMap<String,String>();
			listValues.put("SEPA","SEPA");
			listValues.put("PAYNUM","PAYNUM");
			formatTransfo.setListValues(listValues);
			result.put("fileFormat", formatTransfo);

			return result;
		}
}
