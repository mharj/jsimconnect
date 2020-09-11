package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

/**
 * The <code>FacilityAirport</code> structure is used to return 
 * information on a single airport in the facilities cache.
 * 
 * @since 0.5
 * @author lc0277
 *
 */
public class FacilityAirport {

	private String icao;
	private double latitude;
	private double longitude;
	private double altitude;
	
	FacilityAirport(ByteBuffer bf) {
		icao = makeString(bf, 9);
		latitude = bf.getDouble();
		longitude = bf.getDouble();
		altitude = bf.getDouble();
	}
		
	
	/**
	 * A constructor with fields (if you want to use this class in your
	 * own data model)
	 * @param icao
	 * @param latitude
	 * @param longitude
	 * @param altitude
	 */
	public FacilityAirport(String icao, double latitude, double longitude, double altitude) {
		super();
		this.icao = icao;
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
	}



	/**
	 * Returns the altitude of the airport in facility (in meters)
	 * @return altitude (meters)
	 */
	public double getAltitude() {
		return altitude;
	}
	
	/**
	 * Returns the ICAO code of the facility. 
	 * @return icao
	 */
	public String getIcao() {
		return icao;
	}
	
	/**
	 * Returns the Latitude of the airport in facility (in degrees)
	 * @return latitude (degrees)
	 */
	public double getLatitude() {
		return latitude;
	}
	
	/**
	 * Returns the Longitude of the airport in facility (in degrees)
	 * @return longitude (degrees)
	 */
	public double getLongitude() {
		return longitude;
	}
	
	/**
	 * read a string from the buffer. 
	 * @param bf buffer
	 * @param len len of string to read
	 * @return a string
	 */
	String makeString(ByteBuffer bf, int len) {
		byte[] tmp = new byte[len];
		bf.get(tmp);
		int fZeroPos = 0;
		while ((fZeroPos < len) && (tmp[fZeroPos] != 0)) fZeroPos++;
		return new String(tmp, 0, fZeroPos);
	}
	
	@Override
	public String toString() {
		return icao + " (" + latitude + ", " + longitude + ", " +altitude + ")";
	}

	
}
