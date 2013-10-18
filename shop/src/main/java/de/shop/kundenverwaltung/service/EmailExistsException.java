package de.shop.kundenverwaltung.service;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class EmailExistsException extends AbstractKundeServiceException {
	private static final long serialVersionUID = 2627778829648883809L;
	
	private final String email;
	
	public EmailExistsException(String email) {
		super("Die Email-Adresse " + email + " existiert bereits");
		this.email = email;
	}

	public String getEmail() {
		return email;
	}
}
