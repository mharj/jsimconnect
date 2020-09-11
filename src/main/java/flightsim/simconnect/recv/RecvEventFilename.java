package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

import flightsim.simconnect.SimConnectConstants;

/**
 * The <code>RecvEventFilename</code> structure is used to return a 
 * filename and an event ID to the client.

 * @author lc0277
 *
 */
public class RecvEventFilename extends RecvEvent {
	private final String fileName;
	private final int flags;
	
	RecvEventFilename(ByteBuffer bf) {
		super(bf, RecvID.ID_EVENT_FILENAME);
		fileName = super.makeString(bf, SimConnectConstants.MAX_PATH);
		flags = bf.getInt();
	}

	/**
	 * The returned filename.
	 * @return filenmae
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Reserved
	 * @return 0
	 */
	public int getFlags() {
		return flags;
	}

	
}
