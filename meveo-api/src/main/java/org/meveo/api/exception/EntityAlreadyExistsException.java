package org.meveo.api.exception;

/**
 * @author Edward P. Legaspi
 **/
public class EntityAlreadyExistsException extends MeveoApiException {

	private static final long serialVersionUID = 4276141143551471299L;

	public EntityAlreadyExistsException() {

	}

	public EntityAlreadyExistsException(String message) {
		super(message);
	}

}
