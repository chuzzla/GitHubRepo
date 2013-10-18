package de.shop.data;

import static de.shop.ShopApp.jsonBuilderFactory;

import java.util.Collection;
import java.util.HashSet;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import android.util.Log;


public class Privatkunde extends AbstractKunde {
	private static final long serialVersionUID = -3018823336715723505L;

	public GeschlechtType geschlecht;
	public FamilienstandType familienstand;

	@Override
	protected JsonObjectBuilder getJsonObjectBuilder() {
		final JsonObjectBuilder jsonObjectBuilder = super.getJsonObjectBuilder()
				                                         .add("geschlecht", geschlecht.toString())
			                                             .add("familienstand", familienstand.toString());
		
		return jsonObjectBuilder;
	}
	
	@Override
	public void fromJsonObject(JsonObject jsonObject) {
		super.fromJsonObject(jsonObject);		
		
		try {
			geschlecht = GeschlechtType.valueOf(jsonObject.getString("geschlecht"));
			familienstand = FamilienstandType.valueOf(jsonObject.getString("familienstand"));	
		}
		catch(ClassCastException e) {
			geschlecht = null;
			familienstand = null;
		}
	}
	
	@Override
	public String toString() {
		return "Privatkunde [" + super.toString() + ", geschlecht=" + geschlecht
				+ ", familienstand=" + familienstand + "]";
	}
}
