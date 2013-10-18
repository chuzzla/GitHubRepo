package de.shop.kundenverwaltung.controller;

import static de.shop.util.Constants.JSF_INDEX;
import static de.shop.util.Constants.JSF_REDIRECT_SUFFIX;
import static de.shop.util.Messages.MessagesType.KUNDENVERWALTUNG;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionAttributeType.SUPPORTS;
import static javax.persistence.PersistenceContextType.EXTENDED;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.xml.bind.DatatypeConverter;

import org.jboss.logging.Logger;
import org.richfaces.cdi.push.Push;
import org.richfaces.component.SortOrder;
import org.richfaces.event.FileUploadEvent;
import org.richfaces.model.UploadedFile;

import de.shop.auth.controller.AuthController;
import de.shop.kundenverwaltung.domain.Adresse;
import de.shop.kundenverwaltung.domain.Kunde;
import de.shop.kundenverwaltung.domain.PasswordGroup;
import de.shop.kundenverwaltung.service.EmailExistsException;
import de.shop.kundenverwaltung.service.InvalidKundeException;
import de.shop.kundenverwaltung.service.InvalidNachnameException;
import de.shop.kundenverwaltung.service.KundeDeleteBestellungException;
import de.shop.kundenverwaltung.service.KundeService;
import de.shop.kundenverwaltung.service.KundeService.FetchType;
import de.shop.kundenverwaltung.service.KundeService.OrderByType;
import de.shop.util.AbstractShopException;
import de.shop.util.Client;
import de.shop.util.ConcurrentDeletedException;
import de.shop.util.File;
import de.shop.util.FileHelper;
import de.shop.util.Log;
import de.shop.util.Messages;
import de.shop.util.Transactional;

@Named("kc")
@SessionScoped
@Log
@TransactionAttribute(SUPPORTS)
public class KundeController implements Serializable {
	private static final long serialVersionUID = 6440971162286076765L;
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());
	
	private static final int MAX_AUTOCOMPLETE = 10;
	
	private static final String JSF_KUNDENVERWALTUNG = "/kundenverwaltung/";
	private static final String JSF_VIEW_KUNDE = JSF_KUNDENVERWALTUNG + "viewKunde"; 
	private static final String JSF_LIST_KUNDEN = JSF_KUNDENVERWALTUNG + "listKunden";
	private static final String JSF_UPDATE_KUNDE = JSF_KUNDENVERWALTUNG + "updateKunde";
	private static final String JSF_DELETE_OK = JSF_KUNDENVERWALTUNG + "okDelete";

	private static final String REQUEST_KUNDE_ID = "kundeId";

	
	private static final String MSG_KEY_KUNDE_NOT_FOUND_BY_ID = "viewKunde.notFound";
	private static final String MSG_KEY_KUNDEN_NOT_FOUND_BY_NACHNAME = "listKunden.notFound";
	private static final String MSG_KEY_CREATE_KUNDE_EMAIL_EXISTS = "createPrivatkunde.emailExists";
	private static final String MSG_KEY_DELETE_KUNDE_BESTELLUNG = "viewKunde.deleteKundeBestellung";
	private static final String MSG_KEY_SELECT_DELETE_KUNDE_BESTELLUNG = "listKunden.deleteKundeBestellung";
	private static final String MSG_KEY_UPDATE_KUNDE_DUPLIKAT = "updatePrivatkunde.duplikat";
	private static final String MSG_KEY_UPDATE_KUNDE_CONCURRENT_UPDATE = "updatePrivatkunde.concurrentUpdate";
	private static final String MSG_KEY_UPDATE_KUNDE_CONCURRENT_DELETE = "updatePrivatkunde.concurrentDelete";
	
	private static final String CLIENT_ID_DELETE_BUTTON = "form:deleteButton";
	private static final String CLIENT_ID_KUNDEID = "form:kundeIdInput";
	private static final String CLIENT_ID_CREATE_EMAIL = "createKundeForm:email";
	private static final String CLIENT_ID_KUNDEN_NACHNAME = "form:nachname";
	private static final String CLIENT_ID_UPDATE_PASSWORD = "updateKundeForm:password";
	private static final String CLIENT_ID_UPDATE_EMAIL = "updateKundeForm:email";
	
	private static final Class<?>[] PASSWORD_GROUP = {PasswordGroup.class };

		
	@PersistenceContext(type = EXTENDED)
	private transient EntityManager em;
	
	@Inject
	private transient HttpServletRequest request;
	
	@Inject
	private KundeService ks;
	
	@Inject
	private AuthController auth;
	
	@Inject
	@Client
	private Locale locale;
	
	@Inject
	private Messages messages;
	
	@Inject
	private FileHelper fileHelper;
	
	@Inject
	@Push(topic = "updateKunde")
	private transient Event<String> updateKundeEvent;
	
	private Long kundeId;
	private String nachname;
	private SortOrder vornameSortOrder = SortOrder.unsorted;
	private String vornameFilter = "";
	private boolean geaendertKunde;    // fuer ValueChangeListener

	private Kunde kunde;
	private Kunde neuerKunde;
	private List<Kunde> kunden = Collections.emptyList();
	
	private byte[] bytes;
	private String contentType;

	
	@PostConstruct
	private void postConstruct() {
		LOGGER.debugf("CDI-faehiges Bean %s wurde erzeugt", this);
	}

	@PreDestroy
	private void preDestroy() {
		LOGGER.debugf("CDI-faehiges Bean %s wird geloescht", this);
	}

	private String findKundeByIdErrorMsg(String id) {
		messages.error(KUNDENVERWALTUNG, MSG_KEY_KUNDE_NOT_FOUND_BY_ID, CLIENT_ID_KUNDEID, id);
		return null;
	}
	
	private String createKundeErrorMsg(AbstractShopException e) {
		final Class<? extends AbstractShopException> exceptionClass = e.getClass();
		if (exceptionClass.equals(EmailExistsException.class)) {
			messages.error(KUNDENVERWALTUNG, MSG_KEY_CREATE_KUNDE_EMAIL_EXISTS, CLIENT_ID_CREATE_EMAIL);
		}
		else if (exceptionClass.equals(InvalidKundeException.class)) {
			final InvalidKundeException orig = (InvalidKundeException) e;
			messages.error(orig.getViolations(), null);
		}
		
		return null;
	}
	
	private String updateErrorMsg(RuntimeException e) {
		final Class<? extends RuntimeException> exceptionClass = e.getClass();
		if (exceptionClass.equals(InvalidKundeException.class)) {
			// Ungueltiges Password: Attribute wurden bereits von JSF validiert
			final InvalidKundeException orig = (InvalidKundeException) e;
			final Collection<ConstraintViolation<Kunde>> violations = orig.getViolations();
			messages.error(violations, CLIENT_ID_UPDATE_PASSWORD);
		}
		else if (exceptionClass.equals(EmailExistsException.class)) {
			messages.error(KUNDENVERWALTUNG, MSG_KEY_UPDATE_KUNDE_DUPLIKAT, CLIENT_ID_UPDATE_EMAIL);
		}
		else if (exceptionClass.equals(OptimisticLockException.class)) {
			messages.error(KUNDENVERWALTUNG, MSG_KEY_UPDATE_KUNDE_CONCURRENT_UPDATE, null);
			
		}
		else if (exceptionClass.equals(ConcurrentDeletedException.class)) {
			messages.error(KUNDENVERWALTUNG, MSG_KEY_UPDATE_KUNDE_CONCURRENT_DELETE, null);
			
		}
		return null;
	}

	public Long getKundeId() {
		return kundeId;
	}

	public void setKundeId(Long kundeId) {
		this.kundeId = kundeId;
	}

	public SortOrder getVornameSortOrder() {
		return vornameSortOrder;
	}

	public void setVornameSortOrder(SortOrder vornameSortOrder) {
		this.vornameSortOrder = vornameSortOrder;
	}

	public String getVornameFilter() {
		return vornameFilter;
	}

	public void setVornameFilter(String vornameFilter) {
		this.vornameFilter = vornameFilter;
	}

	public void sortByVorname() {
		vornameSortOrder = vornameSortOrder.equals(SortOrder.ascending)
						   ? SortOrder.descending
						   : SortOrder.ascending;
	} 
	
	public String getNachname() {
		return nachname;
	}
	
	public void setNachname(String nachname) {
		this.nachname = nachname;
	}

	public Kunde getKunde() {
		return kunde;
	}

	public void setKunde(Kunde kunde) {
		this.kunde = kunde;
	}
	
	public Kunde getNeuerKunde() {
		return neuerKunde;
	}

	public void setNeuerKunde(Kunde neuerKunde) {
		this.neuerKunde = neuerKunde;
	}

	public List<Kunde> getKunden() {
		return kunden;
	}
	
	public Date getAktuellesDatum() {
		final Date datum = new Date();
		return datum;
	}
	
	public Class<?>[] getPasswordGroup() {
		return PASSWORD_GROUP.clone();
	}
	
	public String getBase64(File file) {
		return DatatypeConverter.printBase64Binary(file.getBytes());
	}
	
	public String getFilename(File file) {
		if (file == null) {
			return "";
		}
		
		fileHelper.store(file);
		return file.getFilename();
	}

	@TransactionAttribute(REQUIRED)
	public List<Long> findKundenByIdPrefix(String idPrefix) {
		Long id = null;
		try {
			id = Long.valueOf(idPrefix);
		}
		catch (NumberFormatException e) {
			findKundeByIdErrorMsg(idPrefix);
			return null;
		}
		
		final List<Kunde> kundenPrefix = ks.findKundenByIdPrefix(id);
		if (kundenPrefix == null || kundenPrefix.isEmpty()) {
			findKundeByIdErrorMsg(idPrefix);
			return null;
		}
		
		final List<Long> ids = new ArrayList<>();
		for (Kunde k : kundenPrefix) {
			ids.add(k.getId());
		}
		
		if (kundenPrefix.size() > MAX_AUTOCOMPLETE) {
			return ids.subList(0, MAX_AUTOCOMPLETE);
		}
		return ids;
	}
	
	@TransactionAttribute(REQUIRED)
	public List<String> findNachnamenByPrefix(String nachnamePrefix) {
		final List<String> nachnamen = ks.findNachnamenByPrefix(nachnamePrefix);
		if (nachnamen.isEmpty()) {
			messages.error(KUNDENVERWALTUNG, MSG_KEY_KUNDEN_NOT_FOUND_BY_NACHNAME, CLIENT_ID_KUNDEN_NACHNAME, kundeId);
			return nachnamen;
		}

		if (nachnamen.size() > MAX_AUTOCOMPLETE) {
			return nachnamen.subList(0, MAX_AUTOCOMPLETE);
		}
		LOGGER.debugf("Nachname: %s", nachname);
		return nachnamen;
	}
	
	@TransactionAttribute(REQUIRED)
	public String findKundeById() {
		kunde = ks.findKundeById(kundeId, FetchType.NUR_KUNDE, locale);
		if (kunde == null) {
			return findKundeByIdErrorMsg(kundeId.toString());
		}
		kundeId = null;
		return JSF_VIEW_KUNDE;
	}
	
	@TransactionAttribute(REQUIRED)
	public String findKundenByNachname() {
		if (nachname == null || nachname.isEmpty()) {
			kunden = ks.findAllKunden(FetchType.MIT_BESTELLUNGEN, OrderByType.UNORDERED);
			return JSF_LIST_KUNDEN;
		}

		try {
			kunden = ks.findKundenByNachname(nachname, FetchType.MIT_BESTELLUNGEN, locale);
		}
		catch (InvalidNachnameException e) {
			final Collection<ConstraintViolation<Kunde>> violations = e.getViolations();
			messages.error(violations, CLIENT_ID_KUNDEN_NACHNAME);
			return null;
		}
		return JSF_LIST_KUNDEN;
	}

	@TransactionAttribute(REQUIRED)
	public void loadKundeById() {
		// Request-Parameter "kundeId" fuer ID des gesuchten Kunden
		final String idStr = request.getParameter("kundeId");
		Long id;
		try {
			id = Long.valueOf(idStr);
		}
		catch (NumberFormatException e) {
			return;
		}
		
		// Suche durch den Anwendungskern
		kunde = ks.findKundeById(id, FetchType.NUR_KUNDE, locale);
		if (kunde == null) {
			return;
		}
	}
	
	@TransactionAttribute(REQUIRED)
	public String details(Kunde ausgewaehlterKunde) {
		if (ausgewaehlterKunde == null) {
			return null;
		}
		
		// Bestellungen nachladen
		this.kunde = ks.findKundeById(ausgewaehlterKunde.getId(), FetchType.MIT_BESTELLUNGEN, locale);
		this.kundeId = this.kunde.getId();
		
		return JSF_VIEW_KUNDE;
	}
	
	public void uploadListener(FileUploadEvent event) {
		final UploadedFile uploadedFile = event.getUploadedFile();
		contentType = uploadedFile.getContentType();
		bytes = uploadedFile.getData();
	}

	@TransactionAttribute(REQUIRED)
	@Transactional
	public String upload() {
		kunde = ks.findKundeById(kundeId, FetchType.NUR_KUNDE, locale);
		if (kunde == null) {
			return null;
		}
		ks.setFile(kunde, bytes, contentType);

		kundeId = null;
		kunde = null;
		bytes = null;
		contentType = null;

		return JSF_INDEX;
	}
	
	public String resetUpload() {
		kundeId = null;
		kunde = null;
		bytes = null;
		contentType = null;
		
		return JSF_INDEX;
	}
	
	public void createEmptyKunde() {
		if (neuerKunde != null) {
			return;
		}

		neuerKunde = new Kunde();
		final Adresse adresse = new Adresse();
		adresse.setKunde(neuerKunde);
		neuerKunde.setAdresse(adresse);
	}	
	
	@TransactionAttribute(REQUIRED)
	@Transactional
	public String createKunde() {
		try {
			neuerKunde = ks.createKunde(neuerKunde, locale);
		}
		catch (InvalidKundeException | EmailExistsException e) {
			final String outcome = createKundeErrorMsg(e);
			return outcome;
		}
		
		// Aufbereitung fuer viewKunde.xhtml
		kundeId = neuerKunde.getId();
		kunde = neuerKunde;
		neuerKunde = null;  // zuruecksetzen
		
		return JSF_VIEW_KUNDE + JSF_REDIRECT_SUFFIX;
	}
	
	public String selectForUpdate(Kunde ausgewaehlterKunde) {
		if (ausgewaehlterKunde == null) {
			return null;
		}
		
		kunde = ausgewaehlterKunde;
		
		return  JSF_UPDATE_KUNDE;
	}
	
	@TransactionAttribute(REQUIRED)
	@Transactional
	public String delete(Kunde ausgewaehlterKunde) {
		try {
			ks.deleteKunde(ausgewaehlterKunde);
		}
		catch (KundeDeleteBestellungException e) {
			messages.error(KUNDENVERWALTUNG, MSG_KEY_SELECT_DELETE_KUNDE_BESTELLUNG, null,
					       e.getKundeId(), e.getAnzahlBestellungen());
			return null;
		}

		kunden.remove(ausgewaehlterKunde);
		return null;
	}
	
	public void geaendert(ValueChangeEvent e) {
		if (geaendertKunde) {
			return;
		}
		
		if (e.getOldValue() == null) {
			if (e.getNewValue() != null) {
				geaendertKunde = true;
			}
			return;
		}

		if (!e.getOldValue().equals(e.getNewValue())) {
			geaendertKunde = true;				
		}
	}
	
	@TransactionAttribute(REQUIRED)
	@Transactional
	public String update() {
		auth.preserveLogin();
		
		if (!geaendertKunde || kunde == null) {
			return JSF_INDEX;
		}
				
		LOGGER.tracef("Aktualisierter Kunde: %s", kunde);
		try {
			kunde = ks.updateKunde(kunde, locale, false);
		}
		catch (EmailExistsException | InvalidKundeException
			  | OptimisticLockException | ConcurrentDeletedException e) {
			final String outcome = updateErrorMsg(e);
			return outcome;
		}

		// Push-Event fuer Webbrowser
		updateKundeEvent.fire(String.valueOf(kunde.getId()));
		
		// ValueChangeListener zuruecksetzen
		geaendertKunde = false;
		
		// Aufbereitung fuer viewKunde.xhtml
		kundeId = kunde.getId();
		
		return JSF_VIEW_KUNDE + JSF_REDIRECT_SUFFIX;
	}
	
	@TransactionAttribute(REQUIRED)
	@Transactional
	public String deleteAngezeigtenKunden() {
		if (kunde == null) {
			return null;
		}
		
		LOGGER.trace(kunde);
		try {
			ks.deleteKunde(kunde);
		}
		catch (KundeDeleteBestellungException e) {
			messages.error(KUNDENVERWALTUNG, MSG_KEY_DELETE_KUNDE_BESTELLUNG, CLIENT_ID_DELETE_BUTTON,
					       e.getKundeId(), e.getAnzahlBestellungen());
			return null;
		}
		
		// Aufbereitung fuer ok.xhtml
		request.setAttribute(REQUEST_KUNDE_ID, kunde.getId());
		
		// Zuruecksetzen
		kunde = null;
		kundeId = null;

		return JSF_DELETE_OK;
	}
}
