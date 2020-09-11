package flightsim.simconnect;


/**
 * The <code>WeatherMode</code> enumeration type is used to return the
 * current weather mode, when subscribed to the <b>WeatherModeChanged</b>
 * Event. 
 *
 * @since 0.5
 * @author lc0277
 *
 */
public enum WeatherMode {
	/**   Specifies that the weather has been set to a theme. */
	THEME,
	/**   Specifies that real-world weather has been set. */
    RWW,
    /**   Specifies that custom weather has been set.  */
    CUSTOM,
    /**   Specifies that the global weather mode has been set. */
    GLOBAL;
    
    
    /**
     * Returns a enumeration member given by its index. On failure (index
     * parameter does not corresponds to a valid enumeration 
     * member), returns a default value ({@link #THEME})
     * @param i index of enumeration member
     * @return enumeration member
     */
    public static WeatherMode type(int i) {
    	WeatherMode[] values = values();
    	if ((i < 0) || (i > values.length)) return THEME;
    	return values[i];
    }

}
