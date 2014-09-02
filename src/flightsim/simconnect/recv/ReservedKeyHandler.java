package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

public interface ReservedKeyHandler {
	public void handleReservedKey(SimConnect sender, RecvReservedKey e);

}
