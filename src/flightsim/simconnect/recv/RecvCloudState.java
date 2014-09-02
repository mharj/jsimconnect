package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

/**
 * Cloud state. Contains a bi-dimensionnal array of cloud state. The array is 64 x 64 and
 * values are 0 for no cloud and 255 for maximum cloud
 * @author lc0277
 *
 */
public class RecvCloudState extends RecvPacket {
	private final int requestID;
	private final int arraySize;
	private final byte[][] data;
	
	RecvCloudState(ByteBuffer bf) {
		super(bf, RecvID.ID_CLOUD_STATE);
		requestID = bf.getInt();
		arraySize = bf.getInt();
		data = new byte[64][64];
		for (int i = 0; i < 64; i++) {
			if (bf.remaining() >= 64) bf.get(data[i]);
		}
	}

	/**
	 * Returns the array size, usually 64*64
	 * @return  array size
	 */
	public int getArraySize() {
		return arraySize;
	}

	/**
	 * Return the cloud state array
	 * @return cloud data
	 */
	public byte[][] getData() {
		return data;
	}

	/**
	 * Returns the request identifier
	 * @return request ID
	 */
	public int getRequestID() {
		return requestID;
	}
	
	
}
