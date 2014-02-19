package org.meveo.api.exception;

/**
 * @author Edward P. Legaspi
 * @since Nov 22, 2013
 **/
public class LanguageAlreadyExistsException extends MeveoApiException {

	private static final long serialVersionUID = 801577032960916436L;

	public LanguageAlreadyExistsException(String code) {
		super("Language with code=" + code + " already exists.");
	}

}
