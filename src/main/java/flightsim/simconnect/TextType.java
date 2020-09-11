package flightsim.simconnect;

/**
 * The <code>TextType</code> enumeration type is used to specify
 * which type of text is to be displayed by the {@link SimConnect#text(int, float, int, String)}
 * function.
 * 
 * @since 0.5
 * @author lc0277
 *
 */
public enum TextType {
	/** specify scrolling text in the named color */
	SCROLL_BLACK	(0),
	/** specify scrolling text in the named color */
    SCROLL_WHITE	(1),
	/** specify scrolling text in the named color */
    SCROLL_RED		(2),
	/** specify scrolling text in the named color */
    SCROLL_GREEN	(3),
	/** specify scrolling text in the named color */
    SCROLL_BLUE		(4),
	/** specify scrolling text in the named color */
    SCROLL_YELLOW	(5),
	/** specify scrolling text in the named color */
    SCROLL_MAGENTA	(6),
	/** specify scrolling text in the named color */
    SCROLL_CYAN		(7),
    /** specify static text in the named color */
    PRINT_BLACK		(0x0100),
    /** specify static text in the named color */
    PRINT_WHITE		(0x0101),
    /** specify static text in the named color */
    PRINT_RED		(0x0102),
    /** specify static text in the named color */
    PRINT_GREEN		(0x0103),
    /** specify static text in the named color */
    PRINT_BLUE		(0x0104),
    /** specify static text in the named color */
    PRINT_YELLOW	(0x0105),
    /** specify static text in the named color */
    PRINT_MAGENTA	(0x0106),
    /** specify static text in the named color */
    PRINT_CYAN		(0x0107),
    /** specify that the text is for a menu */
    MENU			(0x0200);
	
	private final int value; 
	
	private TextType(int value) {
		this.value = value;
	}

	/**
	 * Return the numeric value of the constant
	 * @return value of the event
	 */
	public int value() {
		return value;
	}

}
