package org.meveo.service.medina.impl;

import org.meveo.model.mediation.CDRRejectionCauseEnum;

public class InvalidAdrFormatException extends ADRParsingException {

	private static final long serialVersionUID = 7574354192096751354L;

	public InvalidAdrFormatException(String adrString) {
		super(adrString, CDRRejectionCauseEnum.INVALID_FORMAT);
	}

	public InvalidAdrFormatException(String adrString, String message) {
		super(adrString, CDRRejectionCauseEnum.INVALID_FORMAT, message);
	}
	
}
