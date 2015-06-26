package org.meveo.service.medina.impl;

import java.io.Serializable;

import org.meveo.model.mediation.CDRRejectionCauseEnum;

public class InvalidAdrAccessException extends ADRParsingException {
	
	private static final long serialVersionUID = -1764986344294937222L;

	public InvalidAdrAccessException(Serializable adr) {
		super(adr, CDRRejectionCauseEnum.INVALID_ACCESS);
	}

	public InvalidAdrAccessException(Serializable adr, String message) {
		super(adr, CDRRejectionCauseEnum.INVALID_ACCESS, message);
	}
}
