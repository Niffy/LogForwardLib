package com.niffy.logforwarder;

import java.util.HashMap;
import java.util.Map;


public enum Flag {
	CLIENT_ERROR(0),
	CLIENT_SHUTDOWN(1),
	CLIENT_DISCONNECT(2),
	CLIENT_CONNECTED(3);
	

	private int mNumber;

	private Flag(int pNumber) {
		this.mNumber = pNumber;
	}

	public int getNumber() {
		return this.mNumber;
	}
	
	private static final Map<Integer, Flag> lookup = new HashMap<Integer, Flag>();
	static {
		for (Flag h : Flag.values())
			lookup.put(h.getNumber(), h);
	}

	/**
	 * Finds a Difficulty type that is related to an ID, e.g ID 2 =
	 * {@link #BUILD}
	 * 
	 * @param pDifficulty
	 *            {@link Integer} The ID you wish to know the Difficulty of.
	 * @return {@link Difficulty} The Difficulty of the given ID. or NULL if the
	 *         Difficulty does not exist.
	 * @throws @link {@link GenericClassCastException} When a given object (ID)
	 *         is of the incorrect type and when if the given object (ID) is
	 *         null;
	 */
	public static Flag get(int pDifficulty) throws GenericClassCastException {
		Flag value = null;
		try {
			value = lookup.get(pDifficulty);
		} catch (ClassCastException CEE) {
			// Debug.e(CEE);
			throw new GenericClassCastException("Flag: Given value is incorect object, should of type INT", CEE);
		} catch (NullPointerException NPE) {
			// Debug.e(NPE);
			throw new GenericClassCastException("Flag: Given value is NULL,", NPE);
		}
		return value;
	}
}
