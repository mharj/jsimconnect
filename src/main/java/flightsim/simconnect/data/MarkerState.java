package flightsim.simconnect.data;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * The <code>MarkerState</code> structure is used to help graphically 
 * link flight model data with the graphics model.
 * 
 * @author lc0277
 *
 */
public class MarkerState implements SimConnectData, Serializable {
	private static final long serialVersionUID = -9147497378014027101L;
	
	/**  string containing the marker name. */
	public String markerName;
	/** the marker state, set to 1 for on and 0 for off. */
	public boolean markerState;
	
	public void read(ByteBuffer buffer) {
		byte[] tmp = new byte[64];
		buffer.get(tmp);
		int fZeroPos = 0;
		while ((fZeroPos < 64) && (tmp[fZeroPos] != 0)) fZeroPos++;
		markerName = new String(tmp, 0, fZeroPos);
		int i = buffer.getInt();
		markerState = (i == 0) ? false : true;
	}

	public void write(ByteBuffer buffer) {
		putString(buffer, markerName, 64);
		buffer.putInt(markerState ? 1 : 0);
	}

	private void putString(ByteBuffer bf, String s, int fixed) {
		if (s == null) s = "";
		byte[] b = s.getBytes();
		bf.put(b, 0, Math.min(b.length, fixed));
		// pad data
		if (b.length < fixed) {
			for (int i = 0; i < (fixed - b.length); i++) {
				bf.put((byte) 0);
			}
		}
	}

}
