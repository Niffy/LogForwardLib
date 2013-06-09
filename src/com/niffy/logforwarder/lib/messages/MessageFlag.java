package com.niffy.logforwarder.lib.messages;

import java.util.HashMap;
import java.util.Map;

import com.niffy.logforwarder.lib.GenericClassCastException;

public enum MessageFlag {
	SEND_REQUEST(0),
	DELETE_REQUEST(1),
	SEND_SIZE_RESPONSE(2),
	SEND_SIZE_ACK(3),
	SEND_DATA_FILE(4),
	DELETE_RESPONSE(5),
	SHUT_DOWN_SERVICE(888),
	ERROR(999);
	

	private int mNumber;

	private MessageFlag(int pNumber) {
		this.mNumber = pNumber;
	}

	public int getNumber() {
		return this.mNumber;
	}

	private static final Map<Integer, MessageFlag> lookup = new HashMap<Integer, MessageFlag>();
	static {
		for (MessageFlag h : MessageFlag.values())
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
	public static MessageFlag get(int pDifficulty) throws GenericClassCastException {
		MessageFlag value = null;
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
