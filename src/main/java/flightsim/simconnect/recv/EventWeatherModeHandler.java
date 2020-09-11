package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

/**
 * Handler interface for receiving weather mode changes
 * 
 * @since 0.5
 * @author lc0277
 *
 */
public interface EventWeatherModeHandler {
	public void handleWeatherMode(SimConnect sender, RecvEventWeatherMode event);
}
