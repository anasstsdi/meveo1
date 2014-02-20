package org.meveo.api.exception;

/**
 * @author Edward P. Legaspi
 **/
public class SellerWithChildCannotBeDeletedException extends MeveoApiException {

	private static final long serialVersionUID = 2642848995041104324L;

	public SellerWithChildCannotBeDeletedException() {
		super("Seller cannot be deleted.");
	}

	public SellerWithChildCannotBeDeletedException(String code) {
		super("Seller with code=" + code
				+ " has/have child/children and cannot be deleted.");
	}

}
