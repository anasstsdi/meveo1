/*
 * (C) Copyright 2009-2013 Manaty SARL (http://manaty.net/) and contributors.
 *
 * Licensed under the GNU Public Licence, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/gpl-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.meveo.admin.action;

import javax.enterprise.inject.Model;
import javax.inject.Inject;

import org.jboss.seam.international.status.Messages;
import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.exception.InactiveUserException;
import org.meveo.admin.exception.LoginException;
import org.meveo.admin.exception.NoRoleException;
import org.meveo.admin.exception.PasswordExpiredException;
import org.meveo.admin.exception.UnknownUserException;
import org.meveo.model.admin.User;
import org.meveo.security.MeveoUser;
import org.meveo.service.admin.impl.UserService;
import org.picketlink.annotations.PicketLink;
import org.picketlink.authentication.BaseAuthenticator;
import org.picketlink.credential.DefaultLoginCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PicketLink
@Model
public class Authenticator extends BaseAuthenticator {

	@Inject
	private UserService userService;

	@Inject
	private DefaultLoginCredentials credentials;

	private static final Logger log = LoggerFactory.getLogger(Authenticator.class);

	@Inject
	private Messages messages;

	/* Authentication errors */
	private boolean noLoginError;
	private boolean inactiveUserError;
	private boolean noRoleError;
	private boolean passwordExpired;


	public String localLogout() {
		return "loggedOut";
	}

	public void authenticate() {

		noLoginError = false;
		inactiveUserError = false;
		noRoleError = false;
		passwordExpired = false;

		User user = null;
		try {

			/* Authentication check */
			user = userService.loginChecks(credentials.getUserId(),
					credentials.getPassword());

		} catch (LoginException e) {
			log.debug("Login failed for the user {} for reason {} {}", credentials.getUserId(), e
					.getClass().getName(), e.getMessage());
			if (e instanceof InactiveUserException) {
				inactiveUserError = true;
				log.error("login failed with username=" + credentials.getUserId()
						+ " and password="
						+ credentials.getPassword()
						+ " : cause user is not active");
				messages.info(new BundleKey("messages", "user.error.inactive"));

			} else if (e instanceof NoRoleException) {
				noRoleError = true;
				log.error("The password of user " + credentials.getUserId() + " has expired.");
				messages.info(new BundleKey("messages", "user.error.noRole"));

			} else if (e instanceof PasswordExpiredException) {
				passwordExpired = true;
				log.error("The password of user " + credentials.getUserId() + " has expired.");
				messages.info(new BundleKey("messages", "user.password.expired"));

			} else if (e instanceof UnknownUserException) {
				noLoginError = true;
				log.debug("login failed with username={} and password={}",
						credentials.getUserId(),
						credentials.getPassword());
				messages.info(new BundleKey("messages", "user.error.login"));
			}
		}

		if (user == null) {
			setStatus(AuthenticationStatus.FAILURE);
		} else {

			// homeMessage = "application.home.message";

			setStatus(AuthenticationStatus.SUCCESS);
			setAccount(new MeveoUser(user));

			log.debug("End of authenticating");
		}
	}
	
	public void setLocale(String language) {
		// TODO: localeSelector.selectLanguage(language);

	}

	public boolean isNoLoginError() {
		return noLoginError;
	}

	public void setNoLoginError(boolean noLoginError) {
		this.noLoginError = noLoginError;
	}

	public boolean isInactiveUserError() {
		return inactiveUserError;
	}

	public void setInactiveUserError(boolean inactiveUserError) {
		this.inactiveUserError = inactiveUserError;
	}

	public boolean isNoRoleError() {
		return noRoleError;
	}

	public void setNoRoleError(boolean noRoleError) {
		this.noRoleError = noRoleError;
	}

	public boolean isPasswordExpired() {
		return passwordExpired;
	}

	public void setPasswordExpired(boolean passwordExpired) {
		this.passwordExpired = passwordExpired;
	}
}
