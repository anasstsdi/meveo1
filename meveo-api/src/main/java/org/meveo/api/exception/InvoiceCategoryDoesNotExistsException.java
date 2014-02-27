package org.meveo.api.exception;

/**
 * @author Edward P. Legaspi
 **/
public class InvoiceCategoryDoesNotExistsException extends MeveoApiException {

	private static final long serialVersionUID = 1711605925576503027L;

	public InvoiceCategoryDoesNotExistsException() {
		super("InvoiceCategory does not exists.");
	}

	public InvoiceCategoryDoesNotExistsException(String code) {
		super("InvoiceCategory with code=" + code + " does not exists.");
	}

}
