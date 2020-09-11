package flightsim.simconnect;

/**
 * The <code>SimObjectType</code> enumeration type is used with the 
 * {@link SimConnect#requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)} 
 * call to request information on specific or nearby objects.
 * 
 * @author lc0277
 *
 */
public enum SimObjectType {
	/** Specifies the user's aircraft. */
	USER,
	/** Specifies all AI controlled objects. */
    ALL,
    /** Specifies all aircraft. */
    AIRCRAFT,
    /** Specifies all helicopters. */
    HELICOPTER,
    /** Specifies all AI controlled boats. */
    BOAT,
    /** Specifies all AI controlled ground vehicles */
    GROUND,
    
    /** for invalid values */
	INVALID;		
	
	private static final String[] NAMES = { 
		"User", 		"All", 		"Airplane", 
		"Helicopter", 	"Boat", 	"GroundVehicle" };
	
   public static final SimObjectType type(int i) {
	   SimObjectType[] values = values();
	   if ((i > values.length) || (i < 0)) return SimObjectType.ALL;	// default
	   else return values[i];
   }

   /**
    * Returns the member of the SimObjectType enumeration corresponding to a
    * string. Recognized string are those returned by the CATEGORY
    * simulation variable.
    * <br/>
    * Returns <code>INVALID</code> if unknown
    * 
    * @since 0.4
    * @param s string representation
    * @return enumeration member
    */
   public static final SimObjectType type(String s) {
	   SimObjectType[] values = values();
	   for (int i = 0; i < NAMES.length; i++) {
		   if (NAMES[i].equals(s)) return values[i];
	   }
	   return INVALID;
   }
   
   @Override
   public String toString() {
	   return NAMES[ordinal()];
   }
   

}
