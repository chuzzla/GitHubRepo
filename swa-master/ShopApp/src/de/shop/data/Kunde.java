package de.shop.data;

import static de.shop.ShopApp.jsonBuilderFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import de.shop.util.InternalShopError;

public class Kunde implements JsonMappable, Serializable {
	private static final long serialVersionUID = -7505776004556360014L;
	private static final String DATE_FORMAT = "yyyy-MM-dd";

	public Long id;
	public int version;
	public String nachname;
	public String vorname;
	public GeschlechtType geschlecht;
	public FamilienstandType familienstand;
	public BigDecimal rabatt;
	public short kategorie;
	public String email;
	public Adresse adresse;
	public boolean newsletter;
	public boolean agbAkzeptiert = true;
	public String bestellungenUri;
	

	
	public Kunde() {
	}

	protected JsonObjectBuilder getJsonObjectBuilder() {
		return jsonBuilderFactory.createObjectBuilder()
				                 .add("id", id)
			                     .add("version", version)
			                     .add("nachname", nachname)
			                     .add("vorname", vorname)
			                     .add("geschlecht", geschlecht.toString())
			                     .add("familienstand", familienstand.toString())
			                     .add("rabatt", rabatt)
			                     .add("kategorie", kategorie)
			                     .add("email", email)
			                     .add("adresse", adresse.getJsonBuilderFactory())
			                     .add("newsletter", newsletter)
			                     .add("agbAkzeptiert", agbAkzeptiert)
			                     .add("bestellungenUri", bestellungenUri);
	}
	
	@Override
	public JsonObject toJsonObject() {
		return getJsonObjectBuilder().build();
	}

	public void fromJsonObject(JsonObject jsonObject) {
		id = Long.valueOf(jsonObject.getJsonNumber("id").longValue());
	    version = jsonObject.getInt("version");
		nachname = jsonObject.getString("nachname");
		vorname = jsonObject.getString("vorname");
		geschlecht = GeschlechtType.valueOf(jsonObject.getString("geschlecht"));
		familienstand = FamilienstandType.valueOf(jsonObject.getString("familienstand"));
		rabatt = jsonObject.getJsonNumber("rabatt").bigDecimalValue();
	    kategorie = (short) jsonObject.getInt("kategorie");
		email = jsonObject.getString("email");
		adresse = new Adresse();
		adresse.fromJsonObject(jsonObject.getJsonObject("adresse"));
		newsletter = jsonObject.getBoolean("newsletter");
		agbAkzeptiert = jsonObject.getBoolean("agbAkzeptiert");
		bestellungenUri = jsonObject.getString("bestellungenUri");
	}
	
	@Override
	public void updateVersion() {
		version++;
	}

	@Override
	public String toString() {
		return "Kunde [id=" + id + ", version=" + version + ", nachname="
				+ nachname + ", vorname=" + vorname + ", geschlecht="
				+ geschlecht + ", familienstand=" + familienstand + ", rabatt="
				+ rabatt + ", kategorie=" + kategorie + ", email=" + email
				+ ", adresse=" + adresse + ", newsletter=" + newsletter
				+ ", agbAkzeptiert=" + agbAkzeptiert + ", bestellungenUri="
				+ bestellungenUri + "]";
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
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Kunde other = (Kunde) obj;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		return true;
	}
}