package flightsim.simconnect.data;

import java.io.Serializable;
import java.nio.ByteBuffer;

import flightsim.simconnect.SimConnectConstants;

/**
 * The {@link Waypoint} structure is used to hold all the necessary 
 * information on a waypoint.
 * 
 * @author lc0277
 *
 */
public class Waypoint implements SimConnectData, Serializable, SimConnectConstants {
	private static final long serialVersionUID = 6165789235638978423L;

	/** latitude of waypoint, in degrees */
	public double latitude;   // degrees
	/** longitude of waypoint, in degrees */
    public double longitude;  // degrees
    /** altitude of waypoint in feets */
    public double altitude;   // feet   
    /** flags of waypoints
     * @see flightsim.simconnect.SimConnectConstants#WAYPOINT_ON_GROUND
     * @see flightsim.simconnect.SimConnectConstants#WAYPOINT_REVERSE
     * @see flightsim.simconnect.SimConnectConstants#WAYPOINT_ALTITUDE_IS_AGL
     * @see flightsim.simconnect.SimConnectConstants#WAYPOINT_COMPUTE_VERTICAL_SPEED
     * @see flightsim.simconnect.SimConnectConstants#WAYPOINT_SPEED_REQUESTED
     * @see flightsim.simconnect.SimConnectConstants#WAYPOINT_THROTTLE_REQUESTED
     * 
     */
    public int flags;
    /** speed, in kots. {@link flightsim.simconnect.SimConnectConstants#WAYPOINT_SPEED_REQUESTED} must be on */
    public double speed;
    /** throttle, in percent {@link flightsim.simconnect.SimConnectConstants#WAYPOINT_THROTTLE_REQUESTED} must be on */
    public double throttle;
    
    /**
     * Construct a waypoint from basic informations
     * @param latitude
     * @param longitude
     * @param altitude
     */
	public Waypoint(double latitude, double longitude, double altitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
	}
	
	
	/**
	 * Construct a waypoint with all informations
	 * @param latitude
	 * @param longitude
	 * @param altitude
	 * @param flags
	 * @param speed
	 * @param throttle
	 * @since 0.2
	 */
	public Waypoint(double latitude, double longitude, double altitude, int flags, double speed, double throttle) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.flags = flags;
		this.speed = speed;
		this.throttle = throttle;
	}



	/**
	 * Construct a blank waypoint
	 *
	 */
	public Waypoint() {
		
	}
	
	/**
	 * @see SimConnectData#read(ByteBuffer)
	 */
	public void read(ByteBuffer buffer) {
		latitude = buffer.getDouble();
		longitude = buffer.getDouble();
		altitude = buffer.getDouble();
		flags = buffer.getInt();
		speed = buffer.getDouble();
		throttle  = buffer.getDouble();
	}
	
	/**
	 * @see SimConnectData#write(ByteBuffer)
	 */
	public void write(ByteBuffer buffer) {
		buffer.putDouble(latitude);
		buffer.putDouble(longitude);
		buffer.putDouble(altitude);
		buffer.putInt(flags);
		buffer.putDouble(speed);
		buffer.putDouble(throttle);
	}
    
	/**
	 * Set position from another structure
	 * @param lla {@link LatLonAlt} structure
	 * @since 0.4
	 */
	public void setLatLonAlt(LatLonAlt lla) {
		this.latitude = lla.latitude;
		this.longitude = lla.longitude;
		altitude = lla.altitude / 0.3048;
	}

}
