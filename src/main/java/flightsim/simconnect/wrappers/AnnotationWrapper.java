package flightsim.simconnect.wrappers;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import flightsim.simconnect.*;
import flightsim.simconnect.data.*;
import flightsim.simconnect.recv.*;

/**
 * <b>AnnotationWrapper</b> is an helper class to automatically build
 * definitions and handles wrapping/unwrapping mapping FS data to Java class.
 * It is a per-simconnect session object, which means that you'll have to 
 * create and initialize it each time you connect or reconnect to FS. 
 * <p>
 * Data classes (usually POJO beans) must be registered prior to use, with
 * {@link #registerClass(Class)}. Then the class can be used to build request
 * with {@link #requestSimObjectData(Class, int, SimConnectPeriod, boolean)}.
 * The <code>AnnotationWrapper</code> can create objects using {@link #unwrap(RecvSimObjectData)}
 * from a {@link RecvSimObjectData} response packets. Don't try to alter the receive buffer
 * before calling this method or it could break
 * 
 * <p>
 * An example use is the following:
<pre>
//
// data class to use
public class SampleDataClass {
	// bean constructor
	public SampleDataClass() {
		
	}
	
	@FlightSimData(variable = "PLANE LATITUDE", units = "DEGREES")
	private double latitude;
	@FlightSimData(variable = "PLANE LONGITUDE", units = "DEGREES")
	private double longitude;
	@FlightSimData(variable = "AUTOPILOT MASTER")
	private boolean autopilot;
	
}
</pre> 
 * <br/> Later in the code, the annotation wrapper is instructed to manage the <code>SampleDataClass</code> with this:
<pre>
		//
		// create annotation wrapper for this simconnect session
		anWrapper = new AnnotationWrapper(sc);
		anWrapper.registerClass(SampleDataClass.class);
		anWrapper.requestSimObjectData(SampleDataClass.class, 
				SimConnectConstants.OBJECT_ID_USER,
				SimConnectPeriod.SECOND,
				false);

</pre>
 * 
 * @author lc0277
 *
 */
public class AnnotationWrapper {

	private Map<Class<?>, Integer> definitionIds = new HashMap<Class<?>, Integer>();
	private SimConnect simConnect;
	
	private int definitionID;
	private int requestID;
	
	/**
	 * Create a new annotation wrapper for a SimConnect session
	 * @param sc simconnect instance
	 * @throws NullPointerException if <code>sc</code> is null
	 */
	public AnnotationWrapper(SimConnect sc) {
		this(sc, 0, 0);
	}

	/**
	 * Create a new annotation wrapper for a SimConnect session. If {@link #requestSimObjectData(Class, int, SimConnectPeriod, boolean)}
	 * is called without <code>requestID</code> parameter, the wrapper will pick a new one starting
	 * at the given index
	 * @param sc simconnect instance
	 * @param startDefID starting index for data definition ids
	 * @param startReqID starting index for request ids
	 * @throws NullPointerException if <code>sc</code> is null
	 */
	public AnnotationWrapper(SimConnect sc, int startDefID, int startReqID) {
		if (sc == null)
			throw new NullPointerException("Simconnect cannot be null");
		this.simConnect = sc;
		setStartDefinitionID(startDefID);
		setStartRequestID(startReqID);
	}

	/**
	 * Sets the starting index for building data definition ID. use this method if you let
	 * this annotationwrapper choose identifiers for you. Default value is 0
	 * @param startID
	 */
	public void setStartDefinitionID(int startID) {
		this.definitionID = startID;
	}
	
	/**
	 * Sets the starting index for building data request ID. use this method if you let
	 * this annotationwrapper choose identifiers for you. Default value is 0
	 * @param startID
	 */
	public void setStartRequestID(int startID) {
		this.requestID = startID;
	}
	
	/**
	 * Register a class prior to use. This is mandatory before calling {@link #requestSimObjectData(Class, int, SimConnectPeriod, boolean)}
	 * or {@link #unwrap(RecvSimObjectData)} or {@link #setSimObjectData(Object, int)}
	 * @param c class to use
	 * @return data definition value (automatically choosed from starting index, see {@link #setStartDefinitionID(int)})
	 * @throws IllegalDataDefinition if the class <code>c</code> contains invalid or non-mappable fields
	 * @throws IOException SimConnect IO Errors
	 */
	public int registerClass(Class<?> c) throws IOException, IllegalDataDefinition {
		return registerClass(++definitionID, c);
	}
	
	/**
	 * Register a class prior to use. This is mandatory before calling {@link #requestSimObjectData(Class, int, SimConnectPeriod, boolean)}
	 * or {@link #unwrap(RecvSimObjectData)} or {@link #setSimObjectData(Object, int)}
	 * @param c class to use
	 * @param dataDefID force data definition ID
	 * @return data definition ID (here <code>dataDefID</code>)
	 * @throws IllegalDataDefinition if the class <code>c</code> contains invalid or non-mappable fields
	 * @throws IOException SimConnect IO Errors
	 * @since 0.7
	 */
	public int registerClass(Enum dataDefID, Class<?> c) throws IOException, IllegalDataDefinition {
		return registerClass(dataDefID.ordinal(), c);
	}
	
	
	/**
	 * Register a class prior to use. This is mandatory before calling {@link #requestSimObjectData(Class, int, SimConnectPeriod, boolean)}
	 * or {@link #unwrap(RecvSimObjectData)} or {@link #setSimObjectData(Object, int)}
	 * @param c class to use
	 * @param dataDefID force data definition ID
	 * @return data definition ID (here <code>dataDefID</code>)
	 * @throws IllegalDataDefinition if the class <code>c</code> contains invalid or non-mappable fields
	 * @throws IOException SimConnect IO Errors
	 */
	public int registerClass(int dataDefID, Class<?> c) throws IOException, IllegalDataDefinition {
		
		//
		// don't do it twice
		if (definitionIds.containsKey(c)) {
			return definitionIds.get(c).intValue();
		}
		
		int fieldAdded = 0;
		
		for (Field f : c.getDeclaredFields()) {
			if (f.isAnnotationPresent(FlightSimData.class)) {
				// register field
				FlightSimData fsd = f.getAnnotation(FlightSimData.class);
				
				String variable = fsd.variable();
				String units = fsd.units();
				
				// determinate field simconnect type from its class
				
				SimConnectDataType type = SimConnectDataType.INVALID;
				if (f.getType().equals(Float.class) ||
						f.getType().equals(float.class)) 
					type = SimConnectDataType.FLOAT32;
				else if (f.getType().equals(Double.class) ||
						f.getType().equals(double.class)) 
					type = SimConnectDataType.FLOAT64;
				else if (f.getType().equals(Integer.class) ||
						f.getType().equals(int.class)) 
					type = SimConnectDataType.INT32;
				else if (f.getType().equals(Long.class) ||
						f.getType().equals(long.class)) 
					type = SimConnectDataType.INT64;
				else if (f.getType().equals(Boolean.class) ||
						f.getType().equals(boolean.class)) 
					type = SimConnectDataType.INT32;
				else if (f.getType().equals(Short.class) ||
						f.getType().equals(short.class)) 
					type = SimConnectDataType.INT32;
				else if (f.getType().equals(LatLonAlt.class))
					type = SimConnectDataType.LATLONALT;
				else if (f.getType().equals(XYZ.class))
					type = SimConnectDataType.XYZ;
				else if (f.getType().equals(Waypoint.class))
					type = SimConnectDataType.WAYPOINT;
				else if (f.getType().equals(MarkerState.class))
					type = SimConnectDataType.MARKERSTATE;
				else if (f.getType().equals(String.class)) {
					int len = fsd.stringWidth();
					switch (len) {
					case 8:
						type = SimConnectDataType.STRING8;
						break;
					case 32:
						type = SimConnectDataType.STRING32;
						break;
					case 64:
						type = SimConnectDataType.STRING64;
						break;
					case 128:
						type = SimConnectDataType.STRING128;
						break;
					case 256:
						type = SimConnectDataType.STRING256;
						break;
					case 260:
						type = SimConnectDataType.STRING260;
						break;
					default:
						throw new IllegalDataDefinition("Invalid string length (" + len +")");
					}
				}
				
				if (type == SimConnectDataType.INVALID) {
					throw new IllegalDataDefinition("Invalid field type (" + 
							f.getType().getName() +")");
				}
				
				//
				// build simconnect data def
				simConnect.addToDataDefinition(dataDefID, variable, units, type);
				fieldAdded++;
			}
		}
		
		// register it if we had something to write
		if (fieldAdded > 0)
			definitionIds.put(c, dataDefID);
		
		return requestID;
	}
	
	/**
	 * Request SimObject data from a class. The class must be registered before using this method (see {@link #registerClass(Class)}).
	 * See {@link SimConnect#requestDataOnSimObject(Enum, Enum, int, SimConnectPeriod)} for
	 * more informations on this simconnect request.
	 * @param cl class to use. must be registered
	 * @param objectId object ID, or {@link SimConnectConstants#OBJECT_ID_USER} for user aircraft
	 * @param period period, see {@link SimConnectPeriod}
	 * @param onlyWhenChanged set to true if you want to receive data only if it was changed
	 * @return ID of request (picked up from starting index, see {@link #setStartRequestID(int)}
	 * @throws IOException simconnect error
	 * @throws IllegalDataDefinition if this class was not registered
	 */
	public int requestSimObjectData(Class<?> cl,
			int objectId,
			SimConnectPeriod period,
			boolean onlyWhenChanged) throws IOException, IllegalDataDefinition {
		return requestSimObjectData(cl, ++this.requestID, objectId, 
				period, onlyWhenChanged);
		
	}
	
	/**
	 * Request SimObject data from a class. The class must be registered before using this method (see {@link #registerClass(Class)}).
	 * See {@link SimConnect#requestDataOnSimObject(Enum, Enum, int, SimConnectPeriod)} for
	 * more informations on this simconnect request.
	 * @param cl class to use. must be registered
	 * @param objectId object ID, or {@link SimConnectConstants#OBJECT_ID_USER} for user aircraft
	 * @param requestId forced request id
	 * @param period period, see {@link SimConnectPeriod}
	 * @param onlyWhenChanged set to true if you want to receive data only if it was changed
	 * @return ID of request (here <code>requestId</code>)
	 * @throws IOException simconnect error
	 * @throws IllegalDataDefinition if this class was not registered
	 * @since 0.7
	 */
	public int requestSimObjectData(Class<?> cl,
			Enum requestId,
			int objectId,
			SimConnectPeriod period,
			boolean onlyWhenChanged) throws IOException, IllegalDataDefinition {
		return requestSimObjectData(cl, requestId.ordinal(), objectId, period, onlyWhenChanged);
	}

	
	/**
	 * Request SimObject data from a class. The class must be registered before using this method (see {@link #registerClass(Class)}).
	 * See {@link SimConnect#requestDataOnSimObject(Enum, Enum, int, SimConnectPeriod)} for
	 * more informations on this simconnect request.
	 * @param cl class to use. must be registered
	 * @param objectId object ID, or {@link SimConnectConstants#OBJECT_ID_USER} for user aircraft
	 * @param requestId forced request id
	 * @param period period, see {@link SimConnectPeriod}
	 * @param onlyWhenChanged set to true if you want to receive data only if it was changed
	 * @return ID of request (here <code>requestId</code>)
	 * @throws IOException simconnect error
	 * @throws IllegalDataDefinition if this class was not registered
	 */
	public int requestSimObjectData(Class<?> cl,
			int requestId,
			int objectId,
			SimConnectPeriod period,
			boolean onlyWhenChanged) throws IOException, IllegalDataDefinition {
		if (!definitionIds.containsKey(cl)) 
			throw new IllegalDataDefinition("Class not defined. call registerClass() first");
		
		int defId = ((Integer) definitionIds.get(cl)).intValue();
		int flags = onlyWhenChanged ? SimConnectConstants.DATA_REQUEST_FLAG_CHANGED :
			SimConnectConstants.DATA_REQUEST_FLAG_DEFAULT;
		simConnect.requestDataOnSimObject(requestId, defId, objectId, 
				period, flags, 0, 0, 0);
		return requestId;
	}
	
	
	/**
	 * Set SimObject data from a managed class
	 * @param data value
	 * @param objectId object id
	 * @throws IOException IO Error
	 * @throws IllegalDataDefinition if the class of <code>data</code> was not registered (see {@link #registerClass(Class)})
	 * @throws NullPointerException if data is null
	 */
	public <T> void setSimObjectData(T data,
			int objectId) throws IOException, IllegalDataDefinition {
		Class<?> cl = data.getClass();
		if (!definitionIds.containsKey(cl)) 
			throw new IllegalDataDefinition("Class not defined in this wrapper.");
		
		int dataLen = classDataSize(cl);
		DataWrapper dw = new DataWrapper(dataLen);
		wrap(data, dw);
		
		int defId = ((Integer) definitionIds.get(cl)).intValue();
		
		simConnect.setDataOnSimObject(defId, objectId, 
				false,
				1,
				dw);
	}
	
	/**
	 * Unwrap data, i.e. build an object instance from a received response packet.
	 * @param dataPacket data packet
	 * @return value of object, or null if it cannot be build for instance if the response packet does not corresponds to a managed request
	 */
	@SuppressWarnings("unchecked")
	public Object unwrap(RecvSimObjectData dataPacket) {
		int defId = dataPacket.getDefineID();
		for (Map.Entry<Class<?>, Integer> me : definitionIds.entrySet()) {
			if (me.getValue().intValue() == defId) {
				// 
				// build object
				try {
					return unwrap(me.getKey(), dataPacket);
				} catch (IllegalDataDefinition e) {
					return null;
				}
			}
		}
		return null;
	}
	
	/**
	 * Unwrap data, i.e. build an object instance from a received response packet. 
	 * @param data data packet
	 * @param cl class
	 * @return value of object, or null if it cannot be build for instance if the response packet does not corresponds to a managed request
	 * @throws IllegalDataDefinition if the given class is not managed by this wrapper
	 */
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cl, RecvSimObjectData data) throws IllegalDataDefinition {
		if (!definitionIds.containsKey(cl)) 
			throw new IllegalDataDefinition("Class not defined in this wrapper.");
		
		// try to build
		T o;
		try {
			o = cl.newInstance();
		} catch (InstantiationException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		}
		return (T) unwrapToObject(o, data);
	}
	
	/**
	 * Unwrap data, i.e. fill an object instance fields from a received response packet. 
	 * @param data data packet
	 * @param o fresh instance to fill
	 * @return value of object, or null if it cannot be build for instance if the response packet does not corresponds to a managed request
	 * @throws IllegalDataDefinition if the class of <code>o</code> is not managed by this wrapper
	 * @throws IllegalArgumentException from reflection API (bad field type?)
	 */
	public Object unwrapToObject(Object o,
			RecvSimObjectData data) throws IllegalDataDefinition {
		
		Class<?> cl = o.getClass();
		if (!definitionIds.containsKey(cl)) 
			throw new IllegalDataDefinition("Class not defined in this wrapper.");
		
		
		for (Field f : cl.getDeclaredFields()) {
			if (f.isAnnotationPresent(FlightSimData.class)) {
				try {
					unwrapField(o, f, data);
				} catch (IllegalAccessException e) {}
			}
		}
		return o;
	}
	
	/**
	 * Unwrap a field
	 * @param o destination object
	 * @param f field
	 * @param ro receive bufer
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private void unwrapField(Object o, Field f, RecvSimObjectData ro) throws IllegalArgumentException, IllegalAccessException {
		FlightSimData fsd = f.getAnnotation(FlightSimData.class);
		
		boolean accStatus =  f.isAccessible();
		f.setAccessible(true);
		
		// determinate field class
		if (f.getType().equals(Float.class) ||
				f.getType().equals(float.class)) {
			Float val = new Float(ro.getDataFloat32());
			f.set(o, val);
		}
		else if (f.getType().equals(Double.class) ||
				f.getType().equals(double.class)) {
			Double val = new Double(ro.getDataFloat64());
			f.set(o, val);
		}
		else if (f.getType().equals(Integer.class) ||
				f.getType().equals(int.class)) {
			Integer val = new Integer(ro.getDataInt32());
			f.set(o, val);
		}
		else if (f.getType().equals(Long.class) ||
				f.getType().equals(long.class))  {
			Long val = new Long(ro.getDataInt64());
			f.set(o, val);
		}
		else if (f.getType().equals(Boolean.class) ||
				f.getType().equals(boolean.class))  {
			Boolean val = new Boolean(ro.getDataInt32() != 0);
			f.set(o, val);
		}
		else if (f.getType().equals(Short.class) ||
				f.getType().equals(short.class))  {
			Short val = new Short((short)ro.getDataInt32());
			f.set(o, val);
		}
		else if (f.getType().equals(LatLonAlt.class)) {
			LatLonAlt val = ro.getLatLonAlt();
			f.set(o, val);
		}
		else if (f.getType().equals(XYZ.class)) {
			XYZ val = ro.getXYZ();
			f.set(o, val);
		}
		else if (f.getType().equals(Waypoint.class)) {
			Waypoint val = ro.getWaypoint();
			f.set(o, val);
		}
		else if (f.getType().equals(MarkerState.class)) {
			MarkerState val = ro.getMarkerState();
			f.set(o, val);
		}
		else if (f.getType().equals(String.class)) {
			int len = fsd.stringWidth();
			String val = null;
			switch (len) {
			case 8:
				val = ro.getDataString8();
				break;
			case 32:
				val = ro.getDataString32();
				break;
			case 64:
				val = ro.getDataString64();
				break;
			case 128:
				val = ro.getDataString128();
				break;
			case 256:
				val = ro.getDataString256();
				break;
			case 260:
				val = ro.getDataString260();
				break;
			}
			f.set(o, val);
		}
		
		f.setAccessible(accStatus);
	}
	
	private int classDataSize(Class<?> cl) {
		int len = 0;
		for (Field f : cl.getDeclaredFields()) {
			if (f.isAnnotationPresent(FlightSimData.class)) {
				len += fieldSize(f);
			}
		}
		return len;
	}
		
	
	private int fieldSize(Field f) {
		FlightSimData fsd = f.getAnnotation(FlightSimData.class);
		
		if (f.getType().equals(Float.class) ||
				f.getType().equals(float.class) ||
				f.getType().equals(Integer.class) ||
				f.getType().equals(int.class) ||
				f.getType().equals(Boolean.class) ||
				f.getType().equals(boolean.class) ||
				f.getType().equals(Short.class) ||
				f.getType().equals(short.class))
			return 4;

		if (f.getType().equals(Double.class) ||
				f.getType().equals(double.class) ||
				f.getType().equals(Long.class) ||
				f.getType().equals(long.class))
			return 8;


		if (f.getType().equals(XYZ.class))
			return DataType.XYZ.size();
		if (f.getType().equals(MarkerState.class))
			return DataType.MARKERSTATE.size();
		if (f.getType().equals(Waypoint.class))
			return DataType.WAYPOINT.size();
		if (f.getType().equals(LatLonAlt.class))
			return DataType.LATLONALT.size();

		else if (f.getType().equals(String.class)) {
			int len = fsd.stringWidth();
			return len;
		}
		
		return 0;	// fallback
	}

	private void wrap(Object o,
			DataWrapper dw) throws IllegalDataDefinition {
		
		Class<?> cl = o.getClass();
		if (!definitionIds.containsKey(cl)) 
			throw new IllegalDataDefinition("Class not defined in this wrapper.");
		
		
		for (Field f : cl.getDeclaredFields()) {
			if (f.isAnnotationPresent(FlightSimData.class)) {
				try {
					wrapField(o, f, dw);
				} catch (IllegalAccessException e) {}
			}
		}
	}
	
	private void wrapField(Object o, Field f, DataWrapper dw) throws IllegalDataDefinition, IllegalAccessException {
		FlightSimData fsd = f.getAnnotation(FlightSimData.class);
		
		boolean accStatus =  f.isAccessible();
		f.setAccessible(true);
		
		// determinate field class
		if (f.getType().equals(Float.class) ||
				f.getType().equals(float.class)) {
			Float val = (Float) f.get(o);
			dw.putFloat32(val.floatValue());
		}
		else if (f.getType().equals(Double.class) ||
				f.getType().equals(double.class)) {
			Double val = (Double) f.get(o);
			dw.putFloat64(val.doubleValue());
		}
		else if (f.getType().equals(Integer.class) ||
				f.getType().equals(int.class)) {
			Integer val = (Integer) f.get(o);
			dw.putInt32(val.intValue());
		}
		else if (f.getType().equals(Long.class) ||
				f.getType().equals(long.class))  {
			Long val = (Long) f.get(o);
			dw.putInt64(val.longValue());
		}
		else if (f.getType().equals(Boolean.class) ||
				f.getType().equals(boolean.class))  {
			Boolean val = (Boolean) f.get(o);
			dw.putInt32(val ? 1 : 0);
		}
		else if (f.getType().equals(Short.class) ||
				f.getType().equals(short.class))  {
			Short val = (Short) f.get(o);
			dw.putInt32(val.intValue());
		}
		else if (f.getType().equals(LatLonAlt.class)) {
			LatLonAlt val = (LatLonAlt) f.get(o);
			dw.putData(val);
		}
		else if (f.getType().equals(XYZ.class)) {
			XYZ val = (XYZ) f.get(o);
			dw.putData(val);
		}
		else if (f.getType().equals(Waypoint.class)) {
			Waypoint val = (Waypoint) f.get(o);
			dw.putData(val);
		}
		else if (f.getType().equals(MarkerState.class)) {
			MarkerState val = (MarkerState) f.get(o);
			dw.putData(val);
		}
		else if (f.getType().equals(String.class)) {
			String s = (String) f.get(o);
			if (s == null) s = "";
			int len = fsd.stringWidth();
			dw.putString(s, len);
		}
		
		f.setAccessible(accStatus);
	}

	
}
