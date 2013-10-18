package de.shop.artikelverwaltung.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Collection;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
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

import de.shop.artikelverwaltung.domain.Artikel;
import de.shop.artikelverwaltung.service.ArtikelService;
import de.shop.util.LocaleHelper;
import de.shop.util.Log;
import de.shop.util.NotFoundException;
import de.shop.util.Transactional;


@Path("/artikel")
@Produces(APPLICATION_JSON)
@Consumes
@RequestScoped
@Transactional
@Log
public class ArtikelResource {
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
	
	@Context
	private UriInfo uriInfo;
	
	@Context
	private HttpHeaders headers;
	
	@Inject
	private ArtikelService as;
	
	@Inject
	private UriHelperArtikel uriHelperArtikel;
	
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
	public Artikel findArtikelById(@PathParam("id") Long id) {
		final Artikel artikel = as.findArtikelById(id);
		if (artikel == null) {
			final String msg = "Kein Artikel gefunden mit der ID " + id;
			throw new NotFoundException(msg);
		}

		return artikel;
	}
	
	@GET
	public Collection<Artikel> findAllArtikel(@QueryParam("bezeichnung") @DefaultValue("") String bezeichnung) {
		Collection<Artikel> artikel = null;
		if ("".equals(bezeichnung)) {
			artikel = as.findVerfuegbareArtikel();
			if (artikel.isEmpty()) {
				final String msg = "Keine Artikel verfuegbar";
				throw new NotFoundException(msg);
			}
		}
		else {
			artikel = as.findArtikelByBezeichnung(bezeichnung);
			if (artikel.isEmpty()) {
				final String msg = "Kein Artikel Gefunden mit Bezeichnung: " + bezeichnung;
				throw new NotFoundException(msg);
			}
		}
		
		return artikel;
	}

	@POST
	@Consumes(APPLICATION_JSON)
	public Response createArtikel(Artikel artikel) {
		final Locale locale = localeHelper.getLocale(headers);
		artikel = as.createArtikel(artikel, locale);
		LOGGER.debugf("Artikel: {0}", artikel);
		
		final URI artikelUri = uriHelperArtikel.getUriArtikel(artikel, uriInfo);
		return Response.created(artikelUri).build();
	}
	
	@PUT
	@Consumes(APPLICATION_JSON)
	public void updateArtikel(Artikel artikel) {
		final Locale locale = localeHelper.getLocale(headers);
		final Artikel origArtikel = as.findArtikelById(artikel.getId());
		if (origArtikel == null) {
			final String msg = "Kein Artikel gefunden mit der ID " + artikel.getId();
			throw new NotFoundException(msg);
		}
		LOGGER.debugf("Artikel vorher: %s", origArtikel);
	
		origArtikel.setBezeichnung(artikel.getBezeichnung());
		origArtikel.setPreis(artikel.getPreis());
		origArtikel.setVerfuegbar(artikel.isVerfuegbar());
		
		LOGGER.debugf("Artikel nachher: %s", origArtikel);
		
		
			artikel = as.updateArtikel(origArtikel, locale);
		if (artikel == null) {
		
			final String msg = "Kein Artikel gefunden mit der ID " + origArtikel.getId();
			throw new NotFoundException(msg);
		}
	}
}
