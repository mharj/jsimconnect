package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

public interface SystemStateHandler {
	public void handleSystemState(SimConnect sender, RecvSystemState e);

}
