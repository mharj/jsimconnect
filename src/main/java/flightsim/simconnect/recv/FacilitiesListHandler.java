package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

/**
 * The handler interface for receiving facilities list. For a more convenient use,
 * all types of facilities are received through the same handler interface. 
 * For instance, requesting an VOR list will result in the call of the 
 * {@link #handleVORList(SimConnect, RecvVORList)} method only.
 * 
 * @since 0.5
 * @author lc0277
 *
 */
public interface FacilitiesListHandler {
	
	public void handleAirportList(SimConnect sender, RecvAirportList list);
	public void handleWaypointList(SimConnect sender, RecvWaypointList list);
	public void handleVORList(SimConnect sender, RecvVORList list);
	public void handleNDBList(SimConnect sender, RecvNDBList list);
	
}
