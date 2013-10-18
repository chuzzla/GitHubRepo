package de.shop.data;

import java.io.Serializable;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import de.shop.service.ArtikelService;
import static de.shop.ShopApp.jsonBuilderFactory;

public class Bestellposition implements JsonMappable, Serializable{
	private static final long serialVersionUID = 7571183193087866154L;
	
	public Long id;
	public int version;
	public int anzahl;
	public String artikelUri;
	
	public Bestellposition() {
		super();
	}
	
	public Bestellposition(long id, int anzahl) {
		this.id = id;
		this.anzahl = anzahl;		
	}
	
	JsonObjectBuilder getJsonBuilderFactory() {
		return jsonBuilderFactory.createObjectBuilder()
								 .add("id", id)
								 .add("version", version)
								 .add("anzahl", anzahl);
	}

	@Override
	public JsonObject toJsonObject() {
		return jsonBuilderFactory.createObjectBuilder()
								 .add("id", id)
								 .add("version", version)
								 .add("anzahl", anzahl)
								 .build();
	}
	
	@Override
	public void fromJsonObject(JsonObject jsonObject) {
		id = Long.valueOf(jsonObject.getJsonNumber("id").longValue());
		version = jsonObject.getInt("version");
		anzahl = jsonObject.getInt("anzahl");
		artikelUri = jsonObject.getString("artikelUri");
	}
	
	public void updateVersion() {
		version++;
	}

	@Override
	public String toString() {
		return "Bestellposition [id=" + id + ", version=" + version
				+ ", anzahl=" + anzahl + "]";
	};
}
