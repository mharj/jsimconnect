package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

/**
 * @author lc0277
 *
 */
public interface AssignedObjectHandler {
	public void handleAssignedObject(SimConnect sender,  RecvAssignedObjectID e);
}
