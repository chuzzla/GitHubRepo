package de.shop.bestellverwaltung.service;


import static de.shop.util.Constants.KEINE_ID;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
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
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.groups.Default;

import org.jboss.logging.Logger;

import de.shop.artikelverwaltung.domain.Artikel;
import de.shop.bestellverwaltung.domain.Bestellposition;
import de.shop.bestellverwaltung.domain.Bestellung;
import de.shop.bestellverwaltung.domain.Lieferung;
import de.shop.kundenverwaltung.domain.Kunde;
import de.shop.kundenverwaltung.service.KundeService;
import de.shop.util.Log;
import de.shop.util.ValidatorProvider;

@Log
public class BestellungService  implements Serializable {
	private static final long serialVersionUID = -5816249017416603515L;
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());

	
	public enum FetchType {
		NUR_BESTELLUNG,
		MIT_LIEFERUNGEN
	}
	
	@PersistenceContext
	private transient EntityManager em;
	
	@Inject
	private KundeService ks;
	
	@Inject
	private ValidatorProvider validatorProvider;
	
	@Inject
	@NeueBestellung
	private transient Event<Bestellung> event;
	
	@PostConstruct
	private void postConstruct() {
		LOGGER.debugf("CDI-faehiges Bean %s wurde erzeugt", this);
	}
	
	@PreDestroy
	private void preDestroy() {
		LOGGER.debugf("CDI-faehiges Bean %s wird geloescht", this);
	}
	
	
	private void validateBestellung(Bestellung bestellung, Locale locale, Class<?>... groups) {
		final Validator validator = validatorProvider.getValidator(locale);	
		final Set<ConstraintViolation<Bestellung>> violations = validator.validate(bestellung);
		
		if (violations != null && !violations.isEmpty()) {
			throw new InvalidBestellungException(bestellung, violations);
		}
	}

	public Bestellung findBestellungById(Long id, FetchType fetch, Locale locale) {
		Bestellung bestellung = null;
		if (fetch == null || FetchType.NUR_BESTELLUNG.equals(fetch)) {
			bestellung = em.find(Bestellung.class, id);
		}
		else if (FetchType.MIT_LIEFERUNGEN.equals(fetch)) {
			try {
			bestellung = em.createNamedQuery(Bestellung.FIND_BESTELLUNG_BY_ID_FETCH_LIEFERUNGEN, Bestellung.class)
					       .setParameter(Bestellung.PARAM_ID, id)
					       .getSingleResult();
			}
			catch (NoResultException e) {
				return null;
			}
		}
		return bestellung;
	}

	public Kunde findKundeById(Long id) {
		try {
			final Kunde kunde = em.createNamedQuery(Bestellung.FIND_KUNDE_BY_ID, Kunde.class)
					                      .setParameter(Bestellung.PARAM_ID, id)
					                      .getSingleResult();
			return kunde;
		}
		catch (NoResultException e) {
			return null;
		}
	}


	public List<Bestellung> findBestellungenByKunde(Kunde kunde) {
		if (kunde == null) {
			return Collections.emptyList();
		}
		final List<Bestellung> bestellungen = em.createNamedQuery(Bestellung.FIND_BESTELLUNGEN_BY_KUNDE,
				                                                  Bestellung.class)
			                                    .setParameter(Bestellung.PARAM_KUNDE_ID, kunde.getId())
			                                    .getResultList();
		return bestellungen;
	}
	
	public List<Bestellung> findBestellungenMitLieferungenByKunde(Kunde kunde) {
		final List<Bestellung> bestellungen =
				               em.createNamedQuery(Bestellung.FIND_BESTELLUNGEN_BY_KUNDE_ID_FETCH_LIEFERUNGEN,
                                                   Bestellung.class)
                                 .setParameter(Bestellung.PARAM_KUNDE_ID, kunde.getId())
                                 .getResultList();
		return bestellungen;
	}
	
	public List<Bestellung> findBestellungenByArtikel(Artikel artikel) {
		final List<Bestellung> bestellungen = em.createNamedQuery(Bestellung.FIND_BESTELLUNGEN_BY_ARTIKEL_ID, 
																	Bestellung.class)
												.setParameter(Bestellung.PARAM_ARTIKEL_ID, artikel.getId())
												.getResultList();
		return bestellungen;
	}

	public List<Bestellung> findBestellungenByIdFetchLieferungen(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return null;
		}
		
		final CriteriaBuilder builder = em.getCriteriaBuilder();
		final CriteriaQuery<Bestellung> criteriaQuery = builder.createQuery(Bestellung.class);
		final Root<Bestellung> b = criteriaQuery.from(Bestellung.class);
		b.fetch("lieferungen", JoinType.LEFT);
		
		final Path<Long> idPath = b.get("id");
		final List<Predicate> predList = new ArrayList<>();
		for (Long id : ids) {
			final Predicate equal = builder.equal(idPath, id);
			predList.add(equal);
		}
		
		final Predicate[] predArray = new Predicate[predList.size()];
		final Predicate pred = builder.or(predList.toArray(predArray));
		criteriaQuery.where(pred).distinct(true);

		final TypedQuery<Bestellung> query = em.createQuery(criteriaQuery);
		final List<Bestellung> bestellungen = query.getResultList();
		return bestellungen;
	}

	public Bestellung createBestellung(Bestellung bestellung, Long kundeId, Locale locale) {
		if (bestellung == null) {
			return null;
		}

		final Kunde kunde = ks.findKundeById(kundeId, KundeService.FetchType.MIT_BESTELLUNGEN, locale);
		return createBestellung(bestellung, kunde, locale);
	}
	
	public Bestellung createBestellung(Bestellung bestellung,
			                           Kunde kunde,
			                           Locale locale) {
		if (bestellung == null) {
			return null;
		}
		
		if (!em.contains(kunde)) {
			kunde = ks.findKundeById(kunde.getId(), KundeService.FetchType.MIT_BESTELLUNGEN, locale);
		}
		bestellung.setKunde(kunde);
		kunde.addBestellung(bestellung);
		
		bestellung.setId(KEINE_ID);
		for (Bestellposition bp : bestellung.getBestellpositionen()) {
			bp.setId(KEINE_ID);
		}
		
		validateBestellung(bestellung, locale, Default.class);
		em.persist(bestellung);
		event.fire(bestellung);
		
		return bestellung;
	}
	
	public Bestellung updateBestellung(Bestellung bestellung, Locale locale) {
		if (bestellung == null) {
			return null;
		}
		
		final Bestellung vorhandeneBestellung = findBestellungById(bestellung.getId(),
																	FetchType.NUR_BESTELLUNG, locale);
		if (vorhandeneBestellung.getId().longValue() != bestellung.getId().longValue()) {
			throw new BestellungDoesntExistException(bestellung.getId());
		}
		
		validateBestellung(bestellung, locale);
		em.merge(bestellung);
		
		return bestellung;
	}
	
	public List<Lieferung> findLieferungen(String nr) {
		final List<Lieferung> lieferungen =
	              em.createNamedQuery(Lieferung.FIND_LIEFERUNGEN_BY_LIEFERNR_FETCH_BESTELLUNGEN,
	            		              Lieferung.class)
                  .setParameter(Lieferung.PARAM_LIEFERNR, nr)
                  .getResultList();
		return lieferungen;
	}

	
	public Lieferung createLieferung(Lieferung lieferung, List<Bestellung> bestellungen) {
		if (lieferung == null || bestellungen == null || bestellungen.isEmpty()) {
			return null;
		}
		
		final List<Long> ids = new ArrayList<>();
		for (Bestellung b : bestellungen) {
			ids.add(b.getId());
		}
		
		bestellungen = findBestellungenByIdFetchLieferungen(ids);
		lieferung.setBestellungen(bestellungen);
		for (Bestellung bestellung : bestellungen) {
			bestellung.addLieferung(lieferung);
		}
		
		lieferung.setId(KEINE_ID);
		em.persist(lieferung);
		
		return lieferung;
	}
}
