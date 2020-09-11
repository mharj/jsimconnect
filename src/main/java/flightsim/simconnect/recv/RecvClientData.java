package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

public class RecvClientData extends RecvSimObjectData {

	RecvClientData(ByteBuffer bf) {
		super(bf, RecvID.ID_CLIENT_DATA);
	}

	
}
