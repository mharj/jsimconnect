package flightsim.simconnect.wrappers;

import flightsim.simconnect.data.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A wrapper around a bytebuffer that support FS types.
 * 
 * @since 0.2
 * @author lc0277
 * 
 *
 */
public class DataWrapper implements DataInput, DataOutput {
	protected ByteBuffer dataBuffer;
	
	/**
	 * Create a wrapper around a buffer of bytes. The buffer position and limit are
	 * used as is
	 * @param bf buffer
	 */
	public DataWrapper(ByteBuffer bf) {
		this.dataBuffer = bf;
	}
	
	/**
	 * Create a datawrapper around fresh blank data of given size
	 * @param size size in bytes
	 */
	public DataWrapper(int size) {
		this(ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN));
	}
	
	/**
	 * For subclasses. Warning: it does not allocate the buffer
	 *
	 */
	protected DataWrapper() {
		
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
	 * Read a float from given position
	 * @param offset
	 * @return float data at position
	 */
	public float getFloat32(int offset) {
		return dataBuffer.getFloat(offset);
	}

	/**
	 * Read a double from given position
	 * @param offset
	 * @return double data at position
	 */
	public double getFloat64(int offset) {
		return dataBuffer.getDouble(offset);
	}

	/**
	 * Read an int from given position
	 * @param offset
	 * @return int data at position
	 */
	public int getInt32(int offset) {
		return dataBuffer.getInt(offset);
	}
	
	/**
	 * Read a long int from given position
	 * @param offset
	 * @return long data at position
	 */
	public long getInt64(int offset) {
		return dataBuffer.getLong(offset);
	}

	/**
	 * Read a 8-characters length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getString8(int offset) {
		return getString(offset, 8);
	}

	/**
	 * Read a 32-characters length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getString32(int offset) {
		return getString(offset, 32);
	}

	/**
	 * Read a 64-characters length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getString64(int offset) {
		return getString(offset, 64);
	}

	/**
	 * Read a 128-characters length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getString128(int offset) {
		return getString(offset, 128);
	}

	/**
	 * Read a 256-characters length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getString256(int offset) {
		return getString(offset, 256);
	}

	/**
	 * Read a 260-characters length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getString260(int offset) {
		return getString(offset, 260);
	}

	/**
	 * Read a fixed length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @param len string length
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getString(int offset, int len) {
		dataBuffer.position(offset);
		return makeString(dataBuffer, len);
	}
	
	/**
	 * Read a variable length string in defined data block starting at given position
	 * @param offset byte position to start reading from
	 * @throws BufferUnderflowException
	 * @return string data at position
	 */
	public String getStringV(int offset) {
		int i = 0;
		for (; dataBuffer.hasRemaining(); i++) {
			if (dataBuffer.get(offset + i) == 0) break;
		}
		// now i is limit
		return makeString(dataBuffer, i);
	}

	/**
	 * Read a {@link SimConnectData} data structure from data block at given offset.
	 * Data is read into the given object
	 * @param offset byte offset to start from (relative to the beginning of defined data)
	 * @return the <code>data</code> argument
	 */
	public <T extends SimConnectData> T getData(T data, int offset) {
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
	public float getFloat32() {
		return dataBuffer.getFloat();
	}

	/**
	 * Read a double from data block 
	 * @return next double
	 */
	public double getFloat64() {
		return dataBuffer.getDouble();
	}

	/**
	 * Read an int from data block 
	 * @return next int
	 */
	public int getInt32() {
		return dataBuffer.getInt();
	}
	
	/**
	 * Read a long int from data block 
	 * @return next long
	 */
	public long getInt64( ) {
		return dataBuffer.getLong();
	}

	/**
	 * Read a 8 characters strings from data block 
	 * @return next string
	 */
	public String getString8( ) {
		return getString(8);
	}

	/**
	 * Read a 32 characters strings from data block 
	 * @return next string
	 */
	public String getString32( ) {
		return getString(32);
	}

	/**
	 * Read a 64 characters strings from data block 
	 * @return next string
	 */
	public String getString64( ) {
		return getString(64);
	}

	/**
	 * Read a 128 characters strings from data block 
	 * @return next string
	 */
	public String getString128( ) {
		return getString(128);
	}

	/**
	 * Read a 256 characters strings from data block 
	 * @return next string
	 */
	public String getString256() {
		return getString(256);
	}
	
	/**
	 * Read a 260 characters strings from data block (typically a path)
	 * @return next string
	 */
	public String getString260( ) {
		return getString(260);
	}

	/**
	 * Read a fixed length string from the data block
	 * @param len
	 * @return next string
	 */
	public String getString(int len) {
		return makeString(dataBuffer, len);
	}
	
	/**
	 * Read a variable string from the data block
	 * @return next string
	 */
	public String getStringV() {
		int i = 0;
		int currentOffset = dataBuffer.position();
		for (; dataBuffer.hasRemaining(); i++) {
			if (dataBuffer.get(currentOffset + i) == 0) break;
		}
		return makeString(dataBuffer, i);
	}

	/**
	 * Read an {@link SimConnectData} object from defined data. No object is instantiated since the data
	 * is read into the given argument 
	 * @param data object to write values in
	 * @throws BufferUnderflowException if defined data  buffer is not big enough for this kind of data
	 * @return the <code>data</code>argument
	 */
	public <T extends SimConnectData> T getData(T data) {
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
		dataBuffer.clear();
	}
	
	/**
	 * Returns true if there is remaining data to read
	 * @return true if there is remaining data
	 */
	public boolean hasRemaining() {
		return dataBuffer.hasRemaining();
	}
	
	/**
	 * Returns the number of remaining bytes of unread data
	 * @return size of remaining data
	 */
	public int remaining() {
		return dataBuffer.remaining();
	}


	/**
	 * put exactyl <code>len</code> bytes of string into buffer, padding
	 * it if needed
	 * @param s string to write
	 * @param len len of string field
	 */
	public void putString(String s, int len) {
		byte[] b  = s.getBytes();
		dataBuffer.put(b, 0, Math.min(len, b.length));
		for (int i = b.length; i < len; i++) dataBuffer.put((byte) 0);
	}

	/**
	 * put exactyl <code>len</code> bytes of string into buffer, padding
	 * it if needed.
	 * @param offset offset in buffer to start writing from
	 * @param s string to write
	 * @param len len of string field
	 */
	public void putString(int offset, String s, int len) {
		int sp = dataBuffer.position();
		dataBuffer.position(offset);
		byte[] b  = s.getBytes();
		dataBuffer.put(b, 0, Math.min(len, b.length));
		for (int i = b.length; i < len; i++) dataBuffer.put((byte) 0);
		dataBuffer.position(sp);
	}

	/**
	 * write value to buffer at current position and increment position
	 * @param value value to write
	 */
	public void putFloat64(double value) {
		dataBuffer.putDouble(value);
	}

	/**
	 * Write value at given position
	 * @param index
	 * @param value
	 */
	public void putFloat64(int index, double value) {
		dataBuffer.putDouble(index, value);
	}

	/**
	 * write value to buffer at current position and increment position
	 * @param value value to write
	 */
	public void putFloat32(float value) {
		dataBuffer.putFloat(value);
	}

	/**
	 * Write value at given position
	 * @param index
	 * @param value
	 */
	public void putFloat32(int index, float value) {
		dataBuffer.putFloat(index, value);
	}

	/**
	 * Write value at given position
	 * @param index
	 * @param value
	 */
	public void putInt32(int index, int value) {
		dataBuffer.putInt(index, value);
	}

	/**
	 * write value to buffer at current position and increment position
	 * @param value value to write
	 */
	public void putInt32(int value) {
		dataBuffer.putInt(value);
	}

	/**
	 * Write value at given position
	 * @param index
	 * @param value
	 */
	public void putInt64(int index, long value) {
		dataBuffer.putLong(index, value);
	}

	/**
	 * write value to buffer at current position and increment position by the size of data
	 * @param value string to write
	 */
	public void putInt64(long value) {
		dataBuffer.putLong(value);
	}
	
	/**
	 * write value to buffer at current position and increment position by the size of data
	 * @param s string to write
	 */
	public void putString8(String s) {
		putString(s, 8);
	}

	/**
	 * write value to buffer at current position and increment position by the size of data
	 * @param s string to write
	 */
	public void putString32(String s) {
		putString(s, 32);
	}

	/**
	 * write value to buffer at current position and increment position by the size of data
	 * @param s string to write
	 */
	public void putString64(String s) {
		putString(s, 64);
	}

	/**
	 * write value to buffer at current position and increment position by the size of data
	 * @param s string to write
	 */
	public void putString128(String s) {
		putString(s, 128);
	}

	/**
	 * write value to buffer at current position and increment position by the size of data
	 * @param s string to write
	 */
	public void putString256(String s) {
		putString(s, 256);
	}

	/**
	 * write value to buffer at current position and increment position by the size of data
	 * @param s string to write
	 */
	public void putString260(String s) {
		putString(s, 260);
	}

	/**
	 * write value to buffer at given position and increment position by the size of data
	 * @param offset index to start writing from
	 * @param s string to write
	 */
	public void putString8(int offset, String s) {
		putString(s, 8);
	}

	/**
	 * write value to buffer at given position and increment position by the size of data
	 * @param offset index to start writing from
	 * @param s string to write
	 */
	public void putString32(int offset, String s) {
		putString(s, 32);
	}

	/**
	 * write value to buffer at given position and increment position by the size of data
	 * @param offset index to start writing from
	 * @param s string to write
	 */
	public void putString64(int offset, String s) {
		putString(s, 64);
	}

	/**
	 * write value to buffer at given position and increment position by the size of data
	 * @param offset index to start writing from
	 * @param s string to write
	 */
	public void putString128(int offset, String s) {
		putString(s, 128);
	}

	/**
	 * write value to buffer at given position and increment position by the size of data
	 * @param offset index to start writing from
	 * @param s string to write
	 */
	public void putString256(int offset, String s) {
		putString(s, 256);
	}

	/**
	 * write value to buffer at given position and increment position by the size of data
	 * @param offset index to start writing from
	 * @param s string to write
	 */
	public void putString260(int offset, String s) {
		putString(s, 260);
	}

	/**
	 * Write string to buffer, String is written entirely and a final null character is added
	 * @param s string to write
	 * @return length wrote to buffer
	 */
	public int putStringV(String s) {
		byte b[] = s.getBytes();
		dataBuffer.put(b);
		dataBuffer.put((byte) 0);
		return b.length + 1;
	}

	/**
	 * Write string to buffer, String is written entirely and a final null character is added
	 * @param s string to write
	 * @param offset offset from where to start writing
	 * @return length wrote to buffer
	 */
	public int putStringV(int offset, String s) {
		byte b[] = s.getBytes();
		int sp = dataBuffer.position();
		dataBuffer.position(offset);
		dataBuffer.put(b);
		dataBuffer.put((byte) 0);
		dataBuffer.position(sp);
		return b.length + 1;
	}
	
	/**
	 * Write data to buffer at given position
	 * @param <T> a type of SimConnectData (latlonalt, etc or your own type)
	 * @param offset offset from where to start writing
	 * @param data data to write
	 */
	public  <T extends SimConnectData> void putData(int offset, T data) {
		int sp = dataBuffer.position();
		dataBuffer.position(offset);
		data.write(dataBuffer);
		dataBuffer.position(sp);
	}

	/**
	 * Write data to buffer at current position and increment position
	 * @param <T> a type of SimConnectData (latlonalt, etc or your own type)
	 * @param data data to write
	 */
	public  <T extends SimConnectData> void putData(T data) {
		data.write(dataBuffer);
	}
	
	/**
	 * Write an array of bytes
	 * @param b byte array
	 * @since 0.4
	 */
	public void putData(byte[] b) {
		dataBuffer.put(b);
	}
	
	/**
	 * Write data to buffer at current position and increment position
	 * @param data data to write
	 */
	public void putLatLonAlt(LatLonAlt data) {
		putData(data);
	}

	/**
	 * Write data to buffer at current position and increment position
	 * @param data data to write
	 */
	public void putInitPosition(InitPosition data) {
		putData(data);
	}

	/**
	 * Write data to buffer at current position and increment position
	 * @param data data to write
	 */
	public void putWaypoint(Waypoint data) {
		putData(data);
	}

	/**
	 * Write data to buffer at current position and increment position
	 * @param data data to write
	 */
	public void putMarkerState(MarkerState data) {
		putData(data);
	}

	/**
	 * Write data to buffer at current position and increment position
	 * @param data data to write
	 */
	public void putXYZ(XYZ data) {
		putData(data);
	}

	
	/**
	 * Write data to buffer at given position
	 * @param offset index
	 * @param data data to write
	 */
	public void putLatLonAlt(int offset, LatLonAlt data) {
		putData(offset, data);
	}

	/**
	 * Write data to buffer at given position
	 * @param offset index
	 * @param data data to write
	 */
	public void putInitPosition(int offset, InitPosition data) {
		putData(offset, data);
	}

	/**
	 * Write data to buffer at given position
	 * @param offset index
	 * @param data data to write
	 */
	public void putWaypoint(int offset, Waypoint data) {
		putData(offset, data);
	}

	/**
	 * Write data to buffer at given position
	 * @param offset index
	 * @param data data to write
	 */
	public void putMarkerState(int offset, MarkerState data) {
		putData(offset, data);
	}

	/**
	 * Write data to buffer at given position
	 * @param offset index
	 * @param data data to write
	 */
	public void putXYZ(int offset, XYZ data) {
		putData(offset, data);
	}

	/**
	 * Returns the data buffer as a byte array
	 * @return byte array backing the data
	 */
	public byte[] bytes() {
		return dataBuffer.array();
	}

	/**
	 * Returns the bytebuffer used to store data, usually before writing
	 * 
	 * @return buffer
	 */
	public ByteBuffer getBuffer() {
		dataBuffer.flip();
		return dataBuffer;
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 * @return read data
	 */
	public boolean readBoolean() throws IOException {
		return dataBuffer.getInt() != 0;
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 * @return read data
	 */
	public byte readByte() throws IOException {
		return dataBuffer.get();
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 * @return read data
	 */
	public char readChar() throws IOException {
		return dataBuffer.getChar();
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 * @return read data
	 */
	public double readDouble() throws IOException {
		return dataBuffer.getDouble();
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 * @return read data
	 */
	public float readFloat() throws IOException {
		return dataBuffer.getFloat();
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 */
	public void readFully(byte[] b) throws IOException {
		dataBuffer.get(b);
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 */
	public void readFully(byte[] b, int off, int len) throws IOException {
		dataBuffer.get(b, off, len);
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 * @return read data
	 */
	public int readInt() throws IOException {
		return dataBuffer.getInt();
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 * @return read data
	 */
	public String readLine() throws IOException {
		return getStringV();
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 * @return read data
	 */
	public long readLong() throws IOException {
		return getInt64();
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 * @return read data
	 */
	public short readShort() throws IOException {
		return dataBuffer.getShort();
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 * @return read data
	 */
	public String readUTF() throws IOException {
		return readLine();
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 * @return read data
	 */
	public int readUnsignedByte() throws IOException {
		return dataBuffer.get() & 0xff;
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 * @return read data
	 */
	public int readUnsignedShort() throws IOException {
		return dataBuffer.getShort() & 0xffff;
	}

	/**
	 * Convenience read method from DataInput
	 * @since 0.3
	 * @return number of bytes skipped
	 */
	public int skipBytes(int n) throws IOException {
		dataBuffer.position(dataBuffer.position() + n);
		return n;
	}

	/**
	 * Convenience write method from DataOutput
	 * @since 0.3
	 * @param b data to write
	 */
	public void write(int b) throws IOException {
		dataBuffer.put((byte)b);
	}

	/**
	 * Convenience write method from DataOutput
	 * @since 0.3
	 * @param b data to write
	 */
	public void write(byte[] b) throws IOException {
		dataBuffer.put(b);
	}

	/**
	 * Convenience write method from DataOutput
	 * @since 0.3
	 * @param b data to write
	 */
	public void write(byte[] b, int off, int len) throws IOException {
		dataBuffer.put(b, off, len);
	}

	/**
	 * Convenience write method from DataOutput
	 * @since 0.3
	 * @param v data to write
	 */
	public void writeBoolean(boolean v) throws IOException {
		dataBuffer.putInt(v ? 1 : 0);
	}

	/**
	 * Convenience write method from DataOutput
	 * @since 0.3
	 * @param v data to write
	 */
	public void writeByte(int v) throws IOException {
		dataBuffer.putInt(v);
	}

	/**
	 * Convenience write method from DataOutput
	 * @since 0.3
	 * @param s data to write
	 */
	public void writeBytes(String s) throws IOException {
		dataBuffer.put(s.getBytes());
		
	}

	/**
	 * Convenience write method from DataOutput
	 * @since 0.3
	 * @param v data to write
	 */
	public void writeChar(int v) throws IOException {
		dataBuffer.putChar((char)v);
	}

	/**
	 * Convenience write method from DataOutput
	 * @since 0.3
	 * @param s data to write
	 */
	public void writeChars(String s) throws IOException {
		char[] sc = s.toCharArray();
		for (char c : sc) writeChar(c);
	}

	/**
	 * Convenience write method from DataOutput
	 * @since 0.3
	 * @param v data to write
	 */
	public void writeDouble(double v) throws IOException {
		dataBuffer.putDouble(v);
	}

	/**
	 * Convenience write method from DataOutput
	 * @since 0.3
	 * @param v data to write
	 */
	public void writeFloat(float v) throws IOException {
		dataBuffer.putFloat(v);
	}

	/**
	 * Convenience write method from DataOutput
	 * @since 0.3
	 * @param v data to write
	 */
	public void writeInt(int v) throws IOException {
		dataBuffer.putInt(v);
	}

	/**
	 * Convenience write method from DataOutput
	 * @since 0.3
	 * @param v data to write
	 */
	public void writeLong(long v) throws IOException {
		dataBuffer.putLong(v);
	}

	/**
	 * Convenience write method from DataOutput
	 * @since 0.3
	 * @param v data to write
	 */
	public void writeShort(int v) throws IOException {
		dataBuffer.putShort((short)v);
	}

	/**
	 * Convenience write method from DataOutput
	 * @since 0.3
	 * @param str data to write
	 */
	public void writeUTF(String str) throws IOException {
		writeChars(str);
	}
	
}
