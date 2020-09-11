package flightsim.simconnect.wrappers;

import java.text.MessageFormat;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


import flightsim.simconnect.*;
import flightsim.simconnect.data.*;
import flightsim.simconnect.recv.*;

/**
 * A data wrapper that memorizes the position of each variable you define.
 * It handles all simconnect communication.
 * <br/>
 * 
<pre>
SimConnect simconnect = new SimConnect(...);		// construct SC connection instance
DataDefinitionWrapper ddw = new DataDefinitionWrapper(simconnect);

ddw.addToDefinition("Plane Altitude", "feet", SimConnectDataType.FLOAT32);
// add other definitions. You don't need to add the data to datadef yourself

// later fill the wrapper with received data (from a simobjectdata event)
ddw.fillDataFrom(recvData);		// recvData is an instance of RecvsimObjectData
							// will throw an exception if recvData is not
							// of the correct request ID
							
float f = ddw.getFloat32("Plane Altitude");		// use the variable name

// can also be used to set data
ddw.setFloat32("Plane altitude", 30000.0f);
ddw.setDataOnSimObject(objectid);

</pre>
 * @author lc0277
 * @since 0.3
 * 
 */
public class DataDefinitionWrapper extends DataWrapper implements Iterable<Object> {
	private class DataDef implements Comparable<DataDef> {
		String name;
		int offset;
		SimConnectDataType type;
		public int compareTo(DataDef o) {
			return offset - o.offset;
		}
		DataDef(String name, int offset, SimConnectDataType type) {
			this.name = name;
			this.offset = offset;
			this.type = type;
		}
	}
	
	private final List<DataDef> dataDefs = new ArrayList<DataDef>();
	private final SimConnect sc;
	private int totalSize = 0;
	private final int datadefid;
	
	/**
	 * Construct a blank data definition wrapper with a predefined data definition ID.
	 * No data is initialized see {@link #fillDataFrom(RecvSimObjectData)} or {@link #fillEmptyData()}
	 * to initialize current data.
	 * <br/>
	 * The given data definition is cleared.
	 * 
	 * @param sc SimConnect instance
	 * @param dataDefinitionID data definition ID
	 * @see #getDataDefinitionID()
	 * @throws IOException SimConnect Client IO errors
	 */
	public DataDefinitionWrapper(SimConnect sc, int dataDefinitionID) throws IOException {
		this.sc = sc;
		this.datadefid = dataDefinitionID;
		clearDataDefinition();
	}
	
	/**
	 * Construct a blank data definition wrapper with a random definition ID (not always unused)
	 * No data is initialized see {@link #fillDataFrom(RecvSimObjectData)} or {@link #fillEmptyData()}
	 * to initialize current data.
	 * <br/>
	 * 
	 * @param sc SimConnect instance
	 * @see #getDataDefinitionID()
	 * @throws IOException SimConnect Client IO errors
	 */
	public DataDefinitionWrapper(SimConnect sc) throws IOException {
		this(sc, (int)(Math.random() * 8192));
	}
	
	/**
	 * Returns the data definition ID used by this wrapper
	 * @return data definition ID used by this wrapped
	 */
	public int getDataDefinitionID() {
		return datadefid;
	}
	
	/**
	 * Add a named variable to this data definition. The request is forwarded to
	 * the wrapped simconnect instance.
	 * 
	 * @param variable FS variable name
	 * @param units FS variable units
	 * @param dataType data type
	 * @throws IOException SimConnect client IO errors
	 * @throws IllegalArgumentException if the variable is not fixed size
	 */
	public void addToDataDefinition(String variable, String units, SimConnectDataType dataType) throws IOException {
		if (dataType.size() == -1) 
			throw new IllegalArgumentException(Messages.getString("DataDefinitionWrapper.0")); //$NON-NLS-1$
		synchronized (this) {
			sc.addToDataDefinition(datadefid, variable, units, dataType);
			DataDef dd = new DataDef(variable, totalSize, dataType);
			dataDefs.add(dd);
			this.totalSize += dataType.size();
		}
	}
	
	/**
	 * Clear the current data definition
	 * @throws IOException SimConnect client IO errors
	 */
	public void clearDataDefinition() throws IOException {
		dataBuffer = null;
		synchronized (this) {
			sc.clearDataDefinition(datadefid);
			dataDefs.clear();
		}
	}
	
	/**
	 * Set data on a specified sim object using the current values of this
	 * data definition set.
	 * <br/>
	 * Data must be initialized before, see {@link #fillEmptyData()}
	 * @param objectId sim object id
	 * @see #fillEmptyData()
	 * @throws IOException SimConnect client IO errors
	 * @throws IllegalDataDefinition if not data is currently available
	 */
	public void setDataOnSimObject(int objectId) throws IOException, IllegalDataDefinition {
		if (dataBuffer == null) {
			throw new IllegalDataDefinition(Messages.getString("DataDefinitionWrapper.1")); //$NON-NLS-1$
		}
		sc.setDataOnSimObject(datadefid, objectId, false, 1, bytes());
	}
	
	/**
	 * Set client data using the current data definition
	 * <br/>
	 * Data must be initialized before, see {@link #fillEmptyData()}
	 * @param clientDataId client data area ID
	 * @throws IOException SimConnect client IO errors
	 * @throws IllegalDataDefinition if not data is currently available
	 */
	public void setClientData(int clientDataId) throws IOException, IllegalDataDefinition {
		if (dataBuffer == null) {
			throw new IllegalDataDefinition(Messages.getString("DataDefinitionWrapper.2")); //$NON-NLS-1$
		}
		sc.setClientData(clientDataId, datadefid, 0, 1, totalSize, bytes());
	}
	
	/**
	 * Returns the offset of a variable
	 * @param variable variable name
	 * @return offset
	 * @throws IllegalDataDefinition if variable is not in the data definition
	 */
	int getOffset(String variable) throws IllegalDataDefinition {
		for (DataDef dd : dataDefs) {
			if (dd.name.equalsIgnoreCase(variable)) return dd.offset;
		}
		throw new IllegalDataDefinition(MessageFormat.format(Messages.getString("DataDefinitionWrapper.3"), variable)); //$NON-NLS-1$
	}
	
	/**
	 * Fill the current data definition with empty data. Must be called prior 
	 * {@link #setClientData(int)} or {@link #setDataOnSimObject(int)} to
	 * initialize the data area.
	 *
	 */
	public void fillEmptyData() {
		synchronized (this) {
			byte[] rawData = new byte[totalSize];
			super.dataBuffer = ByteBuffer.wrap(rawData);
			super.dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
	}
	
	/**
	 * Fill the current data definition with data from a received packet.
	 * Must be called before all <code>get*</code> functions can be used.
	 *  
	 * @param simObjData Received packet data
	 * @throws IllegalDataDefinition If packet is not from the current data definition
	 */
	public void fillDataFrom(RecvSimObjectData simObjData) throws IllegalDataDefinition {
		if (simObjData.getDefineID() != datadefid) {
			throw new IllegalDataDefinition(MessageFormat.format(Messages.getString("DataDefinitionWrapper.4"), datadefid, simObjData.getDefineID())); //$NON-NLS-1$
		}
		
		//
		// construct data buffer
		//
		synchronized (this) {
			byte[] rawData = simObjData.getData();
			if (rawData.length != totalSize) {
				throw new IllegalDataDefinition(MessageFormat.format(Messages.getString("DataDefinitionWrapper.5"), rawData.length)); //$NON-NLS-1$
			}
			super.dataBuffer = ByteBuffer.wrap(rawData);
			super.dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
	}

	private class DataIterator implements Iterator<Object> {
		private Iterator<DataDef> ddIterator;

		public boolean hasNext() {
			return ddIterator.hasNext();
		}

		public Object next() {
			DataDef dd = ddIterator.next();
			switch (dd.type) {
			case FLOAT32:
				return getFloat32(dd.offset);
			case FLOAT64:
				return getFloat64(dd.offset);
			case INITPOSITION:
				return getInitPosition(dd.offset);
			case INT32:
				return getInt32(dd.offset);
			case INT64:
				return getInt64(dd.offset);
			case LATLONALT:
				return getLatLonAlt(dd.offset);
			case MARKERSTATE:
				return getMarkerState(dd.offset);
			case STRING128:
				return getString128(dd.offset);
			case STRING256:
				return getString256(dd.offset);
			case STRING260:
				return getString260(dd.offset);
			case STRING32:
				return getString32(dd.offset);
			case STRING64:
				return getString64(dd.offset);
			case STRING8:
				return getString8(dd.offset);
			case WAYPOINT:
				return getWaypoint(dd.offset);
			case XYZ:
				return getXYZ(dd.offset);
			}
			return null;
		}

		public void remove() {
			throw new UnsupportedOperationException("remove"); //$NON-NLS-1$
		}
		
	}

	/**
	 * Returns an iterator over each variable in the data definition, in
	 * the order they have been added with {@link #addToDataDefinition(String, String, SimConnectDataType)}
	 */
	public Iterator<Object> iterator() {
		return new DataIterator();
	}

	public <T extends SimConnectData> T getData(T data, String var) throws IllegalDataDefinition {
		return super.getData(data, getOffset(var));
	}

	public float getFloat32(String var) throws IllegalDataDefinition {
		return super.getFloat32(getOffset(var));
	}

	public double getFloat64(String var) throws IllegalDataDefinition {
		return super.getFloat64(getOffset(var));
	}

	public InitPosition getInitPosition(String var) throws IllegalDataDefinition {
		return super.getInitPosition(getOffset(var));
	}

	public int getInt32(String var) throws IllegalDataDefinition {
		return super.getInt32(getOffset(var));
	}

	public long getInt64(String var) throws IllegalDataDefinition {
		return super.getInt64(getOffset(var));
	}

	public LatLonAlt getLatLonAlt(String var) throws IllegalDataDefinition {
		return super.getLatLonAlt(getOffset(var));
	}

	public MarkerState getMarkerState(String var) throws IllegalDataDefinition {
		return super.getMarkerState(getOffset(var));
	}

	public String getString128(String var) throws IllegalDataDefinition {
		return super.getString128(getOffset(var));
	}

	public String getString256(String var)  throws IllegalDataDefinition{
		return super.getString256(getOffset(var));
	}

	public String getString260(String var) throws IllegalDataDefinition{
		return super.getString260(getOffset(var));
	}

	public String getString32(String var) throws IllegalDataDefinition {
		return super.getString32(getOffset(var));
	}

	public String getString64(String var) throws IllegalDataDefinition {
		return super.getString64(getOffset(var));
	}

	public String getString8(String var) throws IllegalDataDefinition {
		return super.getString8(getOffset(var));
	}

	public Waypoint getWaypoint(String var) throws IllegalDataDefinition {
		return super.getWaypoint(getOffset(var));
	}

	public XYZ getXYZ(String var) throws IllegalDataDefinition {
		return super.getXYZ(getOffset(var));
	}

	public void putFloat32(String var, float value) throws IllegalDataDefinition{
		super.putFloat32(getOffset(var), value);
	}

	public void putFloat64(String var, double value) throws IllegalDataDefinition {
		super.putFloat64(getOffset(var), value);
	}

	public void putInitPosition(String var, InitPosition data)  throws IllegalDataDefinition{
		super.putInitPosition(getOffset(var), data);
	}

	public void putInt32(String var, int value) throws IllegalDataDefinition {
		super.putInt32(getOffset(var), value);
	}

	public void putInt64(String var, long value) throws IllegalDataDefinition {
		super.putInt64(getOffset(var), value);
	}

	public void putLatLonAlt(String var, LatLonAlt data)  throws IllegalDataDefinition{
		super.putLatLonAlt(getOffset(var), data);
	}

	public void putMarkerState(String var, MarkerState data) throws IllegalDataDefinition {
		super.putMarkerState(getOffset(var), data);
	}

	public void putString128(String var, String s)  throws IllegalDataDefinition{
		super.putString128(getOffset(var), s);
	}

	public void putString256(String var, String s)  throws IllegalDataDefinition{
		super.putString256(getOffset(var), s);
	}

	public void putString260(String var, String s) throws IllegalDataDefinition {
		super.putString260(getOffset(var), s);
	}

	public void putString32(String var, String s) throws IllegalDataDefinition {
		super.putString32(getOffset(var), s);
	}

	public void putString64(String var, String s) throws IllegalDataDefinition {
		super.putString64(getOffset(var), s);
	}

	public void putString8(String var, String s) throws IllegalDataDefinition {
		super.putString8(getOffset(var), s);
	}

	public void putWaypoint(String var, Waypoint data)  throws IllegalDataDefinition{
		super.putWaypoint(getOffset(var), data);
	}

	public void putXYZ(String var, XYZ data) throws IllegalDataDefinition {
		super.putXYZ(getOffset(var), data);
	}
	
	
	
}
