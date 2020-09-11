package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

public interface SimObjectDataTypeHandler {
	public void handleSimObjectType(SimConnect sender, RecvSimObjectDataByType e);

}
