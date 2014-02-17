package org.meveo.api.exception;

/**
 * @author Edward P. Legaspi
 **/
public class UserAlreadyExistsException extends MeveoApiException {

	private static final long serialVersionUID = 7623626184042544119L;

	public UserAlreadyExistsException() {

	}

	public UserAlreadyExistsException(String code) {
		super("User with code=" + code + " already exists.");
	}

}
