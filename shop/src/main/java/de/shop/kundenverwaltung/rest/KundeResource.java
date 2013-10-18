package de.shop.kundenverwaltung.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;

import de.shop.bestellverwaltung.domain.Bestellung;
import de.shop.bestellverwaltung.rest.UriHelperBestellung;
import de.shop.bestellverwaltung.service.BestellungService;
import de.shop.kundenverwaltung.domain.Kunde;
import de.shop.kundenverwaltung.domain.Adresse;
import de.shop.kundenverwaltung.service.KundeService;
import de.shop.util.JsonFile;
import de.shop.util.LocaleHelper;
import de.shop.util.Log;
import de.shop.util.NotFoundException;
import de.shop.util.Transactional;


@Path("/kunden")
@Produces(APPLICATION_JSON)
@Consumes
@RequestScoped
@Transactional
@Log
public class KundeResource {
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

	@Context
	private UriInfo uriInfo;
	
	@Context
	private HttpHeaders headers;
	
	@Inject
	private KundeService ks;
	
	@Inject
	private BestellungService bs;
	
	@Inject
	private UriHelperKunde uriHelperKunde;
	
	@Inject
	private UriHelperBestellung uriHelperBestellung;
	
	@Inject
	private LocaleHelper localeHelper;
	
	@PostConstruct
	private void postConstruct() {
		LOGGER.debugf("CDI-faehiges Bean %s wurde erzeugt", this);
	}
	
	@PreDestroy
	private void preDestroy() {
		LOGGER.debugf("CDI-faehiges Bean %s wird geloescht", this);
	}
	
	@GET
	@Path("{id:[1-9][0-9]*}")
	public Kunde findKundeById(@PathParam("id") Long id) {
		final Locale locale = localeHelper.getLocale(headers);
		final Kunde kunde = ks.findKundeById(id, KundeService.FetchType.NUR_KUNDE, locale);
		if (kunde == null) {
			final String msg = "Kein Kunde gefunden mit der ID " + id;
			throw new NotFoundException(msg);
		}
	
		uriHelperKunde.updateUriKunde(kunde, uriInfo);
		return kunde;
	}
	
	@GET
	@Path("/prefix/id/{id:[1-9][0-9]*}")
	public Collection<Long> findIdsByPrefix(@PathParam("id") String idPrefix) {
		final Collection<Long> ids = ks.findIdsByPrefix(idPrefix);
		return ids;
	}
	
	@GET
	public Collection<Kunde> findKundenByNachname(@QueryParam("nachname") @DefaultValue("") String nachname) {
		final Locale locale = localeHelper.getLocale(headers);
		Collection<Kunde> kunden = null;
		if ("".equals(nachname)) {
			kunden = ks.findAllKunden(KundeService.FetchType.NUR_KUNDE, null);
			if (kunden.isEmpty()) {
				final String msg = "Keine Kunden vorhanden";
				throw new NotFoundException(msg);
			}
		}
		else {
			kunden = ks.findKundenByNachname(nachname, KundeService.FetchType.NUR_KUNDE, locale);
			if (kunden.isEmpty()) {
				final String msg = "Kein Kunde gefunden mit Nachname " + nachname;
				throw new NotFoundException(msg);
			}
		}

		for (Kunde kunde : kunden) {
			uriHelperKunde.updateUriKunde(kunde, uriInfo);
		}	
		return kunden;
	}
	
	@GET
	@Path("/prefix/nachname/{nachname}")
	public Collection<String> findNachnamenByPrefix(@PathParam("nachname") String nachnamePrefix) {
		final Collection<String> nachnamen = ks.findNachnamenByPrefix(nachnamePrefix);
		return nachnamen;
	}

	@GET
	@Path("{id:[1-9][0-9]*}/bestellungen")
	public Collection<Bestellung> findBestellungenByKundeId(@PathParam("id") Long kundeId) {
		final Locale locale = localeHelper.getLocale(headers);
		final Kunde kunde = ks.findKundeById(kundeId, KundeService.FetchType.NUR_KUNDE, locale);
		final Collection<Bestellung> bestellungen = bs.findBestellungenByKunde(kunde);
		if (bestellungen.isEmpty()) {
			final String msg = "Kein Kunde gefunden mit der ID " + kundeId;
			throw new NotFoundException(msg);
		}
		
		for (Bestellung bestellung : bestellungen) {
			uriHelperBestellung.updateUriBestellung(bestellung, uriInfo);
		}
		
		return bestellungen;
	}
	
	@GET
	@Path("{id:[1-9][0-9]*}/bestellungenIds")
	public Collection<Long> findBestellungenIdsByKundeId(@PathParam("id") Long kundeId) {
		final Collection<Bestellung> bestellungen = findBestellungenByKundeId(kundeId);
		if (bestellungen.isEmpty()) {
			final String msg = "Kein Kunde gefunden mit der ID " + kundeId;
			throw new NotFoundException(msg);
		}
		
		final int anzahl = bestellungen.size();
		final Collection<Long> bestellungenIds = new ArrayList<>(anzahl);
		for (Bestellung bestellung : bestellungen) {
			bestellungenIds.add(bestellung.getId());
		}
		
		return bestellungenIds;
	}
	
	@POST
	@Consumes(APPLICATION_JSON)
	@Produces
	public Response createKunde(Kunde kunde) {
		final Adresse adresse = kunde.getAdresse();
		if (adresse != null) {
			adresse.setKunde(kunde);
		}
		
		final List<Locale> locales = headers.getAcceptableLanguages();
		final Locale locale = locales.isEmpty() ? Locale.getDefault() : locales.get(0);
		kunde = ks.createKunde(kunde, locale);
		LOGGER.debugf("Kunde: {0}", kunde);
		
		final URI kundeUri = uriHelperKunde.getUriKunde(kunde, uriInfo);
		return Response.created(kundeUri).build();
	}
	
	@PUT
	@Consumes(APPLICATION_JSON)
	public void updateKunde(Kunde kunde, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
		final Locale locale = localeHelper.getLocale(headers);
	final Kunde origKunde = ks.findKundeById(kunde.getId(), KundeService.FetchType.NUR_KUNDE, locale);
		if (origKunde == null) {
			final String msg = "Kein Kunde gefunden mit der ID " + kunde.getId();
			throw new NotFoundException(msg);
		}
		LOGGER.debugf("Kunde vorher: %s", origKunde);
	
		origKunde.setValues(kunde);
		LOGGER.debugf("Kunde nachher: %s", origKunde);
		
		kunde = ks.updateKunde(origKunde, locale, false);
		if (kunde == null) {
		
			final String msg = "Kein Kunde gefunden mit der ID " + origKunde.getId();
			throw new NotFoundException(msg);
		}
	}
	
	@Path("{id:[1-9][0-9]*}")
	@DELETE
	@Produces
	public void deleteKunde(@PathParam("id") long kundeId) {
		ks.deleteKundeById(kundeId);
	}
	
	@Path("{id:[1-9][0-9]*}/file")
	@POST
	@Consumes(APPLICATION_JSON)
	public Response upload(@PathParam("id") Long kundeId, JsonFile file) {
		final Locale locale = localeHelper.getLocale(headers);
		ks.setFile(kundeId, file.getBytes(), locale);
		final URI location = uriHelperKunde.getUriDownload(kundeId, uriInfo);
		return Response.created(location).build();
	}
	
	@Path("{id:[1-9][0-9]*}/file")
	@GET
	public JsonFile download(@PathParam("id") Long kundeId) throws IOException {
		final Locale locale = localeHelper.getLocale(headers);
		final Kunde kunde = ks.findKundeById(kundeId, KundeService.FetchType.NUR_KUNDE, locale);
		if (kunde.getFile() == null) {
			return new JsonFile(new byte[] {});
		}
		
		return new JsonFile(kunde.getFile().getBytes());
	}
}
