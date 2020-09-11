package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

public interface EventFilenameHandler {
	public void handleFilename(SimConnect sender, RecvEventFilename e);
}
