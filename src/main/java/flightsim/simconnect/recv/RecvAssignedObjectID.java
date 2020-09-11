package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

public class RecvAssignedObjectID extends RecvPacket {
	private final int requestID;
	private final int objectID;
	
	RecvAssignedObjectID(ByteBuffer bf) {
		super(bf, RecvID.ID_ASSIGNED_OBJECT_ID);
		requestID = bf.getInt();
		objectID = bf.getInt();
	}

	public int getObjectID() {
		return objectID;
	}

	public int getRequestID() {
		return requestID;
	}
	
	

}
