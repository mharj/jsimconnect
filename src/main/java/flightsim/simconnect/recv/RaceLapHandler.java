package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

/**
 * @since 0.7
 * @author lc
 *
 */
public interface RaceLapHandler {
	public void handleRaceLap(SimConnect sender, RecvEventRaceLap e);

}
