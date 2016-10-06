package org.meveo.admin.job;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.async.SubListCreator;
import org.meveo.admin.async.WorkflowAsync;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.BaseEntity;
import org.meveo.model.IEntity;
import org.meveo.model.admin.User;
import org.meveo.model.crm.EntityReferenceWrapper;
import org.meveo.model.filter.Filter;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.wf.Workflow;
import org.meveo.service.crm.impl.CustomFieldInstanceService;
import org.meveo.service.filter.FilterService;
import org.meveo.service.wf.WorkflowService;
import org.slf4j.Logger;

@Stateless
public class WorkflowJobBean {

	@Inject
	private Logger log;

	@Inject
	private FilterService filterService;
	
	@Inject
	private WorkflowService workflowService;

	@Inject
	private WorkflowAsync workflowAsync;
	
    @Inject
    protected CustomFieldInstanceService customFieldInstanceService;

	@SuppressWarnings("unchecked")
    @Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void execute(JobExecutionResultImpl result, User currentUser, JobInstance jobInstance) {
		log.debug("Running for user={}, parameter={}", currentUser, jobInstance.getParametres());
		
		try {			
			Long nbRuns = new Long(1);		
			Long waitingMillis = new Long(0);
			String filterCode = null;
			String workflowCode = null;
			try{
				nbRuns = (Long) customFieldInstanceService.getCFValue(jobInstance, "wfJob_nbRuns", currentUser);  			
				waitingMillis = (Long) customFieldInstanceService.getCFValue(jobInstance, "wfJob_waitingMillis$", currentUser);
				if(nbRuns == -1){
					nbRuns  = (long) Runtime.getRuntime().availableProcessors();
				}
				filterCode = ((EntityReferenceWrapper) customFieldInstanceService.getCFValue(jobInstance, "wfJob_filter", currentUser)).getCode();
				workflowCode = ((EntityReferenceWrapper) customFieldInstanceService.getCFValue(jobInstance, "wfJob_workflow", currentUser)).getCode();
			}catch(Exception e){
				log.warn("Cant get customFields for "+jobInstance.getJobTemplate(),e.getMessage());
				log.error("error:",e);
				nbRuns = new Long(1);
				waitingMillis = new Long(0);				
			}
			
			Filter filter = filterService.findByCode(filterCode, currentUser.getProvider());
			Workflow workflow = workflowService.findByCode(workflowCode, currentUser.getProvider());

			log.debug("filter:{}",filter == null ? null : filter.getCode());
			List<? extends IEntity> entities = filterService.filteredListAsObjects(filter, currentUser);
			log.debug("entities:" + entities.size());
			result.setNbItemsToProcess(entities.size());
			
			List<Future<String>> futures = new ArrayList<Future<String>>();
	    	SubListCreator subListCreator = new SubListCreator(entities,nbRuns.intValue());
	    	log.debug("block to run:" + subListCreator.getBlocToRun());
	    	log.debug("nbThreads:" + nbRuns);
			while (subListCreator.isHasNext()) {	
				futures.add(workflowAsync.launchAndForget((List<BaseEntity>) subListCreator.getNextWorkSet(),workflow,result, currentUser));

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
            log.error("Failed to run workflow job",e);
            result.registerError(e.getMessage());
        }
	}


}
