package flightsim.simconnect.recv;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.data.*;
import flightsim.simconnect.wrappers.DataWrapper;


/**
 * <p> The base class for all object data received. Calls that will receive a <code>RecvSimObjectData</code>
 * are {@link flightsim.simconnect.SimConnect#requestDataOnSimObject(int, int, int, SimConnectPeriod)} or
 * {@link flightsim.simconnect.SimConnect#requestDataOnSimObjectType(int, int, int, SimObjectType)} (as
 * {@link flightsim.simconnect.recv.RecvSimObjectDataByType}, a subclass of this). </p>
 * 
 * <p> The class provides access to <i>headers</i> members that are available in all received packets
 * and also to <i>defined</i> data correspond to the data definition you set up. </p>
 * 
 * <p> To access defined datas, use <code>getData</code> functions. Each function exists in two variants,
 * a position-less variant where a pointer is kept internally. Avoid multithreads operations on it.
 * The pointer can be reseted with {@link #reset()}. A variant exists for which the offset of the
 * data, relative to the beginning of the data block is passed. All data are packed in the order
 * you added with {@link SimConnect#addToDataDefinition(int, String, String, flightsim.simconnect.SimConnectDataType, float, int)} </p>
 * 
 * <p> If the data was defined with the following datatypes (in this order):
 * <ul>
 * <li> {@link flightsim.simconnect.SimConnectDataType#FLOAT64} </li>
 * <li> {@link flightsim.simconnect.SimConnectDataType#STRING256} </li>
 * <li> {@link flightsim.simconnect.SimConnectDataType#FLOAT32} </li>
 * <li> {@link flightsim.simconnect.SimConnectDataType#WAYPOINT} </li>
 * </li>
 * It can be recovered with the possible sets of instructions:
 * <ul> 
 * <li> {@link #getDataFloat64()}() </li>
 * <li> {@link #getDataString256()}() </li>
 * <li> {@link #getDataFloat32()}() </li>
 * <li> {@link #getWaypoint()}() </li>
 * </ul>
 * Or by specifiying offsets :
 * <ul> 
 * <li> {@link #getDataFloat64()}(0) </li>
 * <li> {@link #getDataString256()}(8) </li>
 * <li> {@link #getDataFloat32()}(264) </li>
 * <li> {@link #getWaypoint()}(268) </li>
 * </ul>
 * </p>
 * 
 * <p>
 * Note that variants with explicit offsets don't advance the current pointer. {@link #getDataFloat32()}(0) followed
 * by {@link #getDataFloat32()} returns the  same data.
 * </p>
 * 
 * 
 * @author lc0277
 *
 */
public class RecvSimObjectData extends RecvPacket {
	private final int requestID;
	private final int objectID;
	private final int defineID;
	private final int flags;
	private final int entryNumber;
	private final int outOf;
	private final int defineCount;
	private final byte[] data;
	
	private ByteBuffer dataBuffer;

	RecvSimObjectData(ByteBuffer bf) {
		this(bf, RecvID.ID_SIMOBJECT_DATA);
	}

	RecvSimObjectData(ByteBuffer bf, RecvID id) {
		super(bf, id);
		requestID = bf.getInt();
		objectID = bf.getInt();
		defineID = bf.getInt();
		flags = bf.getInt();
		entryNumber = bf.getInt();
		outOf = bf.getInt();
		defineCount = bf.getInt();
		// copy remaining data
		data = new byte[bf.remaining()];
		bf.get(data);
	}

	/**
	 * Returns a bare array representation of the data
	 * @return data
	 */
	public byte[] getData() {
		return data;
	}
	
	
	/**
	 * Returns the length of the data
	 * @since 0.4
	 * @return  data size
	 */
	public int getDataSize() {
		return data.length;
	}
	
	/**
	 * Returns a simple wrapper objects for data in this packet
	 * @return a data wrapper
	 * @see DataWrapper
	 * @since 0.2
	 */
	public DataWrapper getDataWrapper() {
		asByteBuffer();
		return new DataWrapper(dataBuffer);
	}
	
	/**
	 * not used, see {@link #remaining()}
	 * @return define count
	 */
	public int getDefineCount() {
		return defineCount;
	}

	/**
	 * The ID of the client defined data definition. 
	 * @return definition id
	 */
	public int getDefineID() {
		return defineID;
	}

	/**
	 * If multiple objects are being returned, this is the index number of this object out of a 
	 * total of {@link #getOutOf()}. This will always be 1 if the call was 
	 * {@link flightsim.simconnect.SimConnect#requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}, 
	 * and can be 0 or more if the call was {@link flightsim.simconnect.SimConnect#requestDataOnSimObjectType(int, int, int, SimObjectType)}.
	 * 
	 * @return entry number
	 */
	public int getEntryNumber() {
		return entryNumber;
	}

	/**
	 *   The flags that were set for this data request
	 * @return flags
	 */
	public int getFlags() {
		return flags;
	}

	/**
	 * The object ID. For {@link flightsim.simconnect.SimConnect#requestDataOnSimObjectType(int, int, int, SimObjectType)}
	 * it contains the internal object number, else it contains {@link flightsim.simconnect.SimConnectConstants#OBJECT_ID_USER}
	 * @return object ID
	 */
	public int getObjectID() {
		return objectID;
	}

	/**
	 * The total number of objects being returned. Note that {@link #getEntryNumber()} and 
	 * {@link #getOutOf()} start with 1 not 0, so if two objects are being  returned 
	 * {@link #getEntryNumber()} and {@link #getOutOf()} pairs will be 1,2 and 2,2 for the two objects. 
	 * This will always be 1 if the call was {@link flightsim.simconnect.SimConnect#requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}, 
	 * and can be 0 or more if the call was {@link flightsim.simconnect.SimConnect#requestDataOnSimObjectType(int, int, int, SimObjectType)}.
	 * 
	 * @return out of
	 */
	public int getOutOf() {
		return outOf;
	}

	/**
	 * Returns the request ID, as set with {@link flightsim.simconnect.SimConnect#requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}
	 * @return request id
	 */
	public int getRequestID() {
		return requestID;
	}
	
	private ByteBuffer asByteBuffer() {
		if (dataBuffer == null) {
			dataBuffer = ByteBuffer.wrap(data);
			dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
		return dataBuffer;
	}
	
	/**
	 * Read a float from given position
	 * @param offset
	 * @return float data at position
	 */
	public float getDataFloat32(int offset) {
		asByteBuffer();
		return dataBuffer.getFloat(offset);
	}

	/**
	 * Read a double from given position
	 * @param offset
	 * @return double data at position
	 */
	public double getDataFloat64(int offset) {
		asByteBuffer();
		return dataBuffer.getDouble(offset);
	}

	/**
	 * Read an int from given position
	 * @param offset
	 * @return int data at position
	 */
	public int getDataInt32(int offset) {
		asByteBuffer();
		return dataBuffer.getInt(offset);
	}
	
	/**
	 * Read a long int from given position
	 * @param offset
	 * @return long data at position
	 */
	public long getDataInt64(int offset) {
		asByteBuffer();
		return dataBuffer.getLong(offset);
	}

	/**
	 * Read a 8-characters length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getDataString8(int offset) {
		return getDataString(offset, 8);
	}

	/**
	 * Read a 32-characters length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getDataString32(int offset) {
		return getDataString(offset, 32);
	}

	/**
	 * Read a 64-characters length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getDataString64(int offset) {
		return getDataString(offset, 64);
	}

	/**
	 * Read a 128-characters length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getDataString128(int offset) {
		return getDataString(offset, 128);
	}

	/**
	 * Read a 256-characters length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getDataString256(int offset) {
		return getDataString(offset, 256);
	}

	/**
	 * Read a 260-characters length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getDataString260(int offset) {
		return getDataString(offset, 260);
	}

	/**
	 * Read a fixed length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @param len string length
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getDataString(int offset, int len) {
		asByteBuffer();
		dataBuffer.position(offset);
		return super.makeString(dataBuffer, len);
	}
	
	/**
	 * Read a variable length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getDataStringV(int offset) {
		asByteBuffer();
		int i = 0;
		for (; dataBuffer.hasRemaining(); i++) {
			if (dataBuffer.get(offset + i) == 0) break;
		}
		// now i is limit
		return super.makeString(dataBuffer, i);
	}

	/**
	 * Read a {@link SimConnectData} data structure from data block at given offset.
	 * Data is read into the given object
	 * @param offset byte offset to start from (relative to the beginning of defined data)
	 * @return the <code>data</code> argument
	 */
	public <T extends SimConnectData> T getData(T data, int offset) {
		asByteBuffer();
		int current = dataBuffer.position();
		dataBuffer.position(offset);
		data.read(dataBuffer);
		// restore old position
		dataBuffer.position(current);
		return data;
	}
	
	/**
	 * Read a {@link InitPosition} data structure from data block at given offset
	 * @param offset byte offset to start from (relative to the beginning of defined data)
	 * @return data structure at offset
	 */
	public InitPosition getInitPosition(int offset) {
		return getData(new InitPosition(), offset);
	}

	/**
	 * Read a {@link MarkerState} data structure from data block at given offset
	 * @param offset byte offset to start from (relative to the beginning of defined data)
	 * @return data structure at offset
	 */
	public MarkerState getMarkerState(int offset) {
		return getData(new MarkerState(), offset);
	}

	/**
	 * Read a {@link Waypoint} data structure from data block at given offset
	 * @param offset byte offset to start from (relative to the beginning of defined data)
	 * @return data structure at offset
	 */
	public Waypoint getWaypoint(int offset) {
		return getData(new Waypoint(), offset);
	}

	/**
	 * Read a {@link LatLonAlt} data structure from data block at given offset
	 * @param offset byte offset to start from (relative to the beginning of defined data)
	 * @return data structure at offset
	 */ 
	public LatLonAlt getLatLonAlt(int offset) {
		return getData(new LatLonAlt(), offset);
	}

	/**
	 * Read a {@link XYZ} data structure from data block at given offset
	 * @param offset byte offset to start from (relative to the beginning of defined data)
	 * @return data structure at offset
	 */
	public XYZ getXYZ(int offset) {
		return getData(new XYZ(), offset);
	}

	/**
	 * Read a float from data block 
	 * @return next float
	 */
	public float getDataFloat32() {
		asByteBuffer();
		return dataBuffer.getFloat();
	}

	/**
	 * Read a double from data block 
	 * @return next double
	 */
	public double getDataFloat64() {
		asByteBuffer();
		return dataBuffer.getDouble();
	}

	/**
	 * Read an int from data block 
	 * @return next int
	 */
	public int getDataInt32() {
		asByteBuffer();
		return dataBuffer.getInt();
	}
	
	/**
	 * Read a long int from data block 
	 * @return next long
	 */
	public long getDataInt64( ) {
		asByteBuffer();
		return dataBuffer.getLong();
	}

	/**
	 * Read a 8 characters strings from data block 
	 * @return next string
	 */
	public String getDataString8( ) {
		return getDataString(8);
	}

	/**
	 * Read a 32 characters strings from data block 
	 * @return next string
	 */
	public String getDataString32( ) {
		return getDataString(32);
	}

	/**
	 * Read a 64 characters strings from data block 
	 * @return next string
	 */
	public String getDataString64( ) {
		return getDataString(64);
	}

	/**
	 * Read a 128 characters strings from data block 
	 * @return next string
	 */
	public String getDataString128( ) {
		return getDataString(128);
	}

	/**
	 * Read a 256 characters strings from data block 
	 * @return next string
	 */
	public String getDataString256() {
		return getDataString(256);
	}
	
	/**
	 * Read a 260 characters strings from data block (typically a path)
	 * @return next string
	 */
	public String getDataString260( ) {
		return getDataString(260);
	}

	/**
	 * Read a fixed length string from the data block
	 * @param len
	 * @return next string
	 */
	public String getDataString(int len) {
		asByteBuffer();
		return super.makeString(dataBuffer, len);
	}
	
	/**
	 * Read a variable string from the data block
	 * @return next string
	 */
	public String getDataStringV() {
		asByteBuffer();
		int i = 0;
		int currentOffset = dataBuffer.position();
		for (; dataBuffer.hasRemaining(); i++) {
			if (dataBuffer.get(currentOffset + i) == 0) break;
		}
		return super.makeString(dataBuffer, i);
	}

	/**
	 * Read an {@link SimConnectData} object from defined data. No object is instantiated since the data
	 * is read into the given argument 
	 * @param data object to write values in
	 * @throws BufferUnderflowException if defined data  buffer is not big enough for this kind of data
	 * @return the <code>data</code>argument
	 */
	public <T extends SimConnectData> T getData(T data) {
		asByteBuffer();
		data.read(dataBuffer);
		return data;
	}
	
	/**
	 * Read an {@link InitPosition} object from defined data.
	 * @throws BufferUnderflowException if defined data  buffer is not big enough for this kind of data
	 * @return An {@link InitPosition} structure
	 */
	public InitPosition getInitPosition() {
		return getData(new InitPosition());
	}

	/**
	 * Read an {@link MarkerState} object from defined data.
	 * @throws BufferUnderflowException if defined data  buffer is not big enough for this kind of data
	 * @return A {@link MarkerState} structure
	 */
	public MarkerState getMarkerState() {
		return getData(new MarkerState());
	}

	/**
	 * Read an {@link Waypoint} object from defined data.
	 * @throws BufferUnderflowException if defined data  buffer is not big enough for this kind of data
	 * @return A {@link Waypoint} structure
	 */
	public Waypoint getWaypoint( ) {
		return getData(new Waypoint() );
	}

	/**
	 * Read an {@link LatLonAlt} object from defined data.
	 * @throws BufferUnderflowException if defined data  buffer is not big enough for this kind of data
	 * @return A {@link LatLonAlt} structure
	 */
	public LatLonAlt getLatLonAlt( ) {
		return getData(new LatLonAlt() );
	}

	/**
	 * Read an {@link XYZ} object from defined data.
	 * @throws BufferUnderflowException if defined data  buffer is not big enough for this kind of data
	 * @return A {@link XYZ} structure
	 */
	public XYZ getXYZ() throws BufferUnderflowException {
		return getData(new XYZ() );
	}
	
	/**
	 * Reset the data definition reader pointer. The next <code>getData</code> call will start
	 * at the beggining of the data block. 
	 *
	 */
	public void reset() {
		asByteBuffer();
		dataBuffer.clear();
	}
	
	/**
	 * Returns true if there is remaining data to read
	 * @return true if there is remaining data
	 */
	public boolean hasRemaining() {
		asByteBuffer();
		return dataBuffer.hasRemaining();
	}
	
	/**
	 * Returns the number of remaining bytes of unread data
	 * @return size of remaining data
	 */
	public int remaining() {
		asByteBuffer();
		return dataBuffer.remaining();
	}

}
