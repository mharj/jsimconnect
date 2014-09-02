package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

public class RecvCustomAction extends RecvEvent {
	private final byte[] guid;
	private final int waitForCompletion;
	private final String payLoad;
	
	RecvCustomAction(ByteBuffer bf) {
		super(bf, RecvID.ID_CUSTOM_ACTION);
		guid = new byte[16];
		bf.get(guid);
		waitForCompletion = bf.getInt();
		payLoad = super.makeString(bf, bf.remaining());
	}

	public byte[] getGuid() {
		return guid;
	}
	
	public String getPayLoad() {
		return payLoad;
	}

	public int getWaitForCompletion() {
		return waitForCompletion;
	}
	
	

	
}
