package flightsim.simconnect;

import flightsim.simconnect.recv.RecvEvent;

/**
 * The <code>TextResult</code> enumeration type is used to specify which
 * event has occurred as a result of a call to {@link SimConnect#text(int, float, int, String)}
 * or {@link SimConnect#menu(float, int, String, String, String[])}.
 * 
 *
 * @since 0.5
 * @author lc0277
 *
 */
public enum TextResult {
	
	/**   Specifies that the user has selected the menu item. */
	MENU_SELECT_1	(0),
	/**   Specifies that the user has selected the menu item. */
    MENU_SELECT_2	(1),
	/**   Specifies that the user has selected the menu item. */
    MENU_SELECT_3	(2),
	/**   Specifies that the user has selected the menu item. */
    MENU_SELECT_4	(3),
	/**   Specifies that the user has selected the menu item. */
    MENU_SELECT_5	(4),
	/**   Specifies that the user has selected the menu item. */
    MENU_SELECT_6	(5),
	/**   Specifies that the user has selected the menu item. */
    MENU_SELECT_7	(6),
	/**   Specifies that the user has selected the menu item. */
    MENU_SELECT_8	(7),
	/**   Specifies that the user has selected the menu item. */
    MENU_SELECT_9	(8),
	/**   Specifies that the user has selected the menu item. */
    MENU_SELECT_10	(9),
    /**   Specifies that the menu or text identified by the EventID is now on display. */
    DISPLAYED 		(0x00010000),
    /**   Specifies that the menu or text identified by the EventID is waiting in a queue. */
    QUEUED			(0x00010001),
    /**   Specifies that the menu or text identified by the EventID has been removed from the queue. */
    REMOVED			(0x00010002),
    /**    Specifies that the menu or text identified by the EventID has been replaced in the queue. */
    REPLACED		(0x00010003),
    /**   Specifies that the menu or text identified by the EventID has timed-out and is no longer on display. */
    TIMEOUT			(0x00010004);

	private final int value; 
	
	private TextResult(int value) {
		this.value = value;
	}

	/**
	 * Returns true if the text result is a selection item (not a
	 * display event)
	 * @return true if the text result is a selection item
	 * @since 0.7
	 */
	public boolean isSelection() {
		return value < 0x100;
	}
	
	/**
	 * Return the numeric value of the constant
	 * @return value of the event
	 */
	public int value() {
		return value;
	}
	
	/**
	 * Returns a member of the TextResult enumeration given by its value
	 * @param val value
	 * @return member
	 */
	public static TextResult type(int val) {
		TextResult[] values = values();
		for (TextResult tr : values) {
			if (tr.value == val) 
				return tr;
		}
		return null;
	}

	/**
	 * Returns a member of the TextResult enumeration given by its value
	 * @param ev Event structure
	 * @return member
	 */
	public static TextResult type(RecvEvent ev) {
		return type(ev.getData());
	}

}
