package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimObjectType;

/**
 * The <code>RecvEventAddRemove</code> structure is used to return 
 * the type and ID of an AI object that has been added or removed 
 * from the simulation, by any client.
 * 
 * <p> A client can determine whether the object was added or removed
 * from its own event ID that was provided as a parameter to the 
 * {@link SimConnect#subscribeToSystemEvent(int, String)} method. </p>
 * 
 * <p> The ID of the object added or removed is returned in the 
 * {@link RecvEvent#getData()} parameter </p>
 * 
 * @author lc0277
 *
 */
public class RecvEventAddRemove extends RecvEvent {
	private final SimObjectType type;
	
	RecvEventAddRemove(ByteBuffer bf) {
		super(bf, RecvID.ID_EVENT_OBJECT_ADDREMOVE);
		type = SimObjectType.type(bf.getInt());
	}

	/**
	 * Specifies the type of object that was added or removed. 
	 * One member of the {@link SimObjectType} enumeration.
	 * @see SimObjectType
	 * @return object type
	 */
	public SimObjectType getType() {
		return type;
	}
	
	/**
	 * Returns the object container ID who have been added or deleted
	 */
	public int getData() {
		return super.getData();
	}
	
}
