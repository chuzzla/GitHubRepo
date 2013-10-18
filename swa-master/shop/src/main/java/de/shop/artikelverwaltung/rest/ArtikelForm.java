package de.shop.artikelverwaltung.rest;

import javax.ws.rs.FormParam;

public class ArtikelForm {
	@FormParam("bezeichnung")
	private String bezeichnung;

	@FormParam("preis")
	private Double preis;
	
	@Override
	public String toString() {
		return "ArtikelForm [bezeichnung=" + bezeichnung + ", preis=" + preis + ']';
	}
		
		public String getBezeichnung() {
			return bezeichnung;
		}

		public void setBezeichnung(String bezeichnung) {
			this.bezeichnung = bezeichnung;
		}

		public Double getPreis() {
			return preis;
		}

		public void setPreis(Double preis) {
			this.preis = preis;
		}
}
