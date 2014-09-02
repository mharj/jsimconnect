package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

public interface QuitHandler {
	public void handleQuit(SimConnect sender, RecvQuit e);
}
