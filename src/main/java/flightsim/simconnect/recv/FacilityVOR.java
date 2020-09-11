package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

/**
 * The <code>FacilityVOR</code> structure is used to return information
 * on a single VOR station in the facilities cache.
 *
 * @since 0.5
 * @author lc0277
 *
 */
public class FacilityVOR extends FacilityNDB {
	
	/** Set if the station has a NAV transmitter, and if so, 
	 * {@link #getGlideAlt()}, {@link #getGlideLat()} and 
	 * {@link #getGlideLon()} contain valid data. */
	public static final int HAS_NAV_SIGNAL	= 0x00000001;
	/** Set if the station transmits an ILS localizer angle, and if 
	 * so {@link #getLocalizer()} contains valid data. */
	public static final int HAS_LOCALIZER	= 0x00000002;
	/** Set if the station transmits an ILS approach angle, and if so
	 * {@link #getGlideSlopeAngle()} contains valid data. */
	public static final int HAS_GLIDE_SLOPE	= 0x00000004;
	/** Set if the station t transmits a DME signal, and if so the 
	 * inherited DME {@link #getFrequency()} contains valid data. */
	public static final int HAS_DME			= 0x00000008;
	
	private int flags;
	private float localizer;
	private double glideLat;
	private double glideLon;
	private double glideAlt;
	private float glideSlopeAngle;
	
	
	FacilityVOR(ByteBuffer bf) {
		super(bf);
		flags = bf.getInt();
		localizer = bf.getFloat();
		glideLat = bf.getDouble();
		glideLon = bf.getDouble();
		glideAlt = bf.getDouble();
		glideSlopeAngle = bf.getFloat();
	}
	
	public int getFlags() {
		return flags;
	}
	
	public double getGlideAlt() {
		return glideAlt;
	}
	
	public double getGlideLat() {
		return glideLat;
	}
	
	public double getGlideLon() {
		return glideLon;
	}
	
	public float getGlideSlopeAngle() {
		return glideSlopeAngle;
	}
	
	public float getLocalizer() {
		return localizer;
	}
	
	private boolean hasFlag(int constant) {
		return (flags & constant) != 0;
	}
	
	
	/**
	 * Returns true if the station has a NAV transmitter, and if so, 
	 * {@link #getGlideAlt()}, {@link #getGlideLat()} and 
	 * {@link #getGlideLon()} contain valid data. 
	 * @return true if VOR has a NAV transmitter
	 */
	public boolean hasNAVSignal() {
		return hasFlag(HAS_NAV_SIGNAL);
	}

	/**
	 * Returns true if the station transmits an ILS localizer
	 * angle and if 
	 * so {@link #getLocalizer()} contains valid data.
	 * @return true if the station transmits an ILS localizerangle
	 */
	public boolean hasLocalizer() {
		return hasFlag(HAS_LOCALIZER);
	}

	/** Returns true if the station transmits an ILS approach angle, and if so
	 * {@link #getGlideSlopeAngle()} contains valid data.
	 * @return true  if the station transmits an ILS approach angle.
	 */
	public boolean hasGlideSlope() {
		return hasFlag(HAS_GLIDE_SLOPE);
	}

	/**
	 * Returns true if the station transmits a DME signal, and if so the 
	 * inherited DME {@link #getFrequency()} contains valid data.
	 * @return true if the station transmits a DME signal
	 */
	public boolean hasDME() {
		return hasFlag(HAS_DME);
	}
	
	@Override
	public String toString() {
		return super.toString() + " localizer=" + localizer +" glideslope=" + glideSlopeAngle + " flags=0x" +Integer.toHexString(flags); 
	}
	
	
}
