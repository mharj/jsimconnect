package flightsim.simconnect.data;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * The {@link InitPosition} structure is used to initialize the position
 *  of the user aircraft, AI controlled aircraft, or other simulation object.
 *  
 *  <p>
 *  The primary use of this structure is to initialize the positioning of the 
 *  user aircraft, because it also optimizes some of the terrain systems and 
 *  other Flight Simulator systems. Simply setting parameters such as latitude, 
 *  longitude and altitude does not perform this kind of optimization. This 
 *  structure should not be used to incrementally move the user aircraft 
 *  (as this will unnecessarily initiate the reloading of scenery), in this 
 *  case change the latitude, longitude, altitude and other parameters of the 
 *  aircraft appropriately (using the variables described in the Simulation 
 *  Variables document). </p>
 *  
 *  <p> This structure can be used to incrementally move or reposition an
 *   AI controlled aircraft, or any other aircraft not controlled by the 
 *   user, as the terrain system optimizations are not performed in this case.
 *   </p>
 *   
 * <p> This structure is used by the functions: 
 * {@link flightsim.simconnect.SimConnect#aICreateNonATCAircraft(String, String, InitPosition, int)}
 * {@link flightsim.simconnect.SimConnect#aICreateSimulatedObject(String, InitPosition, int)}
 * {@link flightsim.simconnect.SimConnect#addToDataDefinition(int, String, String, SimConnectDataType, float, int)}
 * </p>
 * 
 * <p> This structure can only be used to set data, it cannot be used as part of a 
 * data request.
 * </p>
 * 
 * @author lc0277
 *
 */
public class InitPosition implements SimConnectData, Serializable {
	private static final long serialVersionUID = -1336171966431611602L;
	
	/** latitude, in degrees */
	public double  latitude;   // degrees
	/** longitude, in degrees */
    public double  longitude;  // degrees
	/** altitude, in feet */
    public double  altitude;   // feet   
	/** pitch, in degrees */
    public double  pitch;      // degrees
	/** bank, in degrees */
    public double  bank;       // degrees
	/** heading, in degrees */
    public double  heading;    // degrees
	/** true to set object on ground */
    public boolean   onGround;   // 1=force to be on the ground
	/** airspeed, in knots 
	 * @see flightsim.simconnect.SimConnectConstants#INITPOSITION_AIRSPEED_CRUISE
	 * @see flightsim.simconnect.SimConnectConstants#INITPOSITION_AIRSPEED_KEEP
	 * */
    public int  airspeed;   // knots
    
    /**
     * Initialise the structure with blank arguments
     *
     */
    public InitPosition() {
    	
    }
    
    /**
     * Initialise the structure
     * @param latitude
     * @param longitude
     * @param altitude
     */
	public InitPosition(double latitude, double longitude, double altitude) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
	}

	public InitPosition(double latitude, double longitude, double altitude, double pitch, double bank, double heading, boolean onGround, int airspeed) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.pitch = pitch;
		this.bank = bank;
		this.heading = heading;
		this.onGround = onGround;
		this.airspeed = airspeed;
	}
	
	/**
	 * Read the structure from a byte buffer. Buffer position is updated
	 */
	public void read(ByteBuffer buffer) {
		latitude = buffer.getDouble();
		longitude = buffer.getDouble();
		altitude = buffer.getDouble();
		pitch = buffer.getDouble();
		bank = buffer.getDouble();
		heading = buffer.getDouble();
		int tmp = buffer.getInt();
		onGround = ((tmp == 1) ? true : false);
		airspeed = buffer.getInt();
	}
	
	/**
	 * Write the contents of the structure to a buffer. Buffer position
	 * is updated
	 */
	public void write(ByteBuffer buffer) {
		buffer.putDouble(latitude);
		buffer.putDouble(longitude);
		buffer.putDouble(altitude);
		buffer.putDouble(pitch);
		buffer.putDouble(bank);
		buffer.putDouble(heading);
		buffer.putInt(onGround ? 1 : 0);
		buffer.putInt(airspeed);
	}
    
	/**
	 * Copy the latitude, longitude, altitude of this initposition from a {@link LatLonAlt} 
	 * structure
	 * @param lla structure to copy
	 * @throws NullPointerException if <code>lla</code> is null
	 * @since 0.4
	 */
	public void setLatLonAlt(LatLonAlt lla) {
		latitude = lla.latitude;
		longitude = lla.longitude;
		altitude = lla.altitude / 0.3048;
	}
	
	/**
	 * Copy the latitude, longitude, altitude of this initposition from a {@link XYZ} 
	 * structure, giving the indexes of data
	 * @param xyz 
	 * @param latIndex
	 * @param lonIndex
	 * @param altIndex
	 * @throws NullPointerException if <code>xyz</code> is null
	 * @throws IllegalArgumentException if indexes are invalid
	 * @since 0.4
	 */
	public void setLatLonAlt(XYZ xyz, int latIndex, int lonIndex, int altIndex) {
		if (latIndex < 0 ||
				latIndex > 2 ||
				lonIndex < 0 ||
				lonIndex > 2 ||
				altIndex < 0 ||
				altIndex > 2) throw new IllegalArgumentException("Indices out of bound");
		latitude = xyz.get(latIndex);
		longitude = xyz.get(lonIndex);
		altitude = xyz.get(altIndex) / 0.3048;
	}
	
	/**
	 * Copy the latitude, longitude, altitude of this initposition from a {@link XYZ} 
	 * structure with latitude in x, longitude in y, altitude in z
	 * @param xyz 
	 * @throws NullPointerException if <code>xyz</code> is null
	 * @since 0.4
	 */
	public void setLatLonAlt(XYZ xyz) {
		setLatLonAlt(xyz, 0, 1, 2);
	}
	
	/**
	 * Copy the pitch, bank, heading of this initposition from a {@link XYZ} 
	 * structure, giving the indexes of data
	 * data must be in DEGREES
	 * @throws NullPointerException if <code>xyz</code> is null
	 * @param xyz
	 * @param pitchIndex
	 * @param bankIndex
	 * @param headingIndex
	 * @throws IllegalArgumentException if indexes are invalid
	 * @since 0.4
	 */
	public void setPitchBankHeading(XYZ xyz, int pitchIndex, int bankIndex, int headingIndex) {
		if (pitchIndex < 0 ||
				pitchIndex > 2 ||
				bankIndex < 0 ||
				bankIndex > 2 ||
				headingIndex < 0 ||
				headingIndex > 2) throw new IllegalArgumentException("Indices out of bound");
		pitch = xyz.get(pitchIndex);
		bank = xyz.get(bankIndex);
		heading = xyz.get(headingIndex);
	}
	
	/**
	 * Copy the pitch, bank, heading of this initposition from a {@link XYZ} 
	 * structure with pitch in x, bank in y, heading in z
	 * data must be in DEGREES
	 * @param xyz 
	 * @throws NullPointerException if <code>xyz</code> is null
	 * @since 0.4
	 */
	public void setPitchBankHeading(XYZ xyz) {
		setPitchBankHeading(xyz, 0, 1, 2);
	}
	
	@Override
	public String toString() {
		return latitude + ", " + longitude + ", " + altitude;
	}

}
