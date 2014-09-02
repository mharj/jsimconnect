package flightsim.simconnect;

/**
 * The <code>SimConnectPeriod</code> enumeration type is used with the 
 * {@link SimConnect#requestDataOnSimObject(int, int, int, SimConnectPeriod)}
 * call to specify how often data is to be sent to the client.
 * 
 * <p>
 * <pre>
 * $Id: SimConnectPeriod.java,v 1.3 2007-05-16 19:59:36 lc Exp $
 * </pre>
 * </p>
 * 
 * @see SimConnect
 * @author lc0277
 *
 */
public enum SimConnectPeriod {
   
	/** Specifies that the data is not to be sent. */
	NEVER,
	/** Specifies that the data should be sent once only. Note that this is not an efficient way of receiving 
	 * data frequently, use one of the other periods if there is a regular frequency to the data request. */
    ONCE,
    /** Specifies that the data should be sent every visual (rendered) frame. */
    VISUAL_FRAME,
    /** Specifies that the data should be sent every simulated frame, whether that frame is rendered or not. */
    SIM_FRAME,
    /** Specifies that the data should be sent once every second. */
    SECOND,
}
