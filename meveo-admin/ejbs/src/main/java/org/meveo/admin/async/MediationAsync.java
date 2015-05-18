/**
 * 
 */
package org.meveo.admin.async;

import java.io.File;
import java.util.List;
import java.util.concurrent.Future;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.job.MediationJobBean;
import org.meveo.model.admin.User;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.slf4j.Logger;

/**
 * @author anasseh
 * 
 */

@Stateless
public class MediationAsync {

    @Inject
    private MediationJobBean mediationJobBean;

    @Inject
    protected Logger log;

    @Asynchronous
    public Future<String> launchAndForget(List<File> files, JobExecutionResultImpl result, String parameter, User currentUser) {
        for (File file : files) {
            mediationJobBean.execute(result, parameter, currentUser, file);
        }

        return new AsyncResult<String>("OK");
    }
}