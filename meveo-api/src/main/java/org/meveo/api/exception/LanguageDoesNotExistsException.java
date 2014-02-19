package org.meveo.api.exception;

/**
 * @author Edward P. Legaspi
 * @since Nov 12, 2013
 **/
public class LanguageDoesNotExistsException extends MeveoApiException {

	private static final long serialVersionUID = -6159383154906455450L;

	public LanguageDoesNotExistsException(String code) {
		super("Language with code=" + code + " does not exists.");
	}

}
