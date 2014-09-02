package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

/**
 * @since 0.7
 * @author lc
 *
 */
public interface MultiplayerServerStartedHandler {
	public void handleMultiplayerServerStarted(SimConnect sender, 
			RecvEventMultiplayerServerStarted e);

}
