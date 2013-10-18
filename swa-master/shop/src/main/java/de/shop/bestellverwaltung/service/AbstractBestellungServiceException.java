package de.shop.bestellverwaltung.service;

import de.shop.util.AbstractShopException;

public abstract class AbstractBestellungServiceException extends AbstractShopException {


	/**
	 * 
	 */
	private static final long serialVersionUID = -8555422301930073102L;

	public AbstractBestellungServiceException(String msg) {
		super(msg);
	}
}
