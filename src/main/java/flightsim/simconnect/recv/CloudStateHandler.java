package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

/**
 * 
 * @author lc0277
 *
 */
public interface CloudStateHandler {
	public void handleCloudState(SimConnect sender, RecvCloudState e);

}
