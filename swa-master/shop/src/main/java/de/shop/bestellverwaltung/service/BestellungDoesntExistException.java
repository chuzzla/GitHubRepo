package de.shop.bestellverwaltung.service;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class BestellungDoesntExistException extends AbstractBestellungServiceException {
	private static final long serialVersionUID = 2627778829648883809L;
	
	private final Long id;
	
	public  BestellungDoesntExistException(Long id) {
		super("Die Bestellung mit der ID: " + id + " existiert nicht");
		this.id = id;
	}

	public Long getId() {
		return id;
	}
}

