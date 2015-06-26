package org.meveo.admin.async;

import java.io.File;
import java.util.List;
import java.util.concurrent.Future;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.job.AdvancesImportJobBean;
import org.meveo.model.admin.User;
import org.meveo.model.jobs.JobExecutionResultImpl;

/**
 * @author Edward P. Legaspi
 **/
@Stateless
public class AdvancesImportAsync {

	@Inject
	private AdvancesImportJobBean advancesImportJobBean;

	@Asynchronous
	public Future<String> launchAndForget(List<File> files, JobExecutionResultImpl result, String parameter, User currentUser, int dayOfMonth, String accountCode, int dueDateDelay) {
		for (File file : files) {
			advancesImportJobBean.execute(result, parameter, currentUser, file, dayOfMonth, accountCode, dueDateDelay);
		}

		return new AsyncResult<String>("OK");
	}

}
