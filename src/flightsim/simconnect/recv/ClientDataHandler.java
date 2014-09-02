package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

/**
 * 
 * @author lc0277
 *
 */
public interface ClientDataHandler {
	public void handleClientData(SimConnect sender, RecvClientData e);

}
