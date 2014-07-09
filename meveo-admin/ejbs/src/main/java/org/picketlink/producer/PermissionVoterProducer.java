package org.picketlink.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.meveo.security.MeveoPermissionVoter;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.permission.spi.PermissionVoter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PermissionVoterProducer {

	private static final Logger log = LoggerFactory.getLogger(PermissionVoterProducer.class);
	
	@Produces @ApplicationScoped
	public PermissionVoter producePermissionVoter(PartitionManager partitionManager) {
		log.info("meveo producePermissionVoter partitionManager="+partitionManager);
	   return new MeveoPermissionVoter(partitionManager);
	}

}
