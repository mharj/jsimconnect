package flightsim.simconnect.data;

import java.nio.ByteBuffer;

/**
 * The base interface for simconnect data structures. Definitions of methods
 * to read/write structure to flat byte-array representation.
 * 
 * @see flightsim.simconnect.recv.RecvSimObjectData#getData
 * @see flightsim.simconnect.data.InitPosition
 * @see flightsim.simconnect.data.LatLonAlt
 * @see flightsim.simconnect.data.Waypoint
 * @see flightsim.simconnect.data.XYZ
 * @see flightsim.simconnect.data.MarkerState
 * 
 * @author lc0277
 *
 */
public interface SimConnectData {

	/**
	 * Write the contents of the structure to a buffer. Buffer position
	 * is updated
	 */
	public void write(ByteBuffer buffer);
	
	/**
	 * Read the structure from a byte buffer. Buffer position is updated
	 */
	public void read(ByteBuffer buffer);
}
