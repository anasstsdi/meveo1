package org.meveo.security;


import java.util.HashMap;
import java.util.Map;

import org.picketlink.idm.model.basic.User;
import org.picketlink.idm.permission.spi.PermissionVoter.VotingResult;

public class MeveoUser extends User {

	private static final long serialVersionUID = 4333140556503076034L;

	private org.meveo.model.admin.User user;
	public Map<String, VotingResult> cachedPermissions = new HashMap<String, VotingResult>();

	public MeveoUser(org.meveo.model.admin.User user) {
		this.user = user;
	}

	public String getKey() {
		return getId();
	}

	public String getId() {
		return user.getUserName();
	}

	public org.meveo.model.admin.User getUser() {
		return this.user;
	}
	
}
