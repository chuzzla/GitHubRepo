package de.shop.util;

public class InternalError extends RuntimeException {
	private static final long serialVersionUID = 8753585232932802852L;

	public InternalError(Throwable t) {
		super("Ein interner Fehler ist aufgetreten", t);
	}
	
	public InternalError(String msg) {
		super("Ein interner Fehler ist aufgetreten: " + msg);
	}
}
