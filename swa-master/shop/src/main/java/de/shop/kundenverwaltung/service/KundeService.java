package de.shop.kundenverwaltung.service;

import static de.shop.util.Constants.KEINE_ID;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.groups.Default;

import org.jboss.logging.Logger;

import com.google.common.base.Strings;

import de.shop.auth.service.jboss.AuthService;
import de.shop.bestellverwaltung.domain.Bestellposition;
import de.shop.bestellverwaltung.domain.Bestellung;
import de.shop.bestellverwaltung.domain.Bestellposition_;
import de.shop.bestellverwaltung.domain.Bestellung_;
import de.shop.kundenverwaltung.domain.Kunde;
import de.shop.kundenverwaltung.domain.Kunde_;
import de.shop.util.ConcurrentDeletedException;
import de.shop.util.File;
import de.shop.util.FileHelper;
import de.shop.util.FileHelper.MimeType;
import de.shop.util.IdGroup;
import de.shop.util.Log;
import de.shop.util.NoMimeTypeException;
import de.shop.util.ValidatorProvider;

@Log
public class KundeService implements Serializable {
	private static final long serialVersionUID = 3692819050477194655L;
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());
	
	public enum FetchType {
		NUR_KUNDE,
		MIT_BESTELLUNGEN,
		MIT_BESTELLUNGEN_UND_LIEFERUNGEN
	}
	
	public enum OrderByType {
		UNORDERED,
		ID
	}
	
	@PersistenceContext
	private transient EntityManager em;
	
	@Inject
	private ValidatorProvider validatorProvider;
	
	@Inject
	private AuthService authService;
	
	@Inject
	private FileHelper fileHelper;
	
	@Inject
	@NeuerKunde
	private transient Event<Kunde> event;
	
	@PostConstruct
	private void postConstruct() {
		LOGGER.debugf("CDI-faehiges Bean %s wurde erzeugt", this);
	}
	
	@PreDestroy
	private void preDestroy() {
		LOGGER.debugf("CDI-faehiges Bean %s wird geloescht", this);
	}
	
	private void validateKunde(Kunde kunde, Locale locale, Class<?>... groups) {
		final Validator validator = validatorProvider.getValidator(locale);
		final Set<ConstraintViolation<Kunde>> violations =
			                                          validator.validate(kunde, groups);
		
		if (!violations.isEmpty()) {
			throw new InvalidKundeException(kunde, violations);
		}
	}

	private void validateKundeId(Long id, Locale locale) {
		final Validator validator = validatorProvider.getValidator(locale);
		final Set<ConstraintViolation<Kunde>> violations = validator.validateValue(Kunde.class,
				                                                                           "id",
				                                                                           id,
				                                                                           Default.class);
		if (!violations.isEmpty())
			throw new InvalidKundeIdException(id, violations);
	}

	private void validateNachname(String nachname, Locale locale) {
		final Validator validator = validatorProvider.getValidator(locale);
		final Set<ConstraintViolation<Kunde>> violations = validator.validateValue(Kunde.class,
				                                                                           "nachname",
				                                                                           nachname,
				                                                                           Default.class);
		if (!violations.isEmpty())
			throw new InvalidNachnameException(nachname, violations);
	}

	private void validateEmail(String email, Locale locale) {
		final Validator validator = validatorProvider.getValidator(locale);
		final Set<ConstraintViolation<Kunde>> violations = validator.validateValue(Kunde.class,
				                                                                           "email",
				                                                                           email,
				                                                                           Default.class);
		if (!violations.isEmpty())
			throw new InvalidEmailException(email, violations);
	}

	private void setFile(Kunde kunde, byte[] bytes, MimeType mimeType) {
		if (mimeType == null) {
			throw new NoMimeTypeException();
		}
		
		final String filename = fileHelper.getFilename(kunde.getClass(), kunde.getId(), mimeType);
		
		// Gibt es noch kein (Multimedia-) File
		File file = kunde.getFile();
		if (file == null) {
			file = new File(bytes, filename, mimeType);
			kunde.setFile(file);
			em.persist(file);
		}
		else {
			file.set(bytes, filename, mimeType);
			em.merge(file);
		}
	}

	private void passwordVerschluesseln(Kunde kunde) {
		LOGGER.debugf("passwordVerschluesseln BEGINN: %s", kunde);
	
		final String unverschluesselt = kunde.getPassword();
		final String verschluesselt = authService.verschluesseln(unverschluesselt);
		kunde.setPassword(verschluesselt);
		kunde.setPasswordWdh(verschluesselt);
	
		LOGGER.debugf("passwordVerschluesseln ENDE: %s", verschluesselt);
	}

	private boolean hasBestellungen(Kunde kunde) {
		LOGGER.debugf("hasBestellungen BEGINN: %s", kunde);
		
		boolean result = false;
		
		// Gibt es den Kunden und hat er mehr als eine Bestellung?
		// Bestellungen nachladen wegen Hibernate-Caching
		if (kunde != null && kunde.getBestellungen() != null && !kunde.getBestellungen().isEmpty()) {
			result = true;
		}
		
		LOGGER.debugf("hasBestellungen ENDE: %s", result);
		return result;
	}

	public void setFile(Kunde kunde, byte[] bytes, String mimeTypeStr) {
		final MimeType mimeType = MimeType.get(mimeTypeStr);
		setFile(kunde, bytes, mimeType);
	}

	public void setFile(Long kundeId, byte[] bytes, Locale locale) {
		final Kunde kunde = findKundeById(kundeId, FetchType.NUR_KUNDE, locale);
		if (kunde == null) {
			return;
		}
		final MimeType mimeType = fileHelper.getMimeType(bytes);
		setFile(kunde, bytes, mimeType);
	}

	public List<Kunde> findAllKunden(FetchType fetch, OrderByType order) {
		List<Kunde> kunden;
		switch (fetch) {
			case NUR_KUNDE:
				kunden = OrderByType.ID.equals(order)
				        ? em.createNamedQuery(Kunde.FIND_KUNDEN_ORDER_BY_ID, Kunde.class)
				        	.getResultList()
				        : em.createNamedQuery(Kunde.FIND_KUNDEN, Kunde.class)
				            .getResultList();
				break;
			
			case MIT_BESTELLUNGEN:
				kunden = em.createNamedQuery(Kunde.FIND_KUNDEN_FETCH_BESTELLUNGEN, Kunde.class)
						   .getResultList();
				break;

			default:
				kunden = OrderByType.ID.equals(order)
		                ? em.createNamedQuery(Kunde.FIND_KUNDEN_ORDER_BY_ID, Kunde.class)
		                	.getResultList()
		                : em.createNamedQuery(Kunde.FIND_KUNDEN, Kunde.class)
		                    .getResultList();
				break;
		}
		
		return kunden;
	}
	
	public Kunde findKundeById(Long id, FetchType fetch, Locale locale) {
		validateKundeId(id, locale);
		
		Kunde kunde = null;
		
		try {
			switch (fetch) {
				case NUR_KUNDE:
					kunde = em.find(Kunde.class, id);
					break;
				
				case MIT_BESTELLUNGEN:
					kunde = em.createNamedQuery(Kunde.FIND_KUNDE_BY_ID_FETCH_BESTELLUNGEN, Kunde.class)
					          .setParameter(Kunde.PARAM_KUNDE_ID, id)
							  .getSingleResult();
					break;
				
				default:
					kunde = em.find(Kunde.class, id);
					break;
			}
		}
		catch (NoResultException e) {
			kunde = null;
		}
		return kunde;
	}
	
	public List<Long> findIdsByPrefix(String idPrefix) {
		if (Strings.isNullOrEmpty(idPrefix)) {
			return Collections.emptyList();
		}
		final List<Long> ids = em.createNamedQuery(Kunde.FIND_IDS_BY_PREFIX, Long.class)
				                 .setParameter(Kunde.PARAM_KUNDE_ID_PREFIX, idPrefix + '%')
				                 .getResultList();
		return ids;
	}
	
	public List<Kunde> findKundenByIdPrefix(Long id) {
		if (id == null) {
			return Collections.emptyList();
		}
		
		final List<Kunde> kunden = em.createNamedQuery(Kunde.FIND_KUNDEN_BY_ID_PREFIX,
				                                              Kunde.class)
				                             .setParameter(Kunde.PARAM_KUNDE_ID_PREFIX, id.toString() + '%')
				                             .getResultList();
		return kunden;
	}
	
	public List<Kunde> findKundenByNachname(String nachname, FetchType fetch, Locale locale) {
		validateNachname(nachname, locale);
		List<Kunde> kunden;
		switch (fetch) {
			case NUR_KUNDE:
				kunden = em.createNamedQuery(Kunde.FIND_KUNDEN_BY_NACHNAME, Kunde.class)
						   .setParameter(Kunde.PARAM_KUNDE_NACHNAME, nachname)
                           .getResultList();
				break;
			
			case MIT_BESTELLUNGEN:
				kunden = em.createNamedQuery(Kunde.FIND_KUNDEN_BY_NACHNAME_FETCH_BESTELLUNGEN,
						                     Kunde.class)
						   .setParameter(Kunde.PARAM_KUNDE_NACHNAME, nachname)
                           .getResultList();
				break;

			default:
				kunden = em.createNamedQuery(Kunde.FIND_KUNDEN_BY_NACHNAME, Kunde.class)
						   .setParameter(Kunde.PARAM_KUNDE_NACHNAME, nachname)
                           .getResultList();
				break;
		}

		return kunden;
	}
	
	public List<String> findNachnamenByPrefix(String nachnamePrefix) {
		final List<String> nachnamen = em.createNamedQuery(Kunde.FIND_NACHNAMEN_BY_PREFIX, String.class)
				                         .setParameter(Kunde.PARAM_KUNDE_NACHNAME_PREFIX, nachnamePrefix + '%')
				                         .getResultList();
		return nachnamen;
	}
	
	public Kunde findKundeByEmail(String email, Locale locale) {
		validateEmail(email, locale);
		Kunde kunde;
		try {
			kunde = em.createNamedQuery(Kunde.FIND_KUNDE_BY_EMAIL, Kunde.class)
					  .setParameter(Kunde.PARAM_KUNDE_EMAIL, email)
					  .getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
		
		return kunde;
	}
	
	public List<Kunde> findKundenByPLZ(String plz) {
		final List<Kunde> kunden = em.createNamedQuery(Kunde.FIND_KUNDEN_BY_PLZ, Kunde.class)
				                             .setParameter(Kunde.PARAM_KUNDE_ADRESSE_PLZ, plz)
				                             .getResultList();
		return kunden;
	}
	
	public Kunde findKundeByUserName(String userName) {
		Kunde kunde;
		try {
			kunde = em.createNamedQuery(Kunde.FIND_KUNDE_BY_USERNAME, Kunde.class)
					  .setParameter(Kunde.PARAM_KUNDE_USERNAME, userName)
					  .getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
		
		return kunde;
	}
	
	public List<Kunde> findKundenByNachnameCriteria(String nachname) {
		// SELECT k
		// FROM   AbstractKunde k
		// WHERE  k.nachname = ?
				
		final CriteriaBuilder builder = em.getCriteriaBuilder();
		final CriteriaQuery<Kunde> criteriaQuery = builder.createQuery(Kunde.class);
		final Root<Kunde> k = criteriaQuery.from(Kunde.class);

		final Path<String> nachnamePath = k.get(Kunde_.nachname);
		
		final Predicate pred = builder.equal(nachnamePath, nachname);
		criteriaQuery.where(pred);

		final List<Kunde> kunden = em.createQuery(criteriaQuery).getResultList();
		return kunden;
	}
	
	public List<Kunde> findKundenMitMinBestMenge(short minMenge) {
		// SELECT DISTINCT k
		// FROM   AbstractKunde k
		//        JOIN k.bestellungen b
		//        JOIN b.bestellpositionen bp
		// WHERE  bp.anzahl >= ?
		
		final CriteriaBuilder builder = em.getCriteriaBuilder();
		final CriteriaQuery<Kunde> criteriaQuery  = builder.createQuery(Kunde.class);
		final Root<Kunde> k = criteriaQuery.from(Kunde.class);

		final Join<Kunde, Bestellung> b = k.join(Kunde_.bestellungen);
		final Join<Bestellung, Bestellposition> bp = b.join(Bestellung_.bestellpositionen);
		criteriaQuery.where(builder.gt(bp.<Short>get(Bestellposition_.anzahl), minMenge))
		             .distinct(true);
		
		final List<Kunde> kunden = em.createQuery(criteriaQuery).getResultList();
		return kunden;
	}
	
	public Kunde createKunde(Kunde kunde, Locale locale) {
		if (kunde == null) {
			return kunde;
		}
		
		validateKunde(kunde, locale, Default.class);
		
		final Kunde vorhandenerKunde = findKundeByEmail(kunde.getEmail(), locale);
		
		if (vorhandenerKunde != null) {
			throw new EmailExistsException(kunde.getEmail());
		}
		
		passwordVerschluesseln(kunde);
		kunde.setId(KEINE_ID);
		em.persist(kunde);
		event.fire(kunde);

		return kunde;
	}
	
	public Kunde updateKunde(Kunde kunde, Locale locale,
            boolean geaendertPassword) {
		if (kunde == null) {
			return null;
		}

		validateKunde(kunde, locale, Default.class, IdGroup.class);

		em.detach(kunde);

		Kunde tmp = findKundeById(kunde.getId(), FetchType.NUR_KUNDE, locale);
		if (tmp == null) {
			throw new ConcurrentDeletedException(kunde.getId());
		}
		em.detach(tmp);

		tmp = findKundeByEmail(kunde.getEmail(), locale);
		if (tmp != null) {
			em.detach(tmp);
			if (tmp.getId().longValue() != kunde.getId().longValue()) {
				throw new EmailExistsException(kunde.getEmail());
			}
		}

	
		if (geaendertPassword) {
			passwordVerschluesseln(kunde);
		}

		kunde = em.merge(kunde);   
		kunde.setPasswordWdh(kunde.getPassword());

		return kunde;
	}
	
	public void deleteKunde(Kunde kunde) {
		if (kunde == null) {
			return;
		}

		deleteKundeById(kunde.getId());
	}
	
	public void deleteKundeById(Long kundeId) {
		Kunde kunde;
		try {
			kunde = findKundeById(kundeId, FetchType.MIT_BESTELLUNGEN, Locale.getDefault());
		}
		catch (InvalidKundeIdException e) {
			return;
		}
		if (kunde == null) {
			return;
		}

		final boolean hasBestellungen = hasBestellungen(kunde);
		if (hasBestellungen) {
			throw new KundeDeleteBestellungException(kunde);
		}

		em.remove(kunde);
	}
}
