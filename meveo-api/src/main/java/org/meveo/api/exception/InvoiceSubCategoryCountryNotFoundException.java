package org.meveo.api.exception;

/**
 * @author Edward P. Legaspi
 **/
public class InvoiceSubCategoryCountryNotFoundException extends
		MeveoApiException {

	private static final long serialVersionUID = 4092129176766339674L;

	public InvoiceSubCategoryCountryNotFoundException() {
		super("InvoiceSubCategoryCountry not found.");
	}

	public InvoiceSubCategoryCountryNotFoundException(String code) {
		super("InvoiceSubCategoryCountry with code=" + code + " not found.");
	}

}
