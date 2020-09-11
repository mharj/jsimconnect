package flightsim.simconnect.recv;

/**
 * Enumeration of all known response packet types.
 * 
 * @see RecvPacket#getRecognisedID()
 * @author lc0277
 *
 */
public enum RecvID {
    ID_NULL,
    ID_EXCEPTION,
    ID_OPEN,
    ID_QUIT,
    ID_EVENT,
    ID_EVENT_OBJECT_ADDREMOVE,
    ID_EVENT_FILENAME,
    ID_EVENT_FRAME,
    ID_SIMOBJECT_DATA,
    ID_SIMOBJECT_DATA_BYTYPE,
    ID_WEATHER_OBSERVATION,
    ID_CLOUD_STATE,
    ID_ASSIGNED_OBJECT_ID,
    ID_RESERVED_KEY,
    ID_CUSTOM_ACTION,
    ID_SYSTEM_STATE,
    ID_CLIENT_DATA,
    /** @since 0.5 */
    ID_EVENT_WEATHER_MODE,	
    /** @since 0.5 */
    ID_AIRPORT_LIST,
    /** @since 0.5 */
    ID_VOR_LIST,
    /** @since 0.5 */
    ID_NDB_LIST,
    /** @since 0.5 */
    ID_WAYPOINT_LIST,
    /** @since 0.7 */
    ID_EVENT_MULTIPLAYER_SERVER_STARTED,
    /** @since 0.7 */
    ID_EVENT_MULTIPLAYER_CLIENT_STARTED,
    /** @since 0.7 */
    ID_EVENT_MULTIPLAYER_SESSION_ENDED,
    /** @since 0.7 */
    ID_EVENT_RACE_END,
    /** @since 0.7 */
    ID_EVENT_RACE_LAP;
    
    public static RecvID type(int i) {
    	RecvID[] values = values();
    	if ((i < 0) || (i > values.length)) return ID_NULL;
    	return values[i];
    }
}
