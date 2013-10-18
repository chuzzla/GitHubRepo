package de.shop.artikelverwaltung.service;

import java.util.Collection;
import java.util.Date;

import javax.ejb.ApplicationException;
import javax.validation.ConstraintViolation;

import de.shop.artikelverwaltung.domain.Artikel;

@ApplicationException(rollback = true)
public class ArtikelValidationException extends AbstractArtikelServiceException {
private static final long serialVersionUID = -4634273195424451330L;
	
	private final Date erzeugt;
	private final Collection<ConstraintViolation<Artikel>> violations;
	
	public ArtikelValidationException(Artikel artikel,
			                             Collection<ConstraintViolation<Artikel>> violations) {
		super(violations.toString());
		
		if (artikel == null) {
			this.erzeugt = null;
		}
		else {
			this.erzeugt = artikel.getErzeugt();
		}
		
		this.violations = violations;
	}
	
	public Date getErzeugt() {
		return erzeugt == null ? null : (Date) erzeugt.clone();
	}
	
	public Collection<ConstraintViolation<Artikel>> getViolations() {
		return violations;
	}
}
