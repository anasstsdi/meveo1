package org.meveo.admin.job.importexport;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.admin.User;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.job.Job;

@Startup
@Singleton
@Lock(LockType.READ)
public class ExportAccountsJob extends Job {

    @Inject
    private ExportAccountsJobBean exportAccountsJobBean;

    @Override
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
    public void execute(JobInstance jobIntstance, User currentUser) {
        super.execute(jobIntstance, currentUser);
    }

    @Override
    protected void execute(JobExecutionResultImpl result,JobInstance jobIntstance, User currentUser) throws BusinessException {
        exportAccountsJobBean.execute(result, jobIntstance.getParametres(), currentUser);
    }

    @Override
    public JobCategoryEnum getJobCategory() {
        return JobCategoryEnum.IMPORT_HIERARCHY;
    }
}