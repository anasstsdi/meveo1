package org.meveo.api.exception;

/**
 * @author Edward P. Legaspi
 **/
public class TaxDoesNotExistsException extends MeveoApiException {

	private static final long serialVersionUID = -8674771633432863482L;

	public TaxDoesNotExistsException(String code) {
		super("Tax with code=" + code + " does not exists.");
	}

}
