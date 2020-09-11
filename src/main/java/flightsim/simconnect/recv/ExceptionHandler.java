package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

public interface ExceptionHandler {
	public void handleException(SimConnect sender, RecvException e);
	
}
