package org.meveo.asg.api;

import java.util.Date;

import javax.inject.Inject;

import org.meveo.api.BaseApi;
import org.meveo.asg.api.model.EntityCodeEnum;
import org.meveo.asg.api.service.AsgIdMappingService;
import org.meveo.model.Auditable;

/**
 * @author Edward P. Legaspi
 **/
public abstract class BaseAsgApi extends BaseApi {

	@Inject
	protected AsgIdMappingService asgIdMappingService;

	public void removeAsgMapping(String asgId, EntityCodeEnum entityType) {
		asgIdMappingService.removeByCodeAndType(em, asgId, entityType);
	}

	public boolean isUpdateable(Date newTimeStamp, Auditable auditable) {
		if (auditable != null && auditable.getUpdated() != null
				&& newTimeStamp != null
				&& (newTimeStamp.compareTo(auditable.getUpdated()) < 0)) {
			return false;
		}

		return true;
	}
}
