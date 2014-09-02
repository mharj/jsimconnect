package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

public interface EventObjectHandler {
	public void handleEventObject(SimConnect sender, RecvEventAddRemove e);
}
