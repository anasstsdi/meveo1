package org.meveo.security;

import java.io.Serializable;


import org.meveo.model.security.Role;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.permission.spi.PermissionVoter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeveoPermissionVoter implements PermissionVoter {

	private static final Logger log = LoggerFactory.getLogger(MeveoPermissionVoter.class);

	//seems we dont really need it right now
	PartitionManager partitionManager;
	

	public MeveoPermissionVoter(PartitionManager partitionManager) {
		this.partitionManager=partitionManager;
	}

	@Override
	public VotingResult hasPermission(IdentityType identityType, Object resource, String permission) {
		MeveoUser user = (MeveoUser)identityType;
		String cacheKey = resource + "_" + permission;
		if (user != null
				&& user.getUser() != null && !user.cachedPermissions.containsKey(cacheKey)) {
			VotingResult has = VotingResult.DENY;
			if (user.getUser().getRoles() != null) {
				for (Role role : user.getUser().getRoles()) {
					if (role.hasPermission(resource.toString(), permission)) {
						has = VotingResult.ALLOW;
						break;
					}
				}
			}
			user.cachedPermissions.put(cacheKey, has);
		}
		//log.debug("hasPermission :"+resource+"_"+permission+"->"+user.cachedPermissions.get(cacheKey));
		return user.cachedPermissions.get(cacheKey);
	}

	@Override
	public VotingResult hasPermission(IdentityType arg0, Class<?> arg1,
			Serializable arg2, String arg3) {
		log.warn("hasPermission :"+arg0+","+arg1+","+arg2+","+arg3);
		return null;
	}
}
