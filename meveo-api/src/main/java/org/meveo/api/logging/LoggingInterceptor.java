package org.meveo.api.logging;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.slf4j.Logger;

/**
 * @author Edward P. Legaspi
 **/
@Logged
@Interceptor
public class LoggingInterceptor {

	@Inject
	private Logger log;

	@AroundInvoke
	public Object aroundInvoke(InvocationContext invocationContext)
			throws Exception {
		log.debug("\r\n\r\n===========================================================");
		log.debug("Entering method: "
				+ invocationContext.getMethod().getName().toUpperCase()
				+ " in class "
				+ invocationContext.getMethod().getDeclaringClass().getName());

		if (invocationContext.getParameters() != null) {
			for (Object obj : invocationContext.getParameters()) {
				log.debug(obj.toString());
			}
		}

		return invocationContext.proceed();
	}

}
