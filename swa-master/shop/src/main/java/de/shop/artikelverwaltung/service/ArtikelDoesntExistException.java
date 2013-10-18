package de.shop.artikelverwaltung.service;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class ArtikelDoesntExistException extends AbstractArtikelServiceException {
	private static final long serialVersionUID = 2627778829648883809L;
	
	private final Long id;
	
	public  ArtikelDoesntExistException(Long id) {
		super("Der Artikel mit der ID: " + id + " existiert nicht");
		this.id = id;
	}

	public Long getId() {
		return id;
	}
}
