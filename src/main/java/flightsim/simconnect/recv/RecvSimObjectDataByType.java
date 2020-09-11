package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

import flightsim.simconnect.SimConnect;

/**
 * The <code>RecvSimObjectDataByType</code> structure will be received 
 * by the client after a successful call to {@link SimConnect#requestDataOnSimObjectType(int, int, int, flightsim.simconnect.SimObjectType)}. 
 * It is an identical structure to {@link RecvSimObjectData}.
 *
 * @author lc0277
 *
 */
public class RecvSimObjectDataByType extends RecvSimObjectData {

	RecvSimObjectDataByType(ByteBuffer bf) {
		super(bf, RecvID.ID_SIMOBJECT_DATA_BYTYPE);
	}

	
}
