package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

public interface MultiplayerSessionEndedHandler {
	public void handleMultiplayerSessionEnded(SimConnect sender, RecvEventMultiplayerSessionEnded e);

}
