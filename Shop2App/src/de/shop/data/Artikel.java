package de.shop.data;

import static de.shop.ShopApp.jsonBuilderFactory;

import java.io.Serializable;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class Artikel implements Serializable, JsonMappable {
	private static final long serialVersionUID = -994781068361706752L;

	public Long id;
	public int version;
	public String bezeichnung;
	public Double preis;
	public Boolean verfuegbar;
	
	public Artikel() {
		super();
	}
	
	public Artikel(long id, String bezeichnung, double preis) {
		this.id = id;
		this.bezeichnung = bezeichnung;
		this.preis = preis;
		this.version = 0;
		this.verfuegbar = true;
	}
	
	JsonObjectBuilder getJsonBuilderFactory() {
		return jsonBuilderFactory.createObjectBuilder()
		                         .add("id", id)
		                         .add("version", version)
		                         .add("bezeichnung", bezeichnung)
		                         .add("preis", preis)
		                         .add("verfuegbar", verfuegbar);
	}
	
	@Override
	public JsonObject toJsonObject() {
		return jsonBuilderFactory.createObjectBuilder()
								 .add("id", id)
								 .add("version", version)
								 .add("preis", preis)
								 .add("bezeichnung", bezeichnung)
								 .add("verfuegbar", verfuegbar)
								 .build();
	}

	@Override
	public void fromJsonObject(JsonObject jsonObject) {
		id = Long.valueOf(jsonObject.getJsonNumber("id").longValue());
		version = jsonObject.getInt("version");
		bezeichnung = jsonObject.getString("bezeichnung");
		preis = jsonObject.getJsonNumber("preis").doubleValue();
		verfuegbar = jsonObject.getBoolean("verfuegbar");
	}

	@Override
	public void updateVersion() {
		version ++;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((bezeichnung == null) ? 0 : bezeichnung.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		long temp;
		temp = Double.doubleToLongBits(preis);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (verfuegbar ? 1231 : 1237);
		result = prime * result + version;
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
		Artikel other = (Artikel) obj;
		if (bezeichnung == null) {
			if (other.bezeichnung != null)
				return false;
		} else if (!bezeichnung.equals(other.bezeichnung))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (Double.doubleToLongBits(preis) != Double
				.doubleToLongBits(other.preis))
			return false;
		if (verfuegbar != other.verfuegbar)
			return false;
		if (version != other.version)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Artikel [id=" + id + ", version=" + version + ", bezeichnung="
				+ bezeichnung + ", preis=" + preis + ", verfuegbar="
				+ verfuegbar + "]";
	}

}
