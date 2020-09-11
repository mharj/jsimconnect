package flightsim.simconnect;

/**
 * The <code>ClientDataPeriod</code> enumeration type is used with the {@link SimConnect#requestClientData(int, int, int, int, int)} 
 * call to specify how often data is to be sent to the client.
 * <br/>
 * <b>Note: </b>Although the period definitions are specific, data is always transmitted at the end of a frame,
 * so even if you have specified that data should be sent every second, the
 * data will actually be transmitted at the end of the frame that comes on or after one second has elapsed.
 * 
 * @since 0.5
 * @author lc0277
 *
 */
public enum ClientDataPeriod {
	/** Specifies that the data is not to be sent. */
	NEVER,
	/** Specifies that the data should be sent once only. Note that this is not an efficient way of receiving 
	 * data frequently, use one of the other periods if there is a regular frequency to the data request. */
    ONCE,
    /** Specifies that the data should be sent every visual (rendered) frame. */
    VISUAL_FRAME,
    /** Specifies that the data should be sent whenever it is set. */
    ON_SET,
    /** Specifies that the data should be sent once every second. */
    SECOND,

}
