package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

/**
 * @since 0.7
 * @author lc
 *
 */
public interface RaceEndHandler {
	public void handleRaceEnd(SimConnect sender, RecvEventRaceEnd e);
}
