package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

public interface EventHandler {
	public void handleEvent(SimConnect sender, RecvEvent e);
}
