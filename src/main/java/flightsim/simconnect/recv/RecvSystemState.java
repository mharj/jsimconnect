package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectConstants;

/**
 * The <code>RecvSystemState</code> structure is used with the 
 * {@link SimConnect#requestSystemState(int, String)} method 
 * to retrieve specific Flight Simulator systems states and information.
 * 
 * <p> Typically only one of the received integer, float or string will 
 * contain information, which one will depend on the request and can be 
 * identified by the request ID. Refer to the descriptions of the 
 * {@link SimConnect#setSystemState(String, int, float, String)}
 * and {@link SimConnect#requestSystemState(int, String)} functions. </p>
 * 
 * @author lc0277
 *
 */
public class RecvSystemState extends RecvPacket {
	private final int requestID;
	private final int dataInteger;
	private final float dataFloat;
	private final String dataString;
	
	RecvSystemState(ByteBuffer bf) {
		super(bf, RecvID.ID_SYSTEM_STATE);
		requestID = bf.getInt();
		dataInteger = bf.getInt();
		dataFloat = bf.getFloat();
		dataString = makeString(bf, SimConnectConstants.MAX_PATH);
	}

	/**
	 * Returns a float value
	 * @return misc data
	 */
	public float getDataFloat() {
		return dataFloat;
	}

	/**
	 * Returns an integer, or boolean, value.
	 * @return misc data
	 */
	public int getDataInteger() {
		return dataInteger;
	}

	/**
	 * Returns a string value
	 * @return misc data
	 */
	public String getDataString() {
		return dataString;
	}

	/**
	 * Returns the client defined request ID.
	 * @return client id
	 */
	public int getRequestID() {
		return requestID;
	}
	
	
	
}
