package flightsim.simconnect;

import flightsim.simconnect.data.InitPosition;
import flightsim.simconnect.data.LatLonAlt;
import flightsim.simconnect.data.MarkerState;
import flightsim.simconnect.data.Waypoint;
import flightsim.simconnect.data.XYZ;

/**
 * The <code>DataType</code> enumeration type is used with the
 * {@link SimConnect#addToClientDataDefinition(int, int, int, int)} 
 * call to specify the data type that the server should use to return 
 * the specified data to the client.
 * <br/>
 * This enumeration is just an alias for the {@link SimConnectDataType} enumeration
 *
 * @author lc0277
 * @since 0.5
 *
 */

public enum DataType {
	INVALID,        // invalid data type
	/** Integer */
	INT32,          // 32-bit integer number
	/** Integer */
    INT64,          // 64-bit integer number
    /** Floating point */
    FLOAT32,        // 32-bit floating-point number (float)
    /** Floating point */
    FLOAT64,        // 64-bit floating-point number (double)
    /** Fixed-length string */
    STRING8,        // 8-byte string
    /** Fixed-length string */
    STRING32,       // 32-byte string
    /** Fixed-length string */
    STRING64,       // 64-byte string
    /** Fixed-length string */
    STRING128,      // 128-byte string
    /** Fixed-length string */
    STRING256,      // 256-byte string
    /** Fixed-length string */
    STRING260,      // 260-byte string
    /** Variable-length string */
    STRINGV,        // variable-length string
    
    /** {@link InitPosition} data structure */
    INITPOSITION,   // see INITPOSITION
    /** {@link MarkerState} data structure */
    MARKERSTATE,    // see MARKERSTATE
    /** {@link Waypoint} data structure */
    WAYPOINT,       // see WAYPOINT
    /** {@link LatLonAlt} data structure */
    LATLONALT,      // see LATLONALT
    /** {@link XYZ} data structure */
    XYZ,            // see XYZ

    MAX;             // enum limit
	
	
	/**
	 * Returns the size of this data or data structure, in byte. -1 indicates unknown (variable) size
	 * @since 0.2
	 * @return size in bytes
	 */
	public int size() {
		switch (this) {
		case FLOAT32:
		case INT32:
			return 4;
		case FLOAT64:
		case INT64:
		case STRING8:
			return 8;
		case STRING32:
			return 32;
		case STRING64:
			return 64;
		case STRING128:
			return 128;
		case STRING256:
			return 256;
		case STRING260:
			return 260;
		case INITPOSITION:
			return 56;
		case LATLONALT:
			return 24;
		case WAYPOINT:
			return 48;
		case MARKERSTATE:
			return 68;
		case XYZ:
			return 24;
		case STRINGV:
			return -1;		// indicates unknown length
		default:
			return 0;
		}
	}


}
