package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

/**
 * The <code>RecvFacilitiesList</code> structure is used to provide information on the number of elements in
 * a list of facilities returned to the client, and the number of packets that were used to transmit the data.
 * 
 * @since 0.5
 * @author lc0277
 *
 */
public abstract class RecvFacilitiesList extends RecvPacket {
	private final int requestID;
	private final int arraySize;
	private final int entryNumber;
	private final int outOf;
	
	RecvFacilitiesList(ByteBuffer bf, RecvID id) {
		super(bf, id);
		requestID = bf.getInt();
		arraySize = bf.getInt();
		entryNumber = bf.getInt();
		outOf = bf.getInt();
	}
	
	/**
	 * Returns all the facilities of the packet in an array
	 * @return facilities structures
	 */
	public abstract FacilityAirport[] getFacilities();
	

	/**
	 * Double word containing the number of elements in the list that are within this packet. For example, 
	 * if there are 25 airports returned in the {@link RecvAirportList} structure, then this field will contain 
	 * 25, but if there are 400 airports in the list and the data is returned in two packets, then this 
	 * value will contain the number of entries within each packet.
	 * 
	 * @return number of elements
	 */
	public int getArraySize() {
		return arraySize;
	}

	/**
	 * Double word containing the index number of this list packet. This number will be from 0 to 
	 * {@link #getOutOf()} - 1.
	 * 
	 * @return index number
	 */
	public int getEntryNumber() {
		return entryNumber;
	}

	/**
	 * Double word containing the total number of packets used to transmit the list.
	 * @return total number of packets
	 */
	public int getOutOf() {
		return outOf;
	}

	/**
	 * Double word containing the client defined request ID. 
	 * @return request ID
	 */
	public int getRequestID() {
		return requestID;
	}
	
	
	
	
	
}
