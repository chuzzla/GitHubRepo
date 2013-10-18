package de.shop.kundenverwaltung.domain;

import static de.shop.util.Constants.ERSTE_VERSION;
import static de.shop.util.Constants.KEINE_ID;
import static de.shop.util.Constants.MIN_ID;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.FetchType.EAGER;
import static javax.persistence.TemporalType.TIMESTAMP;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.ScriptAssert;
import org.jboss.logging.Logger;

import de.shop.auth.service.jboss.AuthService.RolleType;
import de.shop.bestellverwaltung.domain.Bestellung;
import de.shop.util.File;
import de.shop.util.IdGroup;

@Entity
@Table(name = "kunde")
@NamedQueries({
		@NamedQuery(name = Kunde.FIND_KUNDEN, query = "SELECT k"
				+ " FROM Kunde k"),
		@NamedQuery(name = Kunde.FIND_KUNDEN_FETCH_BESTELLUNGEN, query = "SELECT  DISTINCT k"
				+ " FROM Kunde k LEFT JOIN FETCH k.bestellungen"),
		@NamedQuery(name = Kunde.FIND_KUNDEN_ORDER_BY_ID, query = "SELECT   k"
				+ " FROM  Kunde k" + " ORDER BY k.id"),
		@NamedQuery(name = Kunde.FIND_IDS_BY_PREFIX, query = "SELECT   k.id"
				+ " FROM  Kunde k" + " WHERE CONCAT('', k.id) LIKE :"
				+ Kunde.PARAM_KUNDE_ID_PREFIX + " ORDER BY k.id"),
		@NamedQuery(name = Kunde.FIND_KUNDEN_BY_ID_PREFIX, query = "SELECT   k"
				+ " FROM  Kunde k" + " WHERE CONCAT('', k.id) LIKE :"
				+ Kunde.PARAM_KUNDE_ID_PREFIX + " ORDER BY k.id"),
		@NamedQuery(name = Kunde.FIND_KUNDE_BY_EMAIL, query = "SELECT DISTINCT k"
				+ " FROM   Kunde k"
				+ " WHERE  k.email = :"
				+ Kunde.PARAM_KUNDE_EMAIL),
		@NamedQuery(name = Kunde.FIND_KUNDEN_BY_NACHNAME, query = "SELECT k"
				+ " FROM   Kunde k" + " WHERE  UPPER(k.nachname) = UPPER(:"
				+ Kunde.PARAM_KUNDE_NACHNAME + ")"),
		@NamedQuery(name = Kunde.FIND_KUNDE_BY_ID_FETCH_BESTELLUNGEN, query = "SELECT DISTINCT k"
				+ " FROM   Kunde k LEFT JOIN FETCH k.bestellungen"
				+ " WHERE  k.id = :" + Kunde.PARAM_KUNDE_ID),
		@NamedQuery(name = Kunde.FIND_NACHNAMEN_BY_PREFIX, query = "SELECT   DISTINCT k.nachname"
				+ " FROM  Kunde k "
				+ " WHERE UPPER(k.nachname) LIKE UPPER(:"
				+ Kunde.PARAM_KUNDE_NACHNAME_PREFIX + ")"),
		@NamedQuery(name = Kunde.FIND_ALL_NACHNAMEN, query = "SELECT      DISTINCT k.nachname"
				+ " FROM     Kunde k" + " ORDER BY k.nachname"),
		@NamedQuery(name = Kunde.FIND_KUNDEN_BY_PLZ, query = "SELECT k"
				+ " FROM  Kunde k" + " WHERE k.adresse.plz = :"
				+ Kunde.PARAM_KUNDE_ADRESSE_PLZ),
		@NamedQuery(name = Kunde.FIND_KUNDEN_BY_NACHNAME_FETCH_BESTELLUNGEN, query = "SELECT DISTINCT k"
				+ " FROM   Kunde k LEFT JOIN FETCH k.bestellungen"
				+ " WHERE  UPPER(k.nachname) = UPPER(:"
				+ Kunde.PARAM_KUNDE_NACHNAME + ")"),
		@NamedQuery(name = Kunde.FIND_KUNDE_BY_BESTELLUNG, query = "SELECT k"
				+ " FROM Kunde k JOIN FETCH k.bestellungen b"
				+ " WHERE b.id = :" + Kunde.PARAM_KUNDE_BESTELLUNG_ID),
		@NamedQuery(name = Kunde.FIND_KUNDE_BY_USERNAME, query = "SELECT   k"
				+ " FROM  Kunde k" + " WHERE CONCAT('', k.id) = :"
				+ Kunde.PARAM_KUNDE_USERNAME),
		@NamedQuery(name = Kunde.FIND_USERNAME_BY_USERNAME_PREFIX, query = "SELECT   CONCAT('', k.id)"
				+ " FROM  Kunde k"
				+ " WHERE CONCAT('', k.id) LIKE :"
				+ Kunde.PARAM_USERNAME_PREFIX) })
@ScriptAssert(lang = "javascript", script = "(_this.password == null && _this.passwordWdh == null)"
		+ "|| (_this.password != null && _this.password.equals(_this.passwordWdh))", 
		message = "{kundenverwaltung.kunde.password.notEqual}", groups = PasswordGroup.class)
@Cacheable
public class Kunde implements java.io.Serializable {
	private static final long serialVersionUID = 8926240073895833886L;
	private static final Logger LOGGER = Logger.getLogger(MethodHandles
			.lookup().lookupClass().getName());

	private static final String NAME_PATTERN = "[A-Z\u00C4\u00D6\u00DC][a-z\u00E4\u00F6\u00FC\u00DF]+";
	public static final String NACHNAME_PATTERN = NAME_PATTERN + "(-"
			+ NAME_PATTERN + ")?";
	public static final int NACHNAME_LENGTH_MIN = 2;
	public static final int NACHNAME_LENGTH_MAX = 32;
	public static final int VORNAME_LENGTH_MAX = 32;
	public static final int EMAIL_LENGTH_MAX = 128;
	public static final int PASSWORD_LENGTH_MAX = 256;
	public static final double UMSATZ_DEFAULT = 0.00;
	public static final float RABATT_DEFAULT = (float) 0.00;

	private static final String PREFIX = "Kunde.";
	public static final String FIND_KUNDEN = PREFIX + "findKunden";
	public static final String FIND_KUNDEN_FETCH_BESTELLUNGEN = PREFIX
			+ "findKundenFetchBestellungen";
	public static final String FIND_IDS_BY_PREFIX = PREFIX
			+ "findIdsByIdPrefix";
	public static final String FIND_KUNDEN_BY_ID_PREFIX = PREFIX
			+ "findKundenByIdPrefix";
	public static final String FIND_KUNDE_BY_EMAIL = PREFIX
			+ "findKundeByEmail";
	public static final String FIND_KUNDEN_BY_NACHNAME = PREFIX
			+ "findKundenByNachname";
	public static final String FIND_KUNDE_BY_ID_FETCH_BESTELLUNGEN = PREFIX
			+ "findKundenByIdFetchBestellungen";
	public static final String FIND_KUNDEN_BY_PLZ = PREFIX + "findKundenByPlz";
	public static final String FIND_KUNDEN_BY_NACHNAME_FETCH_BESTELLUNGEN = PREFIX
			+ "findKundenByNachnameFetchBestellungen";
	public static final String FIND_ALL_NACHNAMEN = PREFIX + "findAllNachnamen";
	public static final String FIND_KUNDE_BY_BESTELLUNG = PREFIX
			+ "findKundeByBestellung";
	public static final String FIND_KUNDEN_ORDER_BY_ID = PREFIX
			+ "findKundenOrderById";
	public static final String FIND_KUNDE_BY_USERNAME = PREFIX
			+ "findKundeByUsername";
	public static final String FIND_USERNAME_BY_USERNAME_PREFIX = PREFIX
			+ "findKundeByUsernamePrefix";
	public static final String FIND_NACHNAMEN_BY_PREFIX = PREFIX
			+ "findNachnamenByPrefix";

	public static final String PARAM_KUNDE_ID = "kundeId";
	public static final String PARAM_KUNDE_ID_PREFIX = "idPrefix";
	public static final String PARAM_KUNDE_EMAIL = "email";
	public static final String PARAM_KUNDE_NACHNAME = "nachname";
	public static final String PARAM_KUNDE_NACHNAME_PREFIX = "nachnamePrefix";
	public static final String PARAM_KUNDE_ADRESSE_PLZ = "plz";
	public static final String PARAM_KUNDE_BESTELLUNG_ID = "bestellungId";
	public static final String PARAM_KUNDE_USERNAME = "username";
	public static final String PARAM_USERNAME_PREFIX = "usernamePrefix";

	@Id
	@GeneratedValue
	@Column(nullable = false, updatable = false, unique = true)
	@Min(value = MIN_ID, message = "{kundenverwaltung.kunde.id.min}", groups = IdGroup.class)
	private Long id = KEINE_ID;

	@Version
	@Basic(optional = false)
	private int version = ERSTE_VERSION;

	@Column(nullable = false)
	private short kategorie;

	@Column(length = NACHNAME_LENGTH_MAX, nullable = false)
	@NotNull(message = "{kundenverwaltung.kunde.nachname.notNull}")
	@Size(min = NACHNAME_LENGTH_MIN, max = NACHNAME_LENGTH_MAX, message = "{kundenverwaltung.kunde.nachname.length}")
	@Pattern(regexp = NACHNAME_PATTERN, message = "{kundenverwaltung.kunde.nachname.pattern}")
	private String nachname;

	@Column(length = VORNAME_LENGTH_MAX)
	@Size(max = VORNAME_LENGTH_MAX, message = "{kundenverwaltung.kunde.vorname.length}")
	private String vorname;

	@Column(name = "familienstand_fk")
	private FamilienstandType familienstand = FamilienstandType.LEDIG;

	@Column(name = "geschlecht_fk")
	private GeschlechtType geschlecht = GeschlechtType.WEIBLICH;

	private boolean newsletter = false;

	@NotNull(message = "{kundenverwaltung.kunde.rabatt.notNull}")
	private float rabatt = RABATT_DEFAULT;

	@Column(length = EMAIL_LENGTH_MAX, nullable = false, unique = true)
	@NotNull(message = "{kundenverwaltung.kunde.email.notNull}")
	@Email(message = "{kundenverwaltung.kunde.email.pattern}")
	private String email;

	@Column(length = PASSWORD_LENGTH_MAX)
	@Size(max = PASSWORD_LENGTH_MAX, message = "{kundenverwaltung.kunde.password.length}")
	private String password;

	@Transient
	@JsonIgnore
	private String passwordWdh;

	@Transient
	@AssertTrue(message = "{kundenverwaltung.kunde.agb}")
	private boolean agbAkzeptiert = true;

	@OneToMany(fetch = EAGER)
	@JoinColumn(name = "kunde_fk", nullable = false)
	@OrderColumn(name = "idx", nullable = false)
	@JsonIgnore
	private List<Bestellung> bestellungen;

	@Transient
	private URI bestellungenUri;

	@ElementCollection(fetch = EAGER)
	@CollectionTable(name = "kunde_rolle", joinColumns = @JoinColumn(name = "kunde_fk", nullable = false), 
						uniqueConstraints = @UniqueConstraint(columnNames = {"kunde_fk", "rolle_fk" }))
	@Column(table = "kunde_rolle", name = "rolle_fk", nullable = false)
	private Set<RolleType> rollen;

	@OneToOne(mappedBy = "kunde", cascade = { PERSIST, REMOVE, MERGE })
	@NotNull(message = "{kundenverwaltung.kunde.adresse.notNull}")
	@Valid
	private Adresse adresse;

	@OneToOne(fetch = EAGER, cascade = { PERSIST, REMOVE })
	@JoinColumn(name = "file_fk")
	@JsonIgnore
	private File file;

	@Transient
	private URI fileUri;

	@Column(nullable = false)
	@Temporal(TIMESTAMP)
	@JsonIgnore
	private Date erzeugt;

	@Column(nullable = false)
	@Temporal(TIMESTAMP)
	@JsonIgnore
	private Date aktualisiert;

	public Kunde() {
		super();
	}

	public Kunde(String nachname, String vorname, String email) {
		super();
		this.nachname = nachname;
		this.vorname = vorname;
		this.email = email;
	}

	@PostPersist
	protected void postPersist() {
		LOGGER.debugf("Neuer Kunde mit ID=%d", id);
	}

	@PostUpdate
	protected void postUpdate() {
		LOGGER.debugf("Kunde mit ID=%d aktualisiert: version=%d", id, version);
	}

	@PrePersist
	private void prePersist() {
		erzeugt = new Date();
		aktualisiert = new Date();
	}

	@PreUpdate
	private void preUpdate() {
		aktualisiert = new Date();
	}

	@PostLoad
	private void postLoad() {
		passwordWdh = password;
	}

	public void setValues(Kunde k) {
		familienstand = k.familienstand;
		geschlecht = k.geschlecht;
		version = k.version;
		nachname = k.nachname;
		vorname = k.vorname;
		email = k.email;
		password = k.password;
		passwordWdh = k.password;
		agbAkzeptiert = k.agbAkzeptiert;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public short getKategorie() {
		return kategorie;
	}

	public void setKategorie(short kategorie) {
		this.kategorie = kategorie;
	}

	public String getNachname() {
		return nachname;
	}

	public void setNachname(String nachname) {
		this.nachname = nachname;
	}

	public String getVorname() {
		return vorname;
	}

	public void setVorname(String vorname) {
		this.vorname = vorname;
	}

	public URI getBestellungenUri() {
		return bestellungenUri;
	}

	public void setBestellungenUri(URI bestellungenUri) {
		this.bestellungenUri = bestellungenUri;
	}

	public Set<RolleType> getRollen() {
		return rollen;
	}

	public void setRollen(Set<RolleType> rollen) {
		this.rollen = rollen;
	}

	public FamilienstandType getFamilienstand() {
		return familienstand;
	}

	public void setFamilienstand(FamilienstandType familienstand) {
		this.familienstand = familienstand;
	}

	public GeschlechtType getGeschlecht() {
		return geschlecht;
	}

	public void setGeschlecht(GeschlechtType geschlecht) {
		this.geschlecht = geschlecht;
	}

	public boolean isNewsletter() {
		return this.newsletter;
	}

	public void setNewsletter(boolean newsletter) {
		this.newsletter = newsletter;
	}

	public float getRabatt() {
		return rabatt;
	}

	public void setRabatt(float rabatt) {
		this.rabatt = rabatt;
	}

	public void setAgbAkzeptiert(boolean agbAkzeptiert) {
		this.agbAkzeptiert = agbAkzeptiert;
	}

	public boolean isAgbAkzeptiert() {
		return agbAkzeptiert;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPasswordWdh() {
		return passwordWdh;
	}

	public void setPasswordWdh(String passwordWdh) {
		this.passwordWdh = passwordWdh;
	}

	public Adresse getAdresse() {
		return adresse;
	}

	public void setAdresse(Adresse adresse) {
		this.adresse = adresse;
	}

	public List<Bestellung> getBestellungen() {
		if (bestellungen == null) {
			return null;
		}

		return Collections.unmodifiableList(bestellungen);
	}

	public void setBestellungen(List<Bestellung> bestellungen) {
		if (this.bestellungen == null) {
			this.bestellungen = bestellungen;
			return;
		}

		this.bestellungen.clear();
		if (bestellungen != null) {
			this.bestellungen.addAll(bestellungen);
		}
	}

	public Kunde addBestellung(Bestellung bestellung) {
		if (bestellungen == null) {
			bestellungen = new ArrayList<>();
		}
		bestellungen.add(bestellung);
		return this;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public URI getFileUri() {
		return fileUri;
	}

	public void setFileUri(URI fileUri) {
		this.fileUri = fileUri;
	}

	public Date getErzeugt() {
		return erzeugt == null ? null : (Date) erzeugt.clone();
	}

	public void setErzeugt(Date erzeugt) {
		this.erzeugt = erzeugt == null ? null : (Date) erzeugt.clone();
	}

	public Date getAktualisiert() {
		return aktualisiert == null ? null : (Date) aktualisiert.clone();
	}

	public void setAktualisiert(Date aktualisiert) {
		this.aktualisiert = aktualisiert == null ? null : (Date) aktualisiert
				.clone();
	}

	@Override
	public String toString() {
		return "Kunde [id=" + id + ", nachname=" + nachname + ", vorname="
				+ vorname + ", email=" + email + ", password=" + password
				+ ", passwordWdh=" + passwordWdh + ", familienstand="
				+ familienstand + ", geschlecht=" + geschlecht + ", erzeugt="
				+ erzeugt + ", aktualisiert=" + aktualisiert + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Kunde other = (Kunde) obj;
		if (email == null) {
			if (other.email != null) {
				return false;
			}
		} 
		else if (!email.equals(other.email)) {
			return false;
		}
		return true;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final Kunde neuesObjekt = (Kunde) super.clone();
		neuesObjekt.id = id;
		neuesObjekt.version = version;
		neuesObjekt.nachname = nachname;
		neuesObjekt.vorname = vorname;
		neuesObjekt.email = email;
		neuesObjekt.newsletter = newsletter;
		neuesObjekt.password = password;
		neuesObjekt.passwordWdh = passwordWdh;
		neuesObjekt.agbAkzeptiert = agbAkzeptiert;
		neuesObjekt.adresse = adresse;
		neuesObjekt.familienstand = familienstand;
		neuesObjekt.geschlecht = geschlecht;
		neuesObjekt.erzeugt = erzeugt;
		neuesObjekt.aktualisiert = aktualisiert;
		return neuesObjekt;
	}
}
