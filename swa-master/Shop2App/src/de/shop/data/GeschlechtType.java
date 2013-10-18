package de.shop.data;

public enum GeschlechtType {
	MAENNLICH(0),
	WEIBLICH(1);
	
	private int value;

	private GeschlechtType(int value) {
		this.value = value;
	}
	
	public static GeschlechtType valueOf(int value) {
		switch(value) {
			case 0:
				return WEIBLICH;
			case 1:
				return MAENNLICH;
			default:
				return null;
		}
	}
	
	
	public static GeschlechtType valueOff(String value) {
		if(value == "MAENNLICH") {
			return MAENNLICH;
		}
		if (value == "WEIBLICH") {
			return WEIBLICH;
		}
		else {
			return null;
		}
	}
	
	public int value() {
		return value;
	}
}
