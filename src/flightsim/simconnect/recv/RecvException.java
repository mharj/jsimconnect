package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

import flightsim.simconnect.SimConnect;

/**
 * The <code>RecvException</code> structure is used with the
 * {@link SimConnectException} enumeration type to return information 
 * on an error that has occured.
 * 
 * <p> In order to match the dwSendID parameter returned here, with the ID
 *  of a request, use the {@link SimConnect#getLastSentPacketID()} 
 *  call after each request is made. </p>

 * @author lc0277
 *
 */
public class RecvException extends RecvPacket {
	private final SimConnectException exception;
	private int sendID;
	private int index;
	
	RecvException(ByteBuffer bf) {
		super(bf, RecvID.ID_EXCEPTION);
		exception = SimConnectException.type(bf.getInt());
		sendID = bf.getInt();
		index = bf.getInt();
	}

	/**
	 * One member of the {@link SimConnectException} enumeration type, 
	 * indicating which error has occured.
	 * @return exception code
	 */
	public SimConnectException getException() {
		return exception;
	}

	/**
	 * The index number (starting at 1) of the first parameter that caused an error. 0 when unknown
	 * @return parameter index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * The ID of the packet that contained the error,
	 * @return erroneous packet id
	 * @see SimConnect#getLastSentPacketID()
	 */
	public int getSendID() {
		return sendID;
	}
	
	

}
