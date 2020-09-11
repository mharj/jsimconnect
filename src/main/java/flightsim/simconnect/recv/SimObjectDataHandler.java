package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

public interface SimObjectDataHandler {
	public void handleSimObject(SimConnect sender, RecvSimObjectData e);

}
