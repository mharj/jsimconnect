package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

import flightsim.simconnect.SimConnectConstants;

/**
 * Base class for all received data packets. It contains structural information
 * about the packet, such as simConnect protocol version.
 * 
 * @author lc0277
 *
 */
public class RecvPacket {
	private int size;
	private int version;
	private int id;
	private RecvID recognisedID;
	private byte[] unparsed;
	
	/**
	 * Should <b>not</b> be called by subclasses, as it consumes all the buffer
	 * @param bf
	 */
	RecvPacket(ByteBuffer bf) {
		size = bf.getInt();
		version = bf.getInt();
		id = bf.getInt();
		unparsed = new byte[bf.remaining()];
		bf.get(unparsed);
	}

	/**
	 * Called that subclasses when they know their ID
	 * @param bf buffer
	 * @param id packet type
	 */
	protected RecvPacket(ByteBuffer bf, RecvID id) {
		size = bf.getInt();
		version = bf.getInt();
		this.id = bf.getInt();
		this.recognisedID = id;
	}
	
	/**
	 * read a string from the buffer. This methods is an helper
	 * for subclasses
	 * @param bf buffer
	 * @param len len of string to read
	 * @return a string
	 */
	protected String makeString(ByteBuffer bf, int len) {
		byte[] tmp = new byte[len];
		bf.get(tmp);
		int fZeroPos = 0;
		while ((fZeroPos < len) && (tmp[fZeroPos] != 0)) fZeroPos++;
		return new String(tmp, 0, fZeroPos);
	}
	
	/**
	 * Returns the total packet size
	 * @return packet size
	 */
	public int getSize() {
		return size;
	}
	
	/**
	 * returns the identificator of packet type, as a member
	 * of {@link RecvID} enumeration. If the packet was not
	 * successfully parsed, this information is not available.
	 * @return pack type
	 */
	public RecvID getID() {
		return recognisedID;
	}
	
	/**
	 * Returns the identificator of packet type, as numeric
	 * integer.
	 * @return id
	 * @since 0.5
	 */
	public int getRawID() {
		return id;
	}
	
	/**
	 * returns the identificator of packet type, as a member
	 * of {@link RecvID} enumeration. If the packet was not
	 * successfully parsed, this information is not available.
	 * @return pack type
	 */
	public RecvID getRecognisedID() {
		return recognisedID;
	}
	
	/**
	 * Returns the protocol version of the packet
	 * @return protocol version
	 * @see SimConnectConstants#PROTO_VERSION
	 */
	public int getVersion() {
		return version;
	}
	
	/**
	 * Returns <value>null</value> on any subclass of recv
	 * @return the entire buffer, only if it was not parsed by a subclass
	 */
	public byte[] getUnparsedData() {
		return unparsed;
	}
	
}

