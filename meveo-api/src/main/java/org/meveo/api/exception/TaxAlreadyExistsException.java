package org.meveo.api.exception;

/**
 * @author Edward P. Legaspi
 **/
public class TaxAlreadyExistsException extends MeveoApiException {

	private static final long serialVersionUID = -3009755850115480507L;

	public TaxAlreadyExistsException(String code) {
		super("Tax with code=" + code + " already exists.");
	}

}
