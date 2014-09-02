package flightsim.simconnect.wrappers;

import java.nio.ByteBuffer;

import flightsim.simconnect.Messages;
import flightsim.simconnect.data.SimConnectData;

/**
 * An instance of a GUID. 
 * @author lc0277
 * @since 0.2
 *
 */
public class GUID implements SimConnectData, Comparable<GUID> {
	protected final byte[] data;	// stored in LE format
	
	/**
	 * Create a new blank guid (initialized to zeros)
	 * @since 0.3
	 */
	public GUID() {
		this.data = new byte[16];
	}
	
	/**
	 * Create a new GUID from the raw guid data, in little-endian format.
	 * Suitable for reading over network or files.
	 * A copy is made
	 * @param data guid data
	 */
	public GUID(byte[] data) {
		this(data, 0);
		
	}
	
	/**
	 * Create a new GUID from raw data, in LE format.
	 * @param data	guid data
	 * @param offset offset to start from
	 * @since 0.3
	 */
	public GUID(byte[] data, int offset) {
		this.data = new byte[16];
		System.arraycopy(data, offset, this.data, 0, 16);
	}
	
	/**
	 * Create a new GUID instance with class C struct initialisation
	 * @param arg1 first member of the GUID structure
	 * @param arg2 second member of the GUID structure
	 * @param arg3 third member of the GUID structure
	 * @param arg4 last member of the GUID structure (byte array)
	 * @see GUID#decode(int, short, short, byte[])
	 * @throws IllegalArgumentException if <code>arg4</code> is not of length 8
	 */
	public GUID (int arg1, short arg2, short arg3, byte[] arg4) throws IllegalArgumentException {
		data = decode(arg1, arg2, arg3, arg4);
	}
	
	/**
	 * Create a new GUID by parsing the given string representing the
	 * GUID in registry format
	 * @throws IllegalArgumentException illegal string format
	 * @throws NullPointerException  <code>s</code> is null
	 * @param s
	 * @see GUID#decodeRegistry(String)
	 */
	public GUID(String s) {
		data = decodeRegistry(s);
	}
	
	/**
	 * 
	 * Decode a registry format string and returns the byte array
	 * representing the GUID. the byte array is in <b>Little-Endian</b>
	 * format.
	 * @param s registry string
	 * @return guid
	 * @see GUID#GUID(String)
	 */
	public static byte[] decodeRegistry(String s) {
		// skip begin and end brackets
		s = s.trim().toLowerCase().substring(1, s.length() - 1);
		if (s.length() != 36) throw new IllegalArgumentException(Messages.getString("GUID.0")); //$NON-NLS-1$
		String[] parts = s.split("-"); //$NON-NLS-1$
		if (parts.length != 5) throw new IllegalArgumentException(Messages.getString("GUID.2")); //$NON-NLS-1$
		
		byte[] data = new byte[16];
		try {
			// avoid overflows
			int a = (int)(Long.parseLong(parts[0], 16) & 0xffffffff);
			data[0] = (byte) ((a) & 0xFF);
			data[1] = (byte) ((a >> 8) & 0xFF);
			data[2] = (byte) ((a >> 16) & 0xFF);
			data[3] = (byte) ((a >> 24) & 0xFF);
			a = Integer.parseInt(parts[1], 16);
			data[4] = (byte) ((a) & 0xFF);
			data[5] = (byte) ((a >> 8) & 0xFF);
			a = Integer.parseInt(parts[2], 16);
			data[6] = (byte) ((a) & 0xFF);
			data[7] = (byte) ((a >> 8) & 0xFF);
			a = Integer.parseInt(parts[3], 16);
			data[8] = (byte) ((a >> 8) & 0xFF);	// different order
			data[9] = (byte) ((a) & 0xFF);
			for (int i = 0, j = 10; i < parts[4].length(); i += 2, j++) {
				data[j] = (byte) (Integer.parseInt(
						parts[4].substring(i, i+2), 16) & 0xFF);
			}
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(nfe);
		}
		return data;
	}
	
	/**
	 * Decode a GUID in old format, typically the one formatted 
	 * with %0.8x%0.8x%0.8x, ie the guid read as 4 LE integers
	 * @param s string format
	 * @return guid data
	 */
	public static byte[] decodeOldRegistry(String s) {
		byte[] data = new byte[16];
		for (int i = 0; i < 4; i++) {
			// avoid overflow with int
			int v = (int)(Long.parseLong(s.substring(8*i, 8*i+8), 16) & 0xffffffff);
			data[4*i+3] = (byte)((v >> 24) & 0xff); 
			data[4*i+2] = (byte)((v >> 16) & 0xff); 
			data[4*i+1] = (byte)((v >> 8) & 0xff); 
			data[4*i] = (byte)((v) & 0xff); 
		}
		return data;
	}
	
	/**
	 * Create a guid from the members of the C struct
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @param arg4
	 * @return guid
	 * @see GUID#GUID(int, short, short, byte[])
	 */
	public static byte[] decode(int arg1, short arg2, short arg3, byte[] arg4) {
		if (arg4 == null || arg4.length != 8) 
			throw new IllegalArgumentException(Messages.getString("GUID.3")); //$NON-NLS-1$
		
		byte[] data = new byte[16];
		data[3] = (byte) ((arg1 >> 24) & 0xFF);
		data[2] = (byte) ((arg1 >> 16) & 0xFF);
		data[1] = (byte) ((arg1 >> 8) & 0xFF);
		data[0] = (byte) ((arg1) & 0xFF);
		data[5] = (byte) ((arg2 >> 8) & 0xFF);
		data[4] = (byte) ((arg2) & 0xFF);
		data[7] = (byte) ((arg3 >> 8) & 0xFF);
		data[6] = (byte) ((arg3) & 0xFF);
		// faster
		System.arraycopy(arg4, 0, data, 8, 8);
		return data;
	}

	/**
	 * Parse a GUID from its string representation in registry format
	 * @param s registry format
	 * @return guid
	 */
	public static GUID parseGUID(String s) {
		return new GUID(s);
	}
	
	/**
	 * Returns the string representation of this GUID. The format
	 * is registry {xxxxxxxx-xxxx-...xxxx}
	 */
	@Override
	public String toString() {
		StringBuffer sgb = new StringBuffer(""); //$NON-NLS-1$
		sgb.append('{');
		if ((data[3] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[3] & 0xff));
		if ((data[2] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[2] & 0xff));
		if ((data[1] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[1] & 0xff));
		if ((data[0] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[0] & 0xff));
		sgb.append('-');
		if ((data[5] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[5] & 0xff));
		if ((data[4] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[4] & 0xff));
		sgb.append('-');
		if ((data[7] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[7] & 0xff));
		if ((data[6] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[6] & 0xff));
		sgb.append('-');
		if ((data[8] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[8] & 0xff));
		if ((data[9] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[9] & 0xff));
		sgb.append('-');
		if ((data[10] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[10] & 0xff));
		if ((data[11] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[11] & 0xff));
		if ((data[12] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[12] & 0xff));
		if ((data[13] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[13] & 0xff));
		if ((data[14] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[14] & 0xff));
		if ((data[15] & 0xff) < 0x10) sgb.append('0');
		sgb.append(Integer.toHexString(data[15] & 0xff));
		sgb.append('}');
		return sgb.toString().toUpperCase();
	}
	

	@Override
	public int hashCode() {
		int h = 0;
		for (int i = 0; i < 16; i++) {
			h = 31*h + data[i]; 
		}
		return h;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GUID) {
			GUID g = (GUID) obj;
			for (int i = 0; i < 16; i++) {
				if (data[i] != g.data[i]) return false;
			}
			return true;
		}
		return false;
	}
	
	public int compareTo(GUID g) {
		// WTF!$@
		return toString().compareToIgnoreCase(g.toString());
	}
	
	public void read(ByteBuffer buffer) {
		// simple
		buffer.get(data);
	}


	public void write(ByteBuffer buffer) {
		buffer.put(data);
	}
	
	/**
	 * Returns the byte representation of this GUID in little endian.
	 * Suitable for writing/reading over network or in files
	 * @return guid data (LE)
	 */
	public byte[] getData() {
		return data;
	}
	
	
	public static void main(String[] args) {
		GUID g = new GUID(GUID.decodeOldRegistry("384058AD4BCA29522AF62095A6DB2206")); //$NON-NLS-1$
		System.out.println(g);
		GUID g2 = new GUID("{384058ad-2952-4bca-9520-f62a0622dba6}"); //$NON-NLS-1$
		System.out.println(g2);
		System.out.println(g.equals(g2));
		for (int i = 0; i < 16; i++) System.out.println(Integer.toHexString(g.data[i] & 0xff));
	}

}
