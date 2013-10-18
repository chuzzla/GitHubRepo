package de.shop.artikelverwaltung.controller;

import static de.shop.util.Constants.JSF_INDEX;
import static de.shop.util.Constants.JSF_REDIRECT_SUFFIX;
import static de.shop.util.Messages.MessagesType.ARTIKELVERWALTUNG;
import static javax.ejb.TransactionAttributeType.REQUIRED;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.TransactionAttribute;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.OptimisticLockException;
import javax.servlet.http.HttpSession;
import org.jboss.logging.Logger;
import org.richfaces.cdi.push.Push;

import de.shop.artikelverwaltung.domain.Artikel;
import de.shop.artikelverwaltung.service.AbstractArtikelServiceException;
import de.shop.artikelverwaltung.service.ArtikelService;
import de.shop.artikelverwaltung.service.InvalidArtikelException;
import de.shop.util.Client;
import de.shop.util.ConcurrentDeletedException;
import de.shop.util.Log;
import de.shop.util.Messages;
import de.shop.util.Transactional;


/**
 * Dialogsteuerung fuer die ArtikelService
 */
@Named("ac")
@SessionScoped
@Log
public class ArtikelController implements Serializable {
	private static final long serialVersionUID = 1564024850446471639L;

	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());
	
	private static final String JSF_ARTIKELVERWALTUNG = "/artikelverwaltung/";
	private static final String JSF_LIST_ARTIKEL = "/artikelverwaltung/listArtikel";
	private static final String JSF_VIEW_ARTIKEL = JSF_ARTIKELVERWALTUNG + "viewArtikel"; 
	private static final String JSF_UPDATE_ARTIKEL = JSF_ARTIKELVERWALTUNG + "updateArtikel";
	private static final int ANZAHL_LADENHUETER = 5;
	
	private static final String JSF_SELECT_ARTIKEL = "/artikelverwaltung/selectArtikel";
	private static final String SESSION_VERFUEGBARE_ARTIKEL = "verfuegbareArtikel";
	private static final String MSG_KEY_UPDATE_ARTIKEL_CONCURRENT_UPDATE = "updateArtikel.concurrentUpdate";
	private static final String MSG_KEY_UPDATE_ARTIKEL_CONCURRENT_DELETE = "updateArtikel.concurrentDelete";
	
	private Artikel artikel;
	private Artikel neuerArtikel;

	private String bezeichnung;
	
	private boolean geaendertArtikel;
	
	private List<Artikel> ladenhueter;
	private List<Artikel> artikelNachBez;

	@Inject
	private ArtikelService as;
	
	
	@Inject
	@Push(topic = "updateArtikel")
	private transient Event<String> updateArtikelEvent;
	
	@Inject
	private transient HttpSession session;
	
	@Inject
	private Messages messages;
	
	@Inject
	@Client
	private Locale locale;



	
	@PostConstruct
	private void postConstruct() {
		LOGGER.debugf("CDI-faehiges Bean %s wurde erzeugt", this);
	}

	@PreDestroy
	private void preDestroy() {
		LOGGER.debugf("CDI-faehiges Bean %s wird geloescht", this);
	}
	
	private String createArtikelErrorMsg(AbstractArtikelServiceException e) {
		final Class<? extends AbstractArtikelServiceException> exceptionClass = e.getClass();
		
		if (exceptionClass.equals(InvalidArtikelException.class)) {
			final InvalidArtikelException orig = (InvalidArtikelException) e;
			messages.error(orig.getViolations(), null);
		}
		
		return null;
	}
	
	private String updateErrorMsg(RuntimeException e) {
		final Class<? extends RuntimeException> exceptionClass = e.getClass();
		
		if (exceptionClass.equals(OptimisticLockException.class)) {
			messages.error(ARTIKELVERWALTUNG, MSG_KEY_UPDATE_ARTIKEL_CONCURRENT_UPDATE, null);
			
		}
		else if (exceptionClass.equals(ConcurrentDeletedException.class)) {
			messages.error(ARTIKELVERWALTUNG, MSG_KEY_UPDATE_ARTIKEL_CONCURRENT_DELETE, null);
			
		}
		return null;
	}
	
	@Override
	public String toString() {
		return "ArtikelController [bezeichnung=" + bezeichnung + "]";
	}

	public String getBezeichnung() {
		return bezeichnung;
	}

	public void setBezeichnung(String bezeichnung) {
		this.bezeichnung = bezeichnung;
	}
	
	public Artikel getNeuerArtikel() {
		return neuerArtikel;
	}

	public void setNeuerArtikel(Artikel neuerArtikel) {
		this.neuerArtikel = neuerArtikel;
	}

	public List<Artikel> getLadenhueter() {
		return ladenhueter;
	}

	@Transactional
	public String findArtikelByBezeichnung() {
		artikelNachBez = as.findArtikelByBezeichnung(bezeichnung);

		return JSF_LIST_ARTIKEL;
	}

	public String selectForUpdate(Artikel ausgewaehlterArtikel) {
		artikel = ausgewaehlterArtikel;
		
		return JSF_UPDATE_ARTIKEL;
	}
	
	@TransactionAttribute(REQUIRED)
	@Transactional
	public String updateArtikel() {
		
		if (artikel == null) {
			return JSF_INDEX;
		}
		
		LOGGER.tracef("Aktualisierter Artikel: %s", artikel);
		try {
			artikel = as.updateArtikel(artikel, locale);
		}
		catch (OptimisticLockException | ConcurrentDeletedException e) {
			final String outcome = updateErrorMsg(e);
			return outcome;
		}
		
		updateArtikelEvent.fire(String.valueOf(artikel.getId()));
		
		return JSF_INDEX;
	}
	
	public Artikel getArtikel() {
		return artikel;
	}

	public void setArtikel(Artikel artikel) {
		this.artikel = artikel;
	}

	public List<Artikel> getArtikelNachBez() {
		return artikelNachBez;
	}

	@Transactional
	public void loadLadenhueter() {
		ladenhueter = as.ladenhueter(ANZAHL_LADENHUETER);
	}
	
	@Transactional
	public void createEmptyArtikel() {
		if (neuerArtikel != null) {
			return;
		}

		neuerArtikel = new Artikel();
	}
	
	@TransactionAttribute(REQUIRED)
	@Transactional
	public String createArtikel() {
		try {
			neuerArtikel = as.createArtikel(neuerArtikel, locale);
			}
		catch (InvalidArtikelException e) {
			final String outcome = createArtikelErrorMsg(e);
			return outcome;
		}
		
		return JSF_VIEW_ARTIKEL + JSF_REDIRECT_SUFFIX;
	}
	
	@Transactional
	public String selectArtikel() {
		if (session.getAttribute(SESSION_VERFUEGBARE_ARTIKEL) != null) {
			return JSF_SELECT_ARTIKEL;
		}
		
		final List<Artikel> alleArtikel = as.findVerfuegbareArtikel();
		session.setAttribute(SESSION_VERFUEGBARE_ARTIKEL, alleArtikel);
		return JSF_SELECT_ARTIKEL;
	}
	
	public void geaendert(ValueChangeEvent e) {
		if (geaendertArtikel) {
			return;
		}
		
		if (e.getOldValue() == null) {
			if (e.getNewValue() != null) {
				geaendertArtikel = true;
			}
			return;
		}

		if (!e.getOldValue().equals(e.getNewValue())) {
			geaendertArtikel = true;				
		}
	}
}
