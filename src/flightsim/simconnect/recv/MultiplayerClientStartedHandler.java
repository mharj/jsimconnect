package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

/**
 * @since 0.7
 * @author lc
 *
 */
public interface MultiplayerClientStartedHandler {
	public void handleMultiplayerClientStarted(SimConnect sender, RecvEventMultiplayerClientStarted e);

}
