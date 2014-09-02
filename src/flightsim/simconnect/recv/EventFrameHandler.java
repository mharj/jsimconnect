package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

public interface EventFrameHandler {
	public void handleEventFrame(SimConnect sender, RecvEventFrame event);
	
}
