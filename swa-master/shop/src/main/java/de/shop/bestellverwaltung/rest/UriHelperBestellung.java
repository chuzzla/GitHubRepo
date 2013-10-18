package de.shop.bestellverwaltung.rest;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;

import de.shop.artikelverwaltung.rest.UriHelperArtikel;
import de.shop.bestellverwaltung.domain.Bestellposition;
import de.shop.bestellverwaltung.domain.Bestellung;
import de.shop.bestellverwaltung.domain.Lieferung;
import de.shop.kundenverwaltung.domain.Kunde;
import de.shop.kundenverwaltung.rest.UriHelperKunde;
import de.shop.util.Log;

@ApplicationScoped
@Log
public class UriHelperBestellung {
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());
	
	@Inject
	private UriHelperKunde uriHelperKunde;
	
	@Inject
	private UriHelperArtikel uriHelperArtikel;
	
	public void updateUriBestellung(Bestellung bestellung, UriInfo uriInfo) {
		final Kunde kunde = bestellung.getKunde();
		if (kunde != null) {
			
			final URI kundeUri = uriHelperKunde.getUriKunde(bestellung.getKunde(), uriInfo);
			bestellung.setKundeUri(kundeUri);
		}
		
		final List<Bestellposition> bestellpositionen = bestellung.getBestellpositionen();
		if (bestellpositionen != null && !bestellpositionen.isEmpty()) {
			for (Bestellposition bp : bestellpositionen) {
				final URI artikelUri = uriHelperArtikel.getUriArtikel(bp.getArtikel(), uriInfo);
				bp.setArtikelUri(artikelUri);
			}
		}
		
		final UriBuilder ub = uriInfo.getBaseUriBuilder()
                                     .path(BestellungResource.class)
                                     .path(BestellungResource.class, "findLieferungenByBestellungId");
		final URI uri = ub.build(bestellung.getId());
		bestellung.setLieferungenUri(uri);
				
		for (Lieferung lieferung : bestellung.getLieferungen()) {
			final List<Bestellung> bestellungen = lieferung.getBestellungen();
			final List<URI> uris = new ArrayList<URI>();
			
			for (Bestellung best : bestellungen) {
				uris.add(getUriBestellung(best, uriInfo));
			}
			lieferung.setBestellungenUris(uris);
		}
		
		LOGGER.trace(bestellung);
	}

	public URI getUriBestellung(Bestellung bestellung, UriInfo uriInfo) {
		final URI uri = uriInfo.getBaseUriBuilder()
		                       .path(BestellungResource.class)
		                       .path(BestellungResource.class, "findBestellungById")
		                       .build(bestellung.getId());
		return uri;
	}
	
	public List<URI> getUrisBestellungen(Lieferung lieferung, UriInfo uriInfo) {
		final List<Bestellung> bestellungen = lieferung.getBestellungen();
		
		final List<URI> uris = new ArrayList<URI>();
		for (Bestellung bestellung : bestellungen) {
			final URI uri = uriInfo.getBaseUriBuilder()
								   .path(BestellungResource.class)
								   .path(BestellungResource.class, "findLieferungenByBestellungId")
								   .build(bestellung.getId());
			uris.add(uri);
		}
		
		return uris;
	}
}
