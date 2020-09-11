package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectConstants;

/**
 * The <code>RecvEvent</code> structure is used to return an event ID 
 * to the client.
 * @author lc0277
 *
 */
public class RecvEvent extends RecvPacket {
	private final int groupID;
	private final int eventID;
	private final int data;
	
	RecvEvent(ByteBuffer bf, RecvID id) {
		super(bf, id);
		groupID = bf.getInt();
		eventID = bf.getInt();
		data = bf.getInt();
	}

	RecvEvent(ByteBuffer bf) {
		this(bf, RecvID.ID_EVENT);
	}
		
	/**
	 * This value is usually zero, but some events require further qualification. 
	 * For example, joystick movement events require a movement value in addition 
	 * to the notification that the joystick has been moved 
	 * (see {@link SimConnect#mapInputEventToClientEvent(int, String, int, int, int, int, boolean)}
	 * for more information).
	 * @return data
	 */
	public int getData() {
		return data;
	}

	/**
	 * The ID of the client defined event that has been requested
	 * @return client event id
	 */
	public int getEventID() {
		return eventID;
	}
	
	/**
	 * The ID of the client defined group, or the special case value: 
	 * {@link SimConnectConstants#UNKNOWN_GROUP}. 
	 * @return group id
	 */
	public int getGroupID() {
		return groupID;
	}
	
	
	
}
