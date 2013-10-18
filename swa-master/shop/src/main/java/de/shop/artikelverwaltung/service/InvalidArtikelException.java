package de.shop.artikelverwaltung.service;

import java.util.Collection;

import javax.ejb.ApplicationException;
import javax.validation.ConstraintViolation;

import de.shop.artikelverwaltung.domain.Artikel;

@ApplicationException(rollback = true)
public class InvalidArtikelException extends ArtikelValidationException {
	private static final long serialVersionUID = 4255133082483647701L;

	private Long id;
	private String bezeichnung;
	private Double preis;
	
	public InvalidArtikelException(Artikel artikel,
            Collection<ConstraintViolation<Artikel>> violations) {
		super(artikel, violations);
			if (artikel != null) {
			this.id = artikel.getId();
			this.bezeichnung = artikel.getBezeichnung();
			this.preis = artikel.getPreis();
			}
	}
			
			/*public InvalidKundeException(Long id, Collection<ConstraintViolation<Kunde>> violations) {
				super(artikel.id, violations);
				this.id = id;
			}
			
			public InvalidArtikelException(Artikel artikel, Collection<ConstraintViolation<Artikel>> violations) {
				super(violations);
				this.bezeichnung = bezeichnung;
			}
			*/
			public Long getId() {
				return id;
			}
			public String getBezeichnung() {
				return bezeichnung;
			}
			public Double getPreis() {
				return preis;
			}
			
			@Override
			public String toString() {
				return "{bezeichnung=" + bezeichnung + ", preis=" + preis + "}";
		}
	}
