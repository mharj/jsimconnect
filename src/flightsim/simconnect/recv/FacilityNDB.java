package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

/**
 * The <code>FacilityNDB</code> structure is used to return information 
 * on a single NDB station in the facilities cache.
 * 
 * @since 0.5
 * @author lc0277
 *
 */
public class FacilityNDB extends FacilityWaypoint {

	private int frequency;

	FacilityNDB(ByteBuffer bf) {
		super(bf);
		frequency = bf.getInt();
	}

	/**
	 * Returns the frequency of the station, in hertz.
	 * @return frequency
	 */
	public int getFrequency() {
		return frequency;
	}
	
	@Override
	public String toString() {
		return super.toString() + " freq=" + frequency + "Hz";
	}
}
