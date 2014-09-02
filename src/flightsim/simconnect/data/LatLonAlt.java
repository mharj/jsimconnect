package flightsim.simconnect.data;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * The <code>LatLonAlt</code> structure is used to hold a world position.
 * @author lc0277
 *
 */
public class LatLonAlt implements SimConnectData, Serializable, Cloneable {
	private static final long serialVersionUID = 7598871346462898633L;
	
	
	/** The latitude of the position in degrees.  */
	public double  latitude;   // degrees
	/** The longitude of the position in degrees.  */
    public double  longitude;  // degrees
    /** The altitude of the position in feet.  */
    public double  altitude;   // feet   
    
	public LatLonAlt(double latitude, double longitude, double altitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
	}
	
	public LatLonAlt() {
		
	}

	public void read(ByteBuffer buffer) {
		latitude = buffer.getDouble();
		longitude = buffer.getDouble();
		altitude = buffer.getDouble();
	}
	
	public void write(ByteBuffer buffer) {
		buffer.putDouble(latitude);
		buffer.putDouble(longitude);
		buffer.putDouble(altitude);
	}
    
	@Override
	public String toString() {
		return latitude + ", " + longitude + ", " + altitude;
	}
    

	@Override
	public LatLonAlt clone() {
		return new LatLonAlt(latitude, longitude, altitude);
	}
}
