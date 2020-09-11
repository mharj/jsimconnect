package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

/**
 * The <code>FacilityWaypoint</code> structure used to return information
 * on a single waypoint in the facilities cache.
 * @author lc0277
 *
 */
public class FacilityWaypoint extends FacilityAirport {
	

	private float magVar;
	
	FacilityWaypoint(ByteBuffer bf) {
		super(bf);
		magVar = bf.getFloat();
	}

	/**
	 * A constructor with fields (if you want to use this class in your
	 * own data model)
	 * @param icao
	 * @param latitude
	 * @param longitude
	 * @param altitude
	 */
	public FacilityWaypoint(String icao, double latitude, 
			double longitude, double altitude, float magVar) {
		super(icao, latitude, longitude, altitude);
		this.magVar = magVar;
	}



	/**
	 * Returns The magnetic variation of the waypoint in degrees.
	 * @return magnetic variation
 	 */
	public float getMagVar() {
		return magVar;
	}
	
	@Override
	public String toString() {
		return super.toString() + " magvar=" + magVar;
	}
}
