package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

public interface CustomActionHandler {
	public void handleCustomAction(SimConnect sender, RecvCustomAction e);

}
