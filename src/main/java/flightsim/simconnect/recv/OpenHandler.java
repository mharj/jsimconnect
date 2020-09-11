package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

public interface OpenHandler {
	public void handleOpen(SimConnect sender,  RecvOpen e);

}
