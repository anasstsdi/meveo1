package org.meveo.service.medina.impl;

import java.io.Serializable;

import org.meveo.model.mediation.CDRRejectionCauseEnum;

public class ADRParsingException extends Exception {

	private static final long serialVersionUID = 2383961368878309626L;

	private Serializable adr;
	private CDRRejectionCauseEnum rejectionCause;

	public ADRParsingException(Serializable adr, CDRRejectionCauseEnum cause) {
		super();
		setAdr(adr);
		setRejectionCause(cause);
	}

	public ADRParsingException(Serializable adr, CDRRejectionCauseEnum cause, String message) {
		super(message);
		setAdr(adr);
		setRejectionCause(cause);
	}

	public CDRRejectionCauseEnum getRejectionCause() {
		return rejectionCause;
	}

	public void setRejectionCause(CDRRejectionCauseEnum rejectionCause) {
		this.rejectionCause = rejectionCause;
	}

	@Override
	public String getMessage() {
		return "Failed to parse ADR. Reason: " + rejectionCause + " " + super.getMessage();
	}

	public Serializable getAdr() {
		return adr;
	}

	public void setAdr(Serializable adr) {
		this.adr = adr;
	}
}
