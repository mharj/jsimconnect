package flightsim.simconnect;

/**
 * The <code>FacilityListType</code> enumeration type is used to 
 * determine which type of facilities data is being requested or 
 * returned.
 * 
 * 
 * @since 0.5
 * @author lc0277
 *
 */
public enum FacilityListType {
	/** Specifies that the type of information is for an airport */
	AIRPORT,
	/**   Specifies that the type of information is for a waypoint */
	WAYPOINT,
	/** Specifies that the type of information is for an NDB */
	NDB,
	/**   Specifies that the type of information is for a VOR */
	VOR,
	/** Not valid as a list type, but simply the number of list types. */
	COUNT;

}
