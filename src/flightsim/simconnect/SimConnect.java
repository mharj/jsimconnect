package flightsim.simconnect;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;

import flightsim.simconnect.config.Configuration;
import flightsim.simconnect.config.ConfigurationManager;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.data.*;
import flightsim.simconnect.recv.*;
import flightsim.simconnect.wrappers.DataWrapper;

/**
 * SimConnect main communication class.
 * <p>
 * 
 * <h3>A note on protocol version </h3>
 * <u>NB: does not deal with IP (4 or 6) protocol ! </u>. <br/>
 * 
 * When using a simconnect instance, some constructors accepts a protocol version number. These version indicates
 * the simconnect protocol to emulate when talking to the remote FSX. The different FSX updates included new features
 * to the simconnect protocol & client library, but also broke backward compatibility when the simconnect client
 * is newer than the remote Flight Simulator instance.<br/>
 * JSimconnect uses the version given to the constructor to <i>emulate</i> an older simconnect protocol client,
 * thus allowing to connect to older FSX versions. Note that by default, the protocol number used is the most up
 * to date, and consequently a VERSION_MISMATCH exception will be received upon connecting to older FSX version.
 * As of jsimconnect 0.7, this protocol version is <b>0x4</b>, corresponding to Pre-SP2/Acceleration XPack.
 * <br/>
 * As of jsimconnect 0.7 the supported protocol numbers are :
 * <ul>
 * <li> 0x2 (RTM version). Note that 0x1 (possibly from fsx beta) is not supported </li>
 * <li> 0x3 (SP1). </li>
 * <li> 0x4 (SP2/Acceleration) </li>
 * </ul>
 * When specifying a protocol version to emulate, some functions will be inhibited as they may not be available
 * in the current version. For instance modeless UI, see {@link #text(int, float, int, String)} are not available
 * before SP1. Constructing a simconnect instance emulating protocol 0x2 will systematically throw a {@link UnsupportedOperationException}
 * exception when calling this methods.
 * 
 * 
 * <h3>License</h3>
 * This software is published under the terms of the <a href="http://www.gnu.org/licenses/lgpl.html">GNU Lesser General Public License</a>.
 * 
 * <br>
 * @author lc0277
 *
 */
public class SimConnect implements SimConnectConstants {
	private SocketChannel sc;
	private ByteBuffer readBuffer;
	private ByteBuffer writeBuffer;
	
	// configuration
	private final String appName;
	@SuppressWarnings("unused") //$NON-NLS-1$
	private final int bufferSize;
	private final InetSocketAddress remoteAddress;
	private final int ourProtocol;
	
	// statistics
	private int currentIndex = 1;
	private int packetsReceived = 0;
	private int packetsSent = 0;
	private int bytesReceived = 0;
	private int bytesSent = 0;
	
	
	/**
	 * Build number of simconnect.
	 * 61355 = FSX SP1+SP1A; 
	 * 60905 = FSX RTM
	 * @since 0.5
	 * 
	 */
	private static final int SIMCONNECT_BUILD_SP0		= 60905;
	private static final int SIMCONNECT_BUILD_SP1		= 61355;
	private static final int SIMCONNECT_BUILD_SP2_XPACK	= 61259;
	
	
	/* ***************************************************************
	 * 
	 * Construction & configuration 
	 * 
	 * ***************************************************************
	 */
	
	/**
	 * Open a simconnect connection to the local machine, guessing the right connection parameters.
	 * Port number, protocol and host are automatically guessed.
	 * <br/>
	 * The most recent simconnect protocol version is used. Take care if you want backward compatiblity.
	 * 
	 * @since 0.6
	 * @param appName application name
	 * @see Configuration#findSimConnectPort()
	 */
	public SimConnect(String appName) throws IOException {
		this(appName, makeAutoConfiguration());
	}
	
	private static Configuration makeAutoConfiguration() {
		Configuration cfg = null;
		
		try {
			cfg = ConfigurationManager.getConfiguration(0); 
		} catch (ConfigurationNotFoundException cnfe) {
			cfg = new Configuration();
		}
		
		// fix port number (with automatic settings)
		int port = cfg.getInt(Configuration.PORT, -1);
		if (port == -1) {
			port = Configuration.findSimConnectPortIPv4();
			if (port <= 0) {
				port = Configuration.findSimConnectPortIPv6();
				cfg.setProtocol(6);
			} else {
				cfg.setProtocol(4);
			}
			cfg.setPort(port);
		}
		
		// fix host
		String host = cfg.get(Configuration.ADDRESS, null);
		if (host == null) {
			if (cfg.getInt(Configuration.PROTOCOL, 4) == 6)
				cfg.setAddress("::1");
			else
				cfg.setAddress("localhost");
		}
		
		return cfg;
	}
	
	
	/**
	 * Open a simconnect connection using the specified configuration
	 * object
	 * 
	 * @param appName application name
	 * @param config configuration parameters
	 * @param simConnectProtocol SimConnect protocol version (0x2 for FSX RTM original; 0x3 for SP1; 0x4 for SP2)
	 * @throws IOException if connection failed
	 * @throws IllegalArgumentException if protocol version is not supported
	 * @since 0.5
	 */
	public SimConnect(String appName, Configuration config, int simConnectProtocol) throws IOException {
		this.appName = appName;
		
		// read configuration
		InetAddress inAddr = null;
		
		// resolve all ip adresses associated with this host
		// and keep only them of right protocol.
		// If multiple addresses are found, the last valid one is used
		String host = config.get(Configuration.ADDRESS, "localhost"); //$NON-NLS-1$ 
		InetAddress[] addrs = InetAddress.getAllByName(host);
		
		String proto = config.get(Configuration.PROTOCOL, ""); //$NON-NLS-1$ 
		for (InetAddress in : addrs) {
			if ("IPv6".equalsIgnoreCase(proto) && (in instanceof Inet6Address)) inAddr = in; //$NON-NLS-1$
			if (!"IPv6".equalsIgnoreCase(proto) && (in instanceof Inet4Address)) inAddr = in; //$NON-NLS-1$
		}
		
		if (inAddr == null && addrs.length > 0) 
			inAddr = addrs[0];
		
		if (inAddr == null)
			throw new IOException(Messages.get("SimConnect.Unknown_host")); //$NON-NLS-1$
		
		// default SimConnect port is unknown ,just take a dumb one
		int port = config.getInt(Configuration.PORT, 8002); //$NON-NLS-1$
		remoteAddress = new InetSocketAddress(host, port);
		
		// buffer size
		bufferSize = config.getInt(Configuration.MAX_RECEIVE_SIZE, RECEIVE_SIZE); //$NON-NLS-1$
		
		// protocol
		this.ourProtocol = simConnectProtocol;
		if (ourProtocol != 0x2 && ourProtocol != 0x3 && ourProtocol != 0x4) {
			throw new IllegalArgumentException(Messages.getString("SimConnect.VersionMismatch")); //$NON-NLS-1$
		}
		
		// open connection
		openNetworkConnection();

		// disable Nagle before sending the first hello packet
		if (config.getBoolean(Configuration.DISABLE_NAGLE, false))  //$NON-NLS-1$
			sc.socket().setTcpNoDelay(false);
		
		// send hello packet
		open();
	}

	/**
	 * Open a simconnect connection using the specified configuration
	 * object. <br/>
	 * The most recent simconnect protocol version is used. Take care if you want backward compatiblity.
	 * 
	 * @param appName application name
	 * @param config configuration parameters
	 * @throws IOException if connection failed
	 * @since 0.2
	 */
	public SimConnect(String appName, Configuration config) throws IOException {
		this(appName, config, config.getInt("SimConnect", SimConnectConstants.PROTO_VERSION)); //$NON-NLS-1$
	}

	/**
	 * Open a simconnect connection specifying the remote host
	 * @param appName	application name
	 * @param host remote host name
	 * @param port remote port
	 * @param simConnectProtocol SimConnect protocol version (0x2 for FSX RTM original; 0x3 for SP1; 0x4 for SP2)
	 * @since 0.5
	 * @throws IOException if connection failed
	 */
	public SimConnect(String appName, String host, int port, int simConnectProtocol) throws IOException {
		this(appName, buildConfiguration(host, port), simConnectProtocol);
	}

	/**
	 * Open a simconnect connection specifying the remote host.
	 * The most recent simconnect protocol version is used. Take care if you want backward compatiblity.
	 * @param appName	application name
	 * @param host remote host name
	 * @param port remote port
	 * @throws IOException if connection failed
	 */
	public SimConnect(String appName, String host, int port) throws IOException {
		this(appName, buildConfiguration(host, port), SimConnectConstants.PROTO_VERSION);
	}
	
	/**
	 * Open a simconnect connection using a configuration block
	 * from the <code>SimConnect.cfg</code> file given by its number
	 * @param appName application name
	 * @param configNumber configuration block number
	 * @param simConnectProtocol SimConnect protocol version (0x2 for FSX RTM original; 0x3 for SP1; 0x4 for SP2)
	 * @throws IOException if connection failed
	 * @throws ConfigurationNotFoundException
	 * @since 0.5
	 */
	public SimConnect(String appName, int configNumber, int simConnectProtocol) throws IOException, ConfigurationNotFoundException {
		this(appName, ConfigurationManager.getConfiguration(configNumber), simConnectProtocol);
	}

	/**
	 * Open a simconnect connection using a configuration block
	 * from the <code>SimConnect.cfg</code> file given by its number. <br/>
	 * The most recent simconnect protocol version is used. Take care if you want backward compatiblity.
	 * @param appName application name
	 * @param configNumber configuration block number
	 * @throws IOException if connection failed
	 * @throws ConfigurationNotFoundException
	 * @since 0.2
	 */
	public SimConnect(String appName, int configNumber) throws IOException, ConfigurationNotFoundException {
		this(appName, ConfigurationManager.getConfiguration(configNumber), SimConnectConstants.PROTO_VERSION);
	}
	
	private static Configuration buildConfiguration(String host, int port) {
		Configuration c = new Configuration();
		c.put("Address", host); //$NON-NLS-1$
		c.put("Port", Integer.toString(port)); //$NON-NLS-1$
		return c;
	}
	
	
	/**
	 * Open network connection and set up buffers
	 * @param isa
	 * @param bufferSize
	 * @throws IOException
	 */
	private void openNetworkConnection() throws IOException {
		// Socket channels are safe for use by multiple concurrent threads.
		
		// this methods is bugged with IPv6 JDK 1.5 _01 and win32
		sc = SocketChannel.open(remoteAddress);
		
		readBuffer = ByteBuffer.allocateDirect(RECEIVE_SIZE);
		readBuffer.order(ByteOrder.LITTLE_ENDIAN);
		writeBuffer = ByteBuffer.allocateDirect(RECEIVE_SIZE);
		writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
	}
	
	/* ***************************************************************
	 * 
	 * Connection control
	 * 
	 * ***************************************************************
	 */
	
	
	/**
	 * Close the connection. Do not attempt anything after that.
	 * @throws IOException socket errors
	 *
	 */
	public void close() throws IOException {
		sc.close();
	}
	
	/**
	 * Returns true if the connection was closed
	 * @return ture if connection is closed
	 */
	public boolean isClosed() {
		return !sc.isConnected();
	}
	
	/**
	 * Returns the socket address (ip v4 or v6 + port number) of the remote
	 * server
	 * @return remote socket address
	 * @since 0.2
	 */
	public SocketAddress remoteAddress() {
		return sc.socket().getRemoteSocketAddress();
	}
	
	/**
	 * Returns the socket address (ip v4 or v6 + port number) of the
	 * local simconnect client
	 * @return local socket address
	 * @since 0.2
	 */
	public SocketAddress localAddress() {
		return sc.socket().getLocalSocketAddress();
	}
	
	/**
	 * Finalize packet to be send
	 * @param type packet type
	 * @throws IOException
	 */
	private synchronized void sendPacket(int type) throws IOException {
		// finalize packet
		int packetSize = writeBuffer.position();
		writeBuffer.putInt(0, packetSize);	// size
		writeBuffer.putInt(4, ourProtocol);
		writeBuffer.putInt(8, 0xF0000000 | type);
		writeBuffer.putInt(12, currentIndex++);
		writeBuffer.flip();
		sc.write(writeBuffer);
		
		packetsSent++;
		bytesSent += packetSize;
	}
	
	/**
	 * Prepare a buffer for writing. Clean it, set to zero and position to 16
	 * @param bf
	 */
	private void clean(ByteBuffer bf) {
		bf.clear();
		writeBuffer.position(16);
	}
	
	private void putString(ByteBuffer bf, String s, int fixed) {
		if (s == null) s = ""; //$NON-NLS-1$
		byte[] b = s.getBytes();
		bf.put(b, 0, Math.min(b.length, fixed));
		if (b.length < fixed) {
			for (int i = 0; i < (fixed - b.length); i++) {
				bf.put((byte) 0);
			}
		}
	}
	
	private synchronized void open() throws IOException {
		clean(writeBuffer);
		putString(writeBuffer, appName, 256);
		writeBuffer.putInt(0);
		writeBuffer.put((byte) 0);
		writeBuffer.put((byte) 'X');
		writeBuffer.put((byte) 'S');
		writeBuffer.put((byte) 'F');
		if (ourProtocol == 2) {
			writeBuffer.putInt(0);		// major version
			writeBuffer.putInt(0);		// minor version
			writeBuffer.putInt(SIMCONNECT_BUILD_SP0);	// major build
			writeBuffer.putInt(0);		// minor build
		} else if (ourProtocol == 3) {
			writeBuffer.putInt(10);		// major version
			writeBuffer.putInt(0);		// minor version
			writeBuffer.putInt(SIMCONNECT_BUILD_SP1);	// major build
			writeBuffer.putInt(0);		// minor build
		} else if (ourProtocol == 4) {
			writeBuffer.putInt(10);		// major version
			writeBuffer.putInt(0);		// minor version
			writeBuffer.putInt(SIMCONNECT_BUILD_SP2_XPACK);	// major build
			writeBuffer.putInt(0);		// minor build
		} else {
			throw new IllegalArgumentException(Messages.getString("SimConnect.InvalidProtocol")); //$NON-NLS-1$
		}
		sendPacket(0x01);	
	}
	
	/**
	 * @param dataDefinitionId see {@link #addToClientDataDefinition(int, int, int, int)}
	 * @param datumName see {@link #addToClientDataDefinition(int, int, int, int)}
	 * @param unitsName see {@link #addToClientDataDefinition(int, int, int, int)}
	 * @param dataType see {@link #addToClientDataDefinition(int, int, int, int)}
	 * @throws IOException
	 */
	public synchronized void addToDataDefinition(int dataDefinitionId, 
			String datumName, String unitsName, 
			SimConnectDataType dataType) throws IOException {
		addToDataDefinition(dataDefinitionId, datumName, unitsName, dataType, 0.0f, UNUSED);
	}

	/**
	 * @param dataDefinitionId see {@link #addToClientDataDefinition(int, int, int, int)}
	 * @param datumName see {@link #addToClientDataDefinition(int, int, int, int)}
	 * @param unitsName see {@link #addToClientDataDefinition(int, int, int, int)}
	 * @param dataType see {@link #addToClientDataDefinition(int, int, int, int)}
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void addToDataDefinition(Enum dataDefinitionId, 
			String datumName, String unitsName, 
			SimConnectDataType dataType) throws IOException {
		addToDataDefinition(dataDefinitionId.ordinal(), datumName, unitsName, dataType);
	}

	/**
	 * 
	 * The <code>addToDataDefinition</code> method is used to add a Flight Simulator simulation 
	 * variable name to a client defined object definition.
	 * 
	 * <p> A data definition must be specified, using this function, before this function can be 
	 * called If the data definition exceeds the size of the client data area on the server, then the 
	 * extra bytes will be filled with zeros, an error will not be returned. </p>
	 * 
	 * <p> The data will be returned in a {@link RecvSimObjectData} structure. </p>
	 * 
	 * @param dataDefinitionId Specifies the ID of the client defined data definition. 
	 * @param datumName Specifies the name of the Flight Simulator simulation variable. 
	 * 			See the Simulation Variables document for a table of variable names. 
	 * 			If an index is required then it should be appended to the variable name following a 
	 * 			colon. Indexes are numbered from 1 (not zero). Simulation variable names are not 
	 * 			case-sensitive (so can be entered in upper or lower case).
	 * @param unitsName Specifies the units of the variable. See the Simulation Variables document for 
	 * 			a table of acceptable unit names. It is possible to specify different units to receive 
	 * 			the data in, from those specified in the Simulation Variables document. 
	 * 			The alternative units must come under the same heading (such as Angular Velocity, or 
	 * 			Volume, as specified in the Units of Measurement section of the Simulation Variables 
	 * 			document). For strings and structures enter <code>null</code>" for this parameter. 
	 * @param dataType One member of the {@link SimConnectDataType} enumeration type. This parameter 
	 * 			is used to determine what datatype should be used to return the data. The default 
	 * 			is {@link SimConnectDataType#FLOAT64}. Note that the structure data types are 
	 * 			legitimate parameters here.
	 * @param epsilon If data is requested only when it changes (see the flags parameter of 
	 * 			{@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)},
	 * 			a change will only be reported if it is greater than the value of this parameter. 
	 * 			The default is zero, so even the tiniest change will initiate the transmission of data. 
	 * 			Set this value appropriately so insignificant changes are not transmitted. This can be 
	 * 			used with integer data.
	 * @param datumId Specifies a client defined datum ID. The default is zero. Use this to identify 
	 * 			the data received if the data is being returned in tagged format (see the flags 
	 * 			parameter of {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}). 
	 * 			There is no need to specify datum IDs if the data is not being returned in tagged format. 
	 * @see #clearDataDefinition(int)
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)
	 * @see #requestDataOnSimObjectType(int, int, int, SimObjectType)
	 * @throws IOException
	 */
	public synchronized void addToDataDefinition(int dataDefinitionId, 
			String datumName, String unitsName, 
			SimConnectDataType dataType, float epsilon, int datumId) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(dataDefinitionId);
		putString(writeBuffer, datumName, 256);
		putString(writeBuffer, unitsName, 256);
		writeBuffer.putInt(dataType.ordinal());
		writeBuffer.putFloat(epsilon);
		writeBuffer.putInt(datumId);
		sendPacket(0x0C);
	}

	/**
	 * 
	 * The <code>addToDataDefinition</code> method is used to add a Flight Simulator simulation 
	 * variable name to a client defined object definition.
	 * 
	 * <p> A data definition must be specified, using this function, before this function can be 
	 * called If the data definition exceeds the size of the client data area on the server, then the 
	 * extra bytes will be filled with zeros, an error will not be returned. </p>
	 * 
	 * <p> The data will be returned in a {@link RecvSimObjectData} structure. </p>
	 * 
	 * @param dataDefinitionId Specifies the ID of the client defined data definition. 
	 * @param datumName Specifies the name of the Flight Simulator simulation variable. 
	 * 			See the Simulation Variables document for a table of variable names. 
	 * 			If an index is required then it should be appended to the variable name following a 
	 * 			colon. Indexes are numbered from 1 (not zero). Simulation variable names are not 
	 * 			case-sensitive (so can be entered in upper or lower case).
	 * @param unitsName Specifies the units of the variable. See the Simulation Variables document for 
	 * 			a table of acceptable unit names. It is possible to specify different units to receive 
	 * 			the data in, from those specified in the Simulation Variables document. 
	 * 			The alternative units must come under the same heading (such as Angular Velocity, or 
	 * 			Volume, as specified in the Units of Measurement section of the Simulation Variables 
	 * 			document). For strings and structures enter <code>null</code>" for this parameter. 
	 * @param dataType One member of the {@link SimConnectDataType} enumeration type. This parameter 
	 * 			is used to determine what datatype should be used to return the data. The default 
	 * 			is {@link SimConnectDataType#FLOAT64}. Note that the structure data types are 
	 * 			legitimate parameters here.
	 * @param epsilon If data is requested only when it changes (see the flags parameter of 
	 * 			{@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)},
	 * 			a change will only be reported if it is greater than the value of this parameter. 
	 * 			The default is zero, so even the tiniest change will initiate the transmission of data. 
	 * 			Set this value appropriately so insignificant changes are not transmitted. This can be 
	 * 			used with integer data.
	 * @param datumId Specifies a client defined datum ID. The default is zero. Use this to identify 
	 * 			the data received if the data is being returned in tagged format (see the flags 
	 * 			parameter of {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}). 
	 * 			There is no need to specify datum IDs if the data is not being returned in tagged format. 
	 * @see #clearDataDefinition(int)
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)
	 * @see #requestDataOnSimObjectType(int, int, int, SimObjectType)
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void addToDataDefinition(Enum dataDefinitionId, 
			String datumName, String unitsName, 
			SimConnectDataType dataType, float epsilon, int datumId) throws IOException {
		addToDataDefinition(dataDefinitionId.ordinal(), datumName, unitsName, dataType, epsilon, datumId);
	}


	/**
	 * See {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}
	 * 
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)
	 * @param dataRequestId see {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}
	 * @param dataDefinitionId see {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}
	 * @param objectId see {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}
	 * @param period see {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}
	 * @throws IOException
	 */
	public synchronized void requestDataOnSimObject(int dataRequestId, int dataDefinitionId, 
			int objectId, SimConnectPeriod period) throws IOException {
		requestDataOnSimObject(dataRequestId, dataDefinitionId, objectId, period, 0, 0, 0, 0);
	}

	/**
	 * See {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}
	 * 
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)
	 * @param dataRequestId see {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}
	 * @param dataDefinitionId see {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}
	 * @param objectId see {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}
	 * @param period see {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)}
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void requestDataOnSimObject(Enum dataRequestId, 
			Enum dataDefinitionId, 
			int objectId, SimConnectPeriod period) throws IOException {
		requestDataOnSimObject(dataRequestId.ordinal(), dataDefinitionId.ordinal(), objectId, period);
	}

	/**
	 * The <code>requestDataOnSimObject</code> method is used to request when the SimConnect 
	 * client is to receive data values for a specific object.
	 * 
	 * <p> Changing the Period parameter or changing the content of a data definition has a higher 
	 * performance cost than changing the origin, interval or limit parameters. So to termporarily 
	 * turn off data requests, especially for short periods of time, consider setting the interval 
	 * parameter to a very high value (such as DWORD _MAX). If changes are required to a data definition, 
	 * consider setting the Period parameter to {@link SimConnectPeriod#NEVER} before making the changes, 
	 * and then turning on the appropriate period after the changes have been made. </p>
	 * 
	 * <p> Data is always transmitted with the <code>requestDataOnSimObject</code> function, so if 
	 * timing only notifications are required, use the {@link #subscribeToSystemEvent(int, String)} function. </p>
	 * 
	 * <p> Note that variable length strings should not be used in data defintions, except where 
	 * the Period parameter has been set to {@link SimConnectPeriod#ONCE}. </p>
	 * 
	 * <p> One method of testing whether the user has changed aircraft type is to use this function
	 * to return the title of the user aircraft, and note that if it changes, the user has changed 
	 * the type of aircraft (all aircraft types have uniques title strings, including those simply 
	 * with different color schemes). </p>
	 * 
	 * @param dataRequestId Specifies the ID of the client defined request. This is used later by 
	 * 		the client to identify which data has been received. This value should be unique for each 
	 * 		request, re-using a RequestID will overwrite any previous request using the same ID. 
	 * @param dataDefinitionId Specifies the ID of the client defined data definition. 
	 * @param objectId  Specifies the ID of the Flight Simulator object that the data should be about. 
	 * 		This ID can be {@link SimConnectConstants#OBJECT_ID_USER} (to specify the user's aircraft) or 
	 * 		obtained from a {@link RecvSimObjectDataByType} structure after a call to 
	 * 		{@link #requestDataOnSimObjectType(int, int, int, SimObjectType)}
	 * @param period One member of the {@link SimConnectPeriod} enumeration type, specifying how often 
	 * 		the data is to be sent by the server and recieved by the client.
	 * @param flags If 0, The default, data will be sent strictly according to the defined period.
	 * 		otherwise, see {@link SimConnectConstants#DATA_REQUEST_FLAG_CHANGED}  or
	 * 		{@link SimConnectConstants#DATA_REQUEST_FLAG_TAGGED} 
	 * @param origin The number of Period events that should elapse before transmission of the data begins. 
	 * 		The default is zero, which means transmissions will start immediately.
	 * @param interval The number of Period events that should elapse between transmissions of the data. 
	 * 		The default is zero, which means the data is transmitted every Period
	 * @param limit The number of times the data should be transmitted before this communication is ended. 
	 * 		The default is zero, which means the data should be transmitted endlessly.
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType, float, int)
	 * @see #clearDataDefinition(int)
	 * @throws IOException
	 */
	public synchronized void requestDataOnSimObject(int dataRequestId, int dataDefinitionId, 
			int objectId, SimConnectPeriod period, int flags, int origin, 
			int interval, int limit) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(dataRequestId);
		writeBuffer.putInt(dataDefinitionId);
		writeBuffer.putInt(objectId);
		writeBuffer.putInt(period.ordinal());
		writeBuffer.putInt(flags);
		writeBuffer.putInt(origin);
		writeBuffer.putInt(interval);
		writeBuffer.putInt(limit);
		sendPacket(0x0E);
	}

	/**
	 * The <code>requestDataOnSimObject</code> method is used to request when the SimConnect 
	 * client is to receive data values for a specific object.
	 * 
	 * <p> Changing the Period parameter or changing the content of a data definition has a higher 
	 * performance cost than changing the origin, interval or limit parameters. So to termporarily 
	 * turn off data requests, especially for short periods of time, consider setting the interval 
	 * parameter to a very high value (such as DWORD _MAX). If changes are required to a data definition, 
	 * consider setting the Period parameter to {@link SimConnectPeriod#NEVER} before making the changes, 
	 * and then turning on the appropriate period after the changes have been made. </p>
	 * 
	 * <p> Data is always transmitted with the <code>requestDataOnSimObject</code> function, so if 
	 * timing only notifications are required, use the {@link #subscribeToSystemEvent(int, String)} function. </p>
	 * 
	 * <p> Note that variable length strings should not be used in data defintions, except where 
	 * the Period parameter has been set to {@link SimConnectPeriod#ONCE}. </p>
	 * 
	 * <p> One method of testing whether the user has changed aircraft type is to use this function
	 * to return the title of the user aircraft, and note that if it changes, the user has changed 
	 * the type of aircraft (all aircraft types have uniques title strings, including those simply 
	 * with different color schemes). </p>
	 * 
	 * @param dataRequestId Specifies the ID of the client defined request. This is used later by 
	 * 		the client to identify which data has been received. This value should be unique for each 
	 * 		request, re-using a RequestID will overwrite any previous request using the same ID. 
	 * @param dataDefinitionId Specifies the ID of the client defined data definition. 
	 * @param objectId  Specifies the ID of the Flight Simulator object that the data should be about. 
	 * 		This ID can be {@link SimConnectConstants#OBJECT_ID_USER} (to specify the user's aircraft) or 
	 * 		obtained from a {@link RecvSimObjectDataByType} structure after a call to 
	 * 		{@link #requestDataOnSimObjectType(int, int, int, SimObjectType)}
	 * @param period One member of the {@link SimConnectPeriod} enumeration type, specifying how often 
	 * 		the data is to be sent by the server and recieved by the client.
	 * @param flags If 0, The default, data will be sent strictly according to the defined period.
	 * 		otherwise, see {@link SimConnectConstants#DATA_REQUEST_FLAG_CHANGED}  or
	 * 		{@link SimConnectConstants#DATA_REQUEST_FLAG_TAGGED} 
	 * @param origin The number of Period events that should elapse before transmission of the data begins. 
	 * 		The default is zero, which means transmissions will start immediately.
	 * @param interval The number of Period events that should elapse between transmissions of the data. 
	 * 		The default is zero, which means the data is transmitted every Period
	 * @param limit The number of times the data should be transmitted before this communication is ended. 
	 * 		The default is zero, which means the data should be transmitted endlessly.
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType, float, int)
	 * @see #clearDataDefinition(int)
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void requestDataOnSimObject(Enum dataRequestId, 
			Enum dataDefinitionId, 
			int objectId, SimConnectPeriod period, int flags, int origin, 
			int interval, int limit) throws IOException {
		requestDataOnSimObject(dataRequestId.ordinal(), dataDefinitionId.ordinal(), objectId, period, 
				flags, origin, interval, limit);
	}

	
	/**
	 * The <code>clearDataDefinition</code> method is used to remove all simulation variables 
	 * from a client defined data definition.
	 * 
	 * <p> Use this funtion to permanently delete a data definition. To temporarily suspend 
	 * data requests see the remarks for the 
	 * {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)} function .
	 * </p>
	 * 
	 * @param dataDefinitionId Specifies the ID of the client defined data definition.
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType, float, int)
	 * @throws IOException
	 */
	public synchronized void clearDataDefinition(int dataDefinitionId) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(dataDefinitionId);
		sendPacket(0x0d);
	}

	/**
	 * The <code>clearDataDefinition</code> method is used to remove all simulation variables 
	 * from a client defined data definition.
	 * 
	 * <p> Use this funtion to permanently delete a data definition. To temporarily suspend 
	 * data requests see the remarks for the 
	 * {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)} function .
	 * </p>
	 * 
	 * @param dataDefinitionId Specifies the ID of the client defined data definition.
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType, float, int)
	 * @throws IOException
	 * @since 0.4
	 */
	public synchronized void clearDataDefinition(Enum dataDefinitionId) throws IOException {
		clearDataDefinition(dataDefinitionId.ordinal());
	}

	/**
	 * The <code>requestDataOnSimObjectType</code> function is used to retrieve informtion about 
	 * simulation objects of a given type that are within a specifed radius of the user's aircraft.
	 * 
	 * <p> 
	 * The data will be returned on all the relevant objects within the specified radius, but they 
	 * will not be returned in any specific order. It is the responsibility of the client program 
	 * to sort the returned data into order, if that is required. Information is returned in 
	 * a {@link RecvSimObjectDataByType} structure, one structure per object.</p>
	 * 
	 * @param dataRequestId Specifies the ID of the client defined request. This is used later by the 
	 * 		client to identify which data has been received. This value should be unique for each request, 
	 * 		re-using a RequestID will overwrite any previous request using the same ID. 
	 * @param dataDefinitionId Specifies the ID of the client defined data definition. 
	 * @param radiusMeters Double word containing the radius in meters. If this is set to zero then 
	 * 		information on all relevant objects will be returned. This value is ignored if type is 
	 * 		set to {@link SimObjectType#USER}. The error {@link SimConnectException#OUT_OF_BOUNDS} 
	 * 		will be returned if a radius is given and it exceeds the maximum allowed (200 Km).
	 * @param type Specifies the type of object to receive information on. One member of the 
	 * 		{@link SimObjectType#USER} enumeration type.
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType, float, int)
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)
	 * @throws IOException
	 */
	public synchronized void requestDataOnSimObjectType(int dataRequestId,
			int dataDefinitionId, int radiusMeters, SimObjectType type) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(dataRequestId);
		writeBuffer.putInt(dataDefinitionId);
		writeBuffer.putInt(radiusMeters);
		writeBuffer.putInt(type.ordinal());
		sendPacket(0x0f);
	}

	/**
	 * The <code>requestDataOnSimObjectType</code> function is used to retrieve informtion about 
	 * simulation objects of a given type that are within a specifed radius of the user's aircraft.
	 * 
	 * <p> 
	 * The data will be returned on all the relevant objects within the specified radius, but they 
	 * will not be returned in any specific order. It is the responsibility of the client program 
	 * to sort the returned data into order, if that is required. Information is returned in 
	 * a {@link RecvSimObjectDataByType} structure, one structure per object.</p>
	 * 
	 * @param dataRequestId Specifies the ID of the client defined request. This is used later by the 
	 * 		client to identify which data has been received. This value should be unique for each request, 
	 * 		re-using a RequestID will overwrite any previous request using the same ID. 
	 * @param dataDefinitionId Specifies the ID of the client defined data definition. 
	 * @param radiusMeters Double word containing the radius in meters. If this is set to zero then 
	 * 		information on all relevant objects will be returned. This value is ignored if type is 
	 * 		set to {@link SimObjectType#USER}. The error {@link SimConnectException#OUT_OF_BOUNDS} 
	 * 		will be returned if a radius is given and it exceeds the maximum allowed (200 Km).
	 * @param type Specifies the type of object to receive information on. One member of the 
	 * 		{@link SimObjectType#USER} enumeration type.
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType, float, int)
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod, int, int, int, int)
	 * @throws IOException
	 * @since 0.4
	 */
	public synchronized void requestDataOnSimObjectType(Enum dataRequestId,
			Enum dataDefinitionId, int radiusMeters, SimObjectType type) throws IOException {
		requestDataOnSimObjectType(dataRequestId.ordinal(), dataDefinitionId.ordinal(), radiusMeters, type);
	}

	/**
	 * The <code>subscribeToSystemEvent</code> function is used to request that a specific system 
	 * event is notified to the client.
	 * 
	 * <p> Notifications of the events are returned in a {@link RecvEvent} structure or one
	 * of its subclasses </p>
	 * 
	 * @param clientEventID Specifies the client-defined event ID. 
	 * @param eventName The string name for the requested system event
	 * @see #unsubscribeFromSystemEvent(int)
	 * @see RecvEvent
	 * @see RecvEventFrame
	 * @see RecvEventFilename
	 * @throws IOException
	 */
	public synchronized void subscribeToSystemEvent(int clientEventID, 
			String eventName) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(clientEventID);
		putString(writeBuffer, eventName, 256);
		sendPacket(0x17);
	}

	/**
	 * The <code>subscribeToSystemEvent</code> function is used to request that a specific system 
	 * event is notified to the client.
	 * 
	 * <p> Notifications of the events are returned in a {@link RecvEvent} structure or one
	 * of its subclasses </p>
	 * 
	 * @param clientEventID Specifies the client-defined event ID. 
	 * @param eventName The string name for the requested system event
	 * @see #unsubscribeFromSystemEvent(int)
	 * @see RecvEvent
	 * @see RecvEventFrame
	 * @see RecvEventFilename
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void subscribeToSystemEvent(Enum clientEventID, 
			String eventName) throws IOException {
		subscribeToSystemEvent(clientEventID.ordinal(), eventName);
	}

	/**
	 * The <code>unsubscribeFromSystemEvent</code> method is used to request that notifications 
	 * are no longer received for the specified system event.
	 * 
	 * <p> There is no limit to the number of system events that can be subscribed to, but use 
	 * this function to improve performance when a system event notification is no longer needed.</p>
	 * 
	 * @param clientEventID Specifies the client-defined event ID.
	 * @throws IOException
	 */
	public synchronized void unsubscribeFromSystemEvent(int clientEventID) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(clientEventID);
		sendPacket(0x18);
	}

	/**
	 * The <code>unsubscribeFromSystemEvent</code> method is used to request that notifications 
	 * are no longer received for the specified system event.
	 * 
	 * <p> There is no limit to the number of system events that can be subscribed to, but use 
	 * this function to improve performance when a system event notification is no longer needed.</p>
	 * 
	 * @param clientEventID Specifies the client-defined event ID.
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void unsubscribeFromSystemEvent(Enum clientEventID) throws IOException {
		unsubscribeFromSystemEvent(clientEventID.ordinal());
	}

	/**
	 * The <code>requestSystemState</code> function is used to request information from a number of 
	 * Flight Simulator system components. 
	 * 
	 * <p> The information requested will be returned in a {@link RecvSystemState} structure. </p>
	 * 
	 * <p> <h4> List of system events : </h4>
	 * <table border="0" cellpading="1" cellspaing="0" summary="System events">
	 * <tr align="left">
	 * <th bgcolor="#CCCCFF" align="left" id="String">String</th>
	 * <th bgcolor="#CCCCFF" align="left" id="Description">Description</th>
	 * </tr>
	 * 
	 * <tr><td><b>AircraftLoaded</b></td><td> Requests the full path name of the last loaded aircraft flight 
	 * dynamics file. These files have a .AIR extension. </td></tr>
	 * <tr><td><b>DialogMode</b></td><td>  	Requests whether the simulation is in Dialog mode or not. 
	 * See {@link #setSystemState(String, int, float, String)} for a description of Dialog mode.</td></tr>
	 * <tr><td><b>FlightLoaded</b></td><td> Requests the full path name of the last loaded flight. Flight files 
	 * have the extension .FLT. </td></tr>
	 * <tr><td><b>FlightPlan</b></td><td> 	Requests the full path name of the active flight plan. An empty string 
	 * will be returned if there is no active flight plan. </td></tr>
	 * <tr><td><b>Sim</b></td><td>  	Requests the state of the simulation. If 1 is returned, the user is 
	 * in control of the aircraft, if 0 is returned, the user is navigating the UI. This is the same state 
	 * that notifications can be subscribed to with the <b>SimStart</b> and <b>SimStop</b> string with the 
	 * {@link #subscribeToSystemEvent(int, String)} function. </td></tr>
	 * </table> </p>
	 * 
	 * @param dataRequestID  The client defined request ID.
	 * @param state A string identifying the system function. 
	 * @see #setSystemState(String, int, float, String)
	 * @throws IOException
	 */
	public synchronized void requestSystemState(int dataRequestID, String state) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(dataRequestID);
		putString(writeBuffer, state, 256);
		sendPacket(0x35);
	}

	/**
	 * The <code>requestSystemState</code> function is used to request information from a number of 
	 * Flight Simulator system components. 
	 * 
	 * <p> The information requested will be returned in a {@link RecvSystemState} structure. </p>
	 * 
	 * <p> <h4> List of system events : </h4>
	 * <table border="0" cellpading="1" cellspaing="0" summary="System events">
	 * <tr align="left">
	 * <th bgcolor="#CCCCFF" align="left" id="String">String</th>
	 * <th bgcolor="#CCCCFF" align="left" id="Description">Description</th>
	 * </tr>
	 * 
	 * <tr><td><b>AircraftLoaded</b></td><td> Requests the full path name of the last loaded aircraft flight 
	 * dynamics file. These files have a .AIR extension. </td></tr>
	 * <tr><td><b>DialogMode</b></td><td>  	Requests whether the simulation is in Dialog mode or not. 
	 * See {@link #setSystemState(String, int, float, String)} for a description of Dialog mode.</td></tr>
	 * <tr><td><b>FlightLoaded</b></td><td> Requests the full path name of the last loaded flight. Flight files 
	 * have the extension .FLT. </td></tr>
	 * <tr><td><b>FlightPlan</b></td><td> 	Requests the full path name of the active flight plan. An empty string 
	 * will be returned if there is no active flight plan. </td></tr>
	 * <tr><td><b>Sim</b></td><td>  	Requests the state of the simulation. If 1 is returned, the user is 
	 * in control of the aircraft, if 0 is returned, the user is navigating the UI. This is the same state 
	 * that notifications can be subscribed to with the <b>SimStart</b> and <b>SimStop</b> string with the 
	 * {@link #subscribeToSystemEvent(int, String)} function. </td></tr>
	 * </table> </p>
	 * 
	 * @param dataRequestID  The client defined request ID.
	 * @param state A string identifying the system function. 
	 * @see #setSystemState(String, int, float, String)
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void requestSystemState(Enum dataRequestID, String state) throws IOException {
		requestSystemState(dataRequestID.ordinal(), state);
	}

	/**
	 * The <code>setSystemState</code> method is used to access a number of Flight Simulator 
	 * system components.
	 * 
	 * <p> The integer, float and string set with this function match those in the 
	 * {@link RecvSystemState} structure (which is returned if the information is requested 
	 * with the {@link #requestSystemState(int, String)} call. </p>
	 * 
	 * @param state A string identifying the system function. 
	 * @param paramInt An integer value, set depending on the value of <b>state</b> (not currently used).
	 * @param paramFloat A float value, set depending on the value of <b>state</b> (not currently used).
	 * @param paramString A string value, set depending on the value of <b>state</b>.
	 * @see #subscribeToSystemEvent(int, String)
	 * @throws IOException
	 */
	public synchronized void setSystemState(String state, int paramInt,
			float paramFloat, String paramString) throws IOException {
		clean(writeBuffer);
		putString(writeBuffer, state, 256);
		writeBuffer.putInt(paramInt);
		writeBuffer.putFloat(paramFloat);
		putString(writeBuffer, paramString, 256);
		writeBuffer.putInt(0);
		sendPacket(0x36);
	}
	
	/**
	 * Shortcut to add an individual unmaskable client defined event to a notification group.
	 * See {@link #addClientEventToNotificationGroup(int, int, boolean)}
	 * 
	 * @param notificationGroupID See {@link #addClientEventToNotificationGroup(int, int, boolean)}
	 * @param clientEventID See {@link #addClientEventToNotificationGroup(int, int, boolean)}
	 * @see #addClientEventToNotificationGroup(int, int, boolean)
	 * @throws IOException
	 */
	public synchronized void addClientEventToNotificationGroup(int notificationGroupID,
			int clientEventID) throws IOException {
		addClientEventToNotificationGroup(notificationGroupID, clientEventID, false);
	}

	/**
	 * Shortcut to add an individual unmaskable client defined event to a notification group.
	 * See {@link #addClientEventToNotificationGroup(int, int, boolean)}
	 * 
	 * @param notificationGroupID See {@link #addClientEventToNotificationGroup(int, int, boolean)}
	 * @param clientEventID See {@link #addClientEventToNotificationGroup(int, int, boolean)}
	 * @see #addClientEventToNotificationGroup(int, int, boolean)
	 * @throws IOException
	 * @since 0.4
	 */
	public synchronized void addClientEventToNotificationGroup(Enum notificationGroupID,
			Enum clientEventID) throws IOException {
		addClientEventToNotificationGroup(notificationGroupID.ordinal(), clientEventID.ordinal(), false);
	}

	/**
	 * The <code>addClientEventToNotificationGroup</code> function is used to add an individual 
	 * client defined event to a notification group.
	 * 
	 * <p> The maximum number of events that can be added to a notfication group is 1000. A notification 
	 * group is simply a convenient way of  setting the appropriate priority for a range of events, 
	 * and all client events must be assigned to a notification group before any event notifications 
	 * will be received from the SimConnect server. </p>
	 * 
	 * @param notificationGroupID Specifies the ID of the client defined group. 
	 * @param clientEventID Specifies the ID of the client defined event. 
	 * @param maskable Boolean, <b>true</b> indicates that the event will be masked by this 
	 * 			client and will not be transmitted to any more clients, possibly including 
	 * 			Flight Simulator itself (if the priority of the client exceeds that of Flight Simulator). 
	 * 			<b>False</b> is the default. See the explanation of {@link NotificationPriority}.
	 * @see #removeClientEvent(int, int)
	 * @see #setNotificationGroupPriority(int, NotificationPriority)
	 * @see #clearNotificationGroup(int)
	 * @throws IOException
	 */
	public synchronized void addClientEventToNotificationGroup(int notificationGroupID,
			int clientEventID, boolean maskable) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(notificationGroupID);
		writeBuffer.putInt(clientEventID);
		writeBuffer.putInt(maskable ? 1 : 0);
		sendPacket(0x07);
	}

	/**
	 * The <code>addClientEventToNotificationGroup</code> function is used to add an individual 
	 * client defined event to a notification group.
	 * 
	 * <p> The maximum number of events that can be added to a notfication group is 1000. A notification 
	 * group is simply a convenient way of  setting the appropriate priority for a range of events, 
	 * and all client events must be assigned to a notification group before any event notifications 
	 * will be received from the SimConnect server. </p>
	 * 
	 * @param notificationGroupID Specifies the ID of the client defined group. 
	 * @param clientEventID Specifies the ID of the client defined event. 
	 * @param maskable Boolean, <b>true</b> indicates that the event will be masked by this 
	 * 			client and will not be transmitted to any more clients, possibly including 
	 * 			Flight Simulator itself (if the priority of the client exceeds that of Flight Simulator). 
	 * 			<b>False</b> is the default. See the explanation of {@link NotificationPriority}.
	 * @see #removeClientEvent(int, int)
	 * @see #setNotificationGroupPriority(int, NotificationPriority)
	 * @see #clearNotificationGroup(int)
	 * @throws IOException
	 */
	public synchronized void addClientEventToNotificationGroup(Enum notificationGroupID,
			Enum clientEventID, boolean maskable) throws IOException {
		addClientEventToNotificationGroup(notificationGroupID.ordinal(), clientEventID.ordinal(), maskable);
	}


	/**
	 * The <code>mapClientEventToSimEvent</code> method associates a client defined event ID 
	 * with a Flight Simulator event name.
	 * 
	 * <p> Client events must be added to a group event (to set the appropriate priority) 
	 * before event notifications will be received from the SimConnect server (see the 
	 * {@link #addClientEventToNotificationGroup(int, int, boolean)} function). </p>
	 * 
	 * @param clientEventId Specifies the ID of the client event. 
	 * @see #addClientEventToNotificationGroup(int, int, boolean)
	 * @see #transmitClientEvent(int, int, int, int, int)
	 * @throws IOException
	 */
	public synchronized void mapClientEventToSimEvent(int clientEventId) throws IOException {
		mapClientEventToSimEvent(clientEventId, ""); //$NON-NLS-1$
	}

	/**
	 * The <code>mapClientEventToSimEvent</code> method associates a client defined event ID 
	 * with a Flight Simulator event name.
	 * 
	 * <p> Client events must be added to a group event (to set the appropriate priority) 
	 * before event notifications will be received from the SimConnect server (see the 
	 * {@link #addClientEventToNotificationGroup(int, int, boolean)} function). </p>
	 * 
	 * @param clientEventId Specifies the ID of the client event. 
	 * @see #addClientEventToNotificationGroup(int, int, boolean)
	 * @see #transmitClientEvent(int, int, int, int, int)
	 * @throws IOException
	 * @since 0.4
	 */
	public synchronized void mapClientEventToSimEvent(Enum clientEventId) throws IOException {
		mapClientEventToSimEvent(clientEventId.ordinal(), ""); //$NON-NLS-1$
	}

	/**
	 * The <code>mapClientEventToSimEvent</code> method associates a client defined event ID 
	 * with a Flight Simulator event name.
	 * 
	 * <p> Client events must be added to a group event (to set the appropriate priority) 
	 * before event notifications will be received from the SimConnect server (see the 
	 * {@link #addClientEventToNotificationGroup(int, int, boolean)} function). </p>
	 * 
	 * @param clientEventId Specifies the ID of the client event. 
	 * @param eventName Specifies the Flight Simulator event name. Refer to the Event 
	 * 		IDs document for a list of event names (listed under SimConnect Name). If the event name 
	 * 		includes one or more periods (such as "Client.Event" in the example below) then they are 
	 * 		custom events specified by the client, and will only be recognised by another client (and 
	 * 		not Flight Simulator) that has been coded to receive such events. No Flight Simulator events 
	 * 		include periods. If no entry is made for this parameter, the event is private to the client.
	 * @see #addClientEventToNotificationGroup(int, int, boolean)
	 * @see #transmitClientEvent(int, int, int, int, int)
	 * @throws IOException
	 */
	public synchronized void mapClientEventToSimEvent(int clientEventId, 
			String eventName) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(clientEventId);
		putString(writeBuffer, eventName, 256);
		sendPacket(0x04);
	}

	/**
	 * The <code>mapClientEventToSimEvent</code> method associates a client defined event ID 
	 * with a Flight Simulator event name.
	 * 
	 * <p> Client events must be added to a group event (to set the appropriate priority) 
	 * before event notifications will be received from the SimConnect server (see the 
	 * {@link #addClientEventToNotificationGroup(int, int, boolean)} function). </p>
	 * 
	 * @param clientEventId Specifies the ID of the client event. 
	 * @param eventName Specifies the Flight Simulator event name. Refer to the Event 
	 * 		IDs document for a list of event names (listed under SimConnect Name). If the event name 
	 * 		includes one or more periods (such as "Client.Event" in the example below) then they are 
	 * 		custom events specified by the client, and will only be recognised by another client (and 
	 * 		not Flight Simulator) that has been coded to receive such events. No Flight Simulator events 
	 * 		include periods. If no entry is made for this parameter, the event is private to the client.
	 * @see #addClientEventToNotificationGroup(int, int, boolean)
	 * @see #transmitClientEvent(int, int, int, int, int)
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void mapClientEventToSimEvent(Enum clientEventId, 
			String eventName) throws IOException {
		mapClientEventToSimEvent(clientEventId.ordinal(), eventName);
	}

	/**
	 * The <code>transmitClientEvent</code> function is used to request that the Flight Simulator 
	 * server transmit to all SimConnect clients the specified client event.
	 * 
	 * <p> Typically use this function to transmit an event to other SimConnect clients, 
	 * including the simulation engine of Flight Simulator, although the client that transmits the 
	 * event can also receive it. The order in which client notification groups are informed of the 
	 * event is determined by the priority of each group. The higher the priority of the group, the 
	 * earlier it will receive the event notification. Refer to the explanation of the maskable 
	 * parameter for the {@link #addClientEventToNotificationGroup(int, int, boolean)} call, which 
	 * describes when the event may be masked and not transmitted to lower priority groups.  
	 * Also see the explanation of {@link NotificationPriority}. </p>
	 * 
	 * 
	 * @param objectID Specifies the ID of the server defined object. If this parameter is set to 
	 * 		{@link SimConnectConstants#OBJECT_ID_USER}, then the transmitted event will be sent to 
	 * 		the other clients in priority order. If this parameters contains another object ID, then 
	 * 		the event will be sent direct to that sim-object, and no other clients will receive it.
	 * @param eventID Specifies the ID of the client event. 
	 * @param data integer containing any additional number required by the event. This is often 
	 * 			zero. If the event is a Flight Simulator event, then refer to the Event IDs document 
	 * 			for information on this additional value. If the event is a custom event, then any 
	 * 			value put in this parameter will be available to the clients that receive the event.
	 * @param groupID The default behavior is that this specifies the GroupID of the event. The 
	 * 			SimConnect server will use the priority of this group to send the messasge to all 
	 * 			clients with a lower priority. To receive the event notification other SimConnect 
	 * 			clients must have subscribed to receive the event.  See the explanation of
	 * 			{@link NotificationPriority}. The exception to the default behaviour is set by the
	 * 			{@link SimConnectConstants#EVENT_FLAG_GROUPID_IS_PRIORITY} flag
	 * @param flags One of the following flags : {@link SimConnectConstants#EVENT_FLAG_DEFAULT},
	 * 			{@link SimConnectConstants#EVENT_FLAG_FAST_REPEAT_TIMER},
	 * 			{@link SimConnectConstants#EVENT_FLAG_GROUPID_IS_PRIORITY},
	 * 			{@link SimConnectConstants#EVENT_FLAG_SLOW_REPEAT_TIMER}
	 * @see #mapClientEventToSimEvent(int, String)
	 * @throws IOException
	 */
	public synchronized void transmitClientEvent(int objectID, int eventID, 
			int data, int groupID, int flags) throws IOException {
		// packet size 0x24
		// packet id 0x05
		
		clean(writeBuffer);
		writeBuffer.putInt(objectID);
		writeBuffer.putInt(eventID);
		writeBuffer.putInt(data);
		writeBuffer.putInt(groupID);
		writeBuffer.putInt(flags);
		sendPacket(0x05);
	}	

	/**
	 * The <code>transmitClientEvent</code> function is used to request that the Flight Simulator 
	 * server transmit to all SimConnect clients the specified client event.
	 * 
	 * <p> Typically use this function to transmit an event to other SimConnect clients, 
	 * including the simulation engine of Flight Simulator, although the client that transmits the 
	 * event can also receive it. The order in which client notification groups are informed of the 
	 * event is determined by the priority of each group. The higher the priority of the group, the 
	 * earlier it will receive the event notification. Refer to the explanation of the maskable 
	 * parameter for the {@link #addClientEventToNotificationGroup(int, int, boolean)} call, which 
	 * describes when the event may be masked and not transmitted to lower priority groups.  
	 * Also see the explanation of {@link NotificationPriority}. </p>
	 * 
	 * 
	 * @param objectID Specifies the ID of the server defined object. If this parameter is set to 
	 * 		{@link SimConnectConstants#OBJECT_ID_USER}, then the transmitted event will be sent to 
	 * 		the other clients in priority order. If this parameters contains another object ID, then 
	 * 		the event will be sent direct to that sim-object, and no other clients will receive it.
	 * @param eventID Specifies the ID of the client event. 
	 * @param data integer containing any additional number required by the event. This is often 
	 * 			zero. If the event is a Flight Simulator event, then refer to the Event IDs document 
	 * 			for information on this additional value. If the event is a custom event, then any 
	 * 			value put in this parameter will be available to the clients that receive the event.
	 * @param groupID The default behavior is that this specifies the GroupID of the event. The 
	 * 			SimConnect server will use the priority of this group to send the messasge to all 
	 * 			clients with a lower priority. To receive the event notification other SimConnect 
	 * 			clients must have subscribed to receive the event.  See the explanation of
	 * 			{@link NotificationPriority}. The exception to the default behaviour is set by the
	 * 			{@link SimConnectConstants#EVENT_FLAG_GROUPID_IS_PRIORITY} flag
	 * @param flags One of the following flags : {@link SimConnectConstants#EVENT_FLAG_DEFAULT},
	 * 			{@link SimConnectConstants#EVENT_FLAG_FAST_REPEAT_TIMER},
	 * 			{@link SimConnectConstants#EVENT_FLAG_GROUPID_IS_PRIORITY},
	 * 			{@link SimConnectConstants#EVENT_FLAG_SLOW_REPEAT_TIMER}
	 * @see #mapClientEventToSimEvent(int, String)
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void transmitClientEvent(int objectID, Enum eventID, 
			int data, Enum groupID, int flags) throws IOException {
		transmitClientEvent(objectID, eventID.ordinal(), data, groupID.ordinal(), flags);
	}	

	/**
	 * The <code> setSystemEventState</code> function is used to turn requests for event information 
	 * from the server on and off.
	 * 
	 * <p> If this function is not called, the default is for the state to be on. This is different 
	 * from input events, which have a default state of off. </p>
	 * 
	 * <p> Use this function to turn system events temporarily on and off, rather than make multiple 
	 * calls to {@link #subscribeToSystemEvent(int, String)} and {@link #unsubscribeFromSystemEvent(int)}, 
	 * which is less efficient </p>
	 * 
	 * @param eventID Specifies the ID of the client event that is to have its state changed. 
	 * @param state true to activate
	 * @throws IOException
	 */
	public synchronized void setSystemEventState(int eventID, boolean state) throws IOException {
		// packet size 0x18
		// packet id 0x06
		
		clean(writeBuffer);
		writeBuffer.putInt(eventID);
		writeBuffer.putInt(state ? 1 : 0);
		sendPacket(0x06);
	}

	/**
	 * The <code> setSystemEventState</code> function is used to turn requests for event information 
	 * from the server on and off.
	 * 
	 * <p> If this function is not called, the default is for the state to be on. This is different 
	 * from input events, which have a default state of off. </p>
	 * 
	 * <p> Use this function to turn system events temporarily on and off, rather than make multiple 
	 * calls to {@link #subscribeToSystemEvent(int, String)} and {@link #unsubscribeFromSystemEvent(int)}, 
	 * which is less efficient </p>
	 * 
	 * @param eventID Specifies the ID of the client event that is to have its state changed. 
	 * @param state true to activate
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void setSystemEventState(Enum eventID, boolean state) throws IOException {
		setSystemEventState(eventID.ordinal(), state);
	}

	/**
	 * The <code>removeClientEvent</code> function is used to remove a client defined event 
	 * from a notification group.
	 * 
	 * <p> Use this function to permanently remove the client event. There is no reliable procedure 
	 * to temporarily turn off a client event. </p>
	 * 
	 * @param groupID Specifies the ID of the client defined group. 
	 * @param eventID Specifies the ID of the client defined event ID that is to be removed from the group.
	 * @see #addClientEventToNotificationGroup(int, int, boolean)
	 * @see #setNotificationGroupPriority(int, NotificationPriority)
	 * @see #clearNotificationGroup(int)
	 * @throws IOException
	 */
	public synchronized void removeClientEvent(int groupID, int eventID) throws IOException {
		// packet size 0x18
		// packet id 0x08
		
		clean(writeBuffer);
		writeBuffer.putInt(eventID);
		writeBuffer.putInt(eventID);
		sendPacket(0x08);
	}
	
	/**
	 * The <code>removeClientEvent</code> function is used to remove a client defined event 
	 * from a notification group.
	 * 
	 * <p> Use this function to permanently remove the client event. There is no reliable procedure 
	 * to temporarily turn off a client event. </p>
	 * 
	 * @param groupID Specifies the ID of the client defined group. 
	 * @param eventID Specifies the ID of the client defined event ID that is to be removed from the group.
	 * @see #addClientEventToNotificationGroup(int, int, boolean)
	 * @see #setNotificationGroupPriority(int, NotificationPriority)
	 * @see #clearNotificationGroup(int)
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void removeClientEvent(Enum groupID, Enum eventID) throws IOException {
		removeClientEvent(groupID.ordinal(), eventID.ordinal());
	}

	
	/**
	 * The <coce>setNotificationGroupPriority</code> function is used to set the priority for a 
	 * notification group.
	 * 
	 * <p> {@link NotificationPriority} for a list of notification group priority. Each notifiction 
	 * group has an assigned priority, and the SimConnect server will send events out strictly in 
	 * the order of priority. No two groups will be set at the same priority. If a request is recieved 
	 * for a group to be set at a priority that has already been taken, the group will be assigned the 
	 * next lowest priority that is available. This includes groups from all the clients that have 
	 * opened communications with the server. </p>
	 * 
	 * <p> If a group has an assigned priority above {@link NotificationPriority#HIGHEST_MASKABLE} 
	 * then it cannot mask events (hide them from other clients). If the group has a priority equal 
	 * to or below {@link NotificationPriority#HIGHEST_MASKABLE} , then events can be masked 
	 * (the maskable flag must be set by the {@link #addClientEventToNotificationGroup(int, int, boolean)} 
	 * function to do this). Note that it is possible to mask Flight Simulator events, and therefore 
	 * intercept them before they reach the simulation engine, and perhaps send new events to the 
	 * simulation engine after appropriate processing has been done. Flight Simulator's simulation 
	 * engine is treated as as SimConnect client in this regard, with a priority of 
	 * {@link NotificationPriority#DEFAULT}. </p>
	 * 
	 * <p> Input group events work in a similar manner. The priority groups are not combined though, 
	 * a group and an input group can both have the same priority number. The SimConnect server manages 
	 * two lists: notification groups and input groups. </p>
	 * 
	 * <p> A typical use of masking is to prevent Flight Simulator itself from receiving an event, in 
	 * order for the SimConnect client to completely replace the fucntionality in this case. Another use 
	 * of masking is with co-operative clients, where there are multiple versions (perhaps a deluxe and 
	 * standard version, or later and earlier versions), where the deluxe or later version might need to 
	 * mask events from the other client, if they are both up and running. Flight Simulator does not mask 
	 * any events.  </>p
	 * 
	 * @param groupID  Specifies the ID of the client defined group. 
	 * @param priority Requests the group's priority
	 * @throws IOException
	 */
	public synchronized void setNotificationGroupPriority(int groupID, 
			NotificationPriority priority) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(groupID);
		writeBuffer.putInt(priority.value());
		sendPacket(0x09);
	}

	/**
	 * The <coce>setNotificationGroupPriority</code> function is used to set the priority for a 
	 * notification group.
	 * 
	 * <p> {@link NotificationPriority} for a list of notification group priority. Each notifiction 
	 * group has an assigned priority, and the SimConnect server will send events out strictly in 
	 * the order of priority. No two groups will be set at the same priority. If a request is recieved 
	 * for a group to be set at a priority that has already been taken, the group will be assigned the 
	 * next lowest priority that is available. This includes groups from all the clients that have 
	 * opened communications with the server. </p>
	 * 
	 * <p> If a group has an assigned priority above {@link NotificationPriority#HIGHEST_MASKABLE} 
	 * then it cannot mask events (hide them from other clients). If the group has a priority equal 
	 * to or below {@link NotificationPriority#HIGHEST_MASKABLE} , then events can be masked 
	 * (the maskable flag must be set by the {@link #addClientEventToNotificationGroup(int, int, boolean)} 
	 * function to do this). Note that it is possible to mask Flight Simulator events, and therefore 
	 * intercept them before they reach the simulation engine, and perhaps send new events to the 
	 * simulation engine after appropriate processing has been done. Flight Simulator's simulation 
	 * engine is treated as as SimConnect client in this regard, with a priority of 
	 * {@link NotificationPriority#DEFAULT}. </p>
	 * 
	 * <p> Input group events work in a similar manner. The priority groups are not combined though, 
	 * a group and an input group can both have the same priority number. The SimConnect server manages 
	 * two lists: notification groups and input groups. </p>
	 * 
	 * <p> A typical use of masking is to prevent Flight Simulator itself from receiving an event, in 
	 * order for the SimConnect client to completely replace the fucntionality in this case. Another use 
	 * of masking is with co-operative clients, where there are multiple versions (perhaps a deluxe and 
	 * standard version, or later and earlier versions), where the deluxe or later version might need to 
	 * mask events from the other client, if they are both up and running. Flight Simulator does not mask 
	 * any events.  </>p
	 * 
	 * @param groupID  Specifies the ID of the client defined group. 
	 * @param priority Requests the group's priority
	 * @throws IOException
	 */
	public synchronized void setNotificationGroupPriority(Enum groupID, 
			NotificationPriority priority) throws IOException {
		setNotificationGroupPriority(groupID.ordinal(), priority);
	}

	/**
	 * Remove all the client defined events from a notification group.
	 * <p>
	 * <b>Remark</b> There is a maximum of 20 notification groups in any SimConnect client. Use this function if the maximum has been reached, but one or more are not longer required.
	 * @param groupID Specifies the ID of the client defined group that is to have all its events removed.
	 * @see #addClientEventToNotificationGroup(int, int)
	 * @see #removeClientEvent(int, int)
	 * @see #setNotificationGroupPriority(int, NotificationPriority)
	 * @throws IOException
	 */
	public synchronized void clearNotificationGroup(int groupID) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(groupID);
		sendPacket(0x0A);
	}

	/**
	 * The <code>requestNotificationGroup</code> function is used to request events are transmitted 
	 * from a notification group, when the simulation is in Dialog Mode.
	 * @param groupID Specifies the ID of the client defined group. 
	 * @see #addClientEventToNotificationGroup(int, int)
	 * @see #clearNotificationGroup(int)
	 * @see #setNotificationGroupPriority(int, NotificationPriority)
	 * @throws IOException
	 */
	public synchronized void requestNotificationGroup(int groupID) throws IOException {
		requestNotificationGroup(groupID, 0, 0);
	}

	/**
	 * The <code>requestNotificationGroup</code> function is used to request events are transmitted 
	 * from a notification group, when the simulation is in Dialog Mode.
	 * @param groupID Specifies the ID of the client defined group. 
	 * @param reserved Reserved for future use.
	 * @param flags Reserved for future use.
	 * @see #addClientEventToNotificationGroup(int, int)
	 * @see #clearNotificationGroup(int)
	 * @see #setNotificationGroupPriority(int, NotificationPriority)
	 * @throws IOException
	 */
	public synchronized void requestNotificationGroup(int groupID, int reserved, int flags) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(groupID);
		writeBuffer.putInt(reserved);
		writeBuffer.putInt(flags);
		sendPacket(0x0B);
	}
	
	/**
	 * The <code>setDataOnSimObject</code> function is used to make changes to the data properties of an object.
	 * <p> The data that is set on an object is defined in a data definition 
	 * (see {@link #addToDataDefinition(int, String, String, SimConnectDataType)}). 
	 * This data can include the following structures: {@link flightsim.simconnect.data.Waypoint},
	 * {@link InitPosition} and {@link flightsim.simconnect.data.MarkerState}. Any number 
	 * of waypoints can be given to an AI object using a single call to this function, and
	 * any number of marker state structures can also be combined into an array. </p>
	 * 
	 * <p> The Simulation Variables document includes a column indicating whether variables 
	 * can be written to or not. An exception will be returned if an attempt is made to write 
	 * to a variable that cannot be set in this way.  </p>
	 * 
	 * @param dataDefinitionID Specifies the ID of the client defined data definition.
	 * @param objectID Specifies the ID of the Flight Simulator object that the data should be about. This ID can be SIMCONNECT_OBJECT_ID_USER (to specify the user's aircraft) or obtained from a SIMCONNECT_RECV_SIMOBJECT_DATA_BYTYPE structure after a call to SimConnect_RequestDataOnSimObjectType.
	 * @param tagged The data to be set is being sent in tagged format. Refer to SimConnect_RequestDataOnSimObject for more details on the tagged format. 
	 * @param arrayCount Specifies the number of elements in the data array. A count of zero is interpreted as one element. 
	 * @param data data array
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType)
	 * @see #clearDataDefinition(int)
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod)
	 * @see #requestDataOnSimObjectType(int, int, int, SimObjectType)
	 * @throws IOException
	 */
	public synchronized void setDataOnSimObject(int dataDefinitionID, int objectID, 
			boolean tagged, int arrayCount, byte [] data) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(dataDefinitionID);
		writeBuffer.putInt(objectID);
		writeBuffer.putInt(tagged ? DATA_SET_FLAG_TAGGED : DATA_SET_FLAG_DEFAULT);
		if (arrayCount == 0) arrayCount = 1;
		writeBuffer.putInt(arrayCount);
		writeBuffer.putInt(data.length);
		writeBuffer.put(data);
		sendPacket(0x10);
	}

	/**
	 * The <code>setDataOnSimObject</code> function is used to make changes to the data properties of an object.
	 * <p> The data that is set on an object is defined in a data definition 
	 * (see {@link #addToDataDefinition(int, String, String, SimConnectDataType)}). 
	 * This data can include the following structures: {@link flightsim.simconnect.data.Waypoint},
	 * {@link InitPosition} and {@link flightsim.simconnect.data.MarkerState}. Any number 
	 * of waypoints can be given to an AI object using a single call to this function, and
	 * any number of marker state structures can also be combined into an array. </p>
	 * 
	 * <p> The Simulation Variables document includes a column indicating whether variables 
	 * can be written to or not. An exception will be returned if an attempt is made to write 
	 * to a variable that cannot be set in this way.  </p>
	 * 
	 * @param dataDefinitionID Specifies the ID of the client defined data definition.
	 * @param objectID Specifies the ID of the Flight Simulator object that the data should be about. This ID can be SIMCONNECT_OBJECT_ID_USER (to specify the user's aircraft) or obtained from a SIMCONNECT_RECV_SIMOBJECT_DATA_BYTYPE structure after a call to SimConnect_RequestDataOnSimObjectType.
	 * @param tagged The data to be set is being sent in tagged format. Refer to SimConnect_RequestDataOnSimObject for more details on the tagged format. 
	 * @param arrayCount Specifies the number of elements in the data array. A count of zero is interpreted as one element. 
	 * @param data data array
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType)
	 * @see #clearDataDefinition(int)
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod)
	 * @see #requestDataOnSimObjectType(int, int, int, SimObjectType)
	 * @throws IOException
	 * @since 0.4
	 */
	public synchronized void setDataOnSimObject(Enum dataDefinitionID, int objectID, 
			boolean tagged, int arrayCount, byte [] data) throws IOException {
		setDataOnSimObject(dataDefinitionID.ordinal(),objectID, tagged, arrayCount, data);
	}

	/**
	 * The <code>setDataOnSimObject</code> function is used to make changes to the data properties of an object.
	 * <p> The data that is set on an object is defined in a data definition 
	 * (see {@link #addToDataDefinition(int, String, String, SimConnectDataType)}). 
	 * This data can include the following structures: {@link flightsim.simconnect.data.Waypoint},
	 * {@link InitPosition} and {@link flightsim.simconnect.data.MarkerState}. Any number 
	 * of waypoints can be given to an AI object using a single call to this function, and
	 * any number of marker state structures can also be combined into an array. </p>
	 * 
	 * <p> The Simulation Variables document includes a column indicating whether variables 
	 * can be written to or not. An exception will be returned if an attempt is made to write 
	 * to a variable that cannot be set in this way.  </p>
	 * 
	 * @param dataDefinitionID Specifies the ID of the client defined data definition.
	 * @param objectID Specifies the ID of the Flight Simulator object that the data should be about. This ID can be SIMCONNECT_OBJECT_ID_USER (to specify the user's aircraft) or obtained from a SIMCONNECT_RECV_SIMOBJECT_DATA_BYTYPE structure after a call to SimConnect_RequestDataOnSimObjectType.
	 * @param tagged The data to be set is being sent in tagged format. Refer to SimConnect_RequestDataOnSimObject for more details on the tagged format. 
	 * @param arrayCount Specifies the number of elements in the data array. A count of zero is interpreted as one element. 
	 * @param data ByteBuffer of data to write. Write start at current position
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType)
	 * @see #clearDataDefinition(int)
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod)
	 * @see #requestDataOnSimObjectType(int, int, int, SimObjectType)
	 * @since 0.2
	 * @throws IOException
	 */
	public synchronized void setDataOnSimObject(int dataDefinitionID, int objectID, 
			boolean tagged, int arrayCount, ByteBuffer data) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(dataDefinitionID);
		writeBuffer.putInt(objectID);
		writeBuffer.putInt(tagged ? SimConnectConstants.DATA_SET_FLAG_TAGGED : SimConnectConstants.DATA_SET_FLAG_DEFAULT);
		if (arrayCount == 0) arrayCount = 1;
		writeBuffer.putInt(arrayCount);
		writeBuffer.putInt(data.remaining());
		writeBuffer.put(data);
		sendPacket(0x10);
	}

	/**
	 * The <code>setDataOnSimObject</code> function is used to make changes to the data properties of an object.
	 * <p> The data that is set on an object is defined in a data definition 
	 * (see {@link #addToDataDefinition(int, String, String, SimConnectDataType)}). 
	 * This data can include the following structures: {@link flightsim.simconnect.data.Waypoint},
	 * {@link InitPosition} and {@link flightsim.simconnect.data.MarkerState}. Any number 
	 * of waypoints can be given to an AI object using a single call to this function, and
	 * any number of marker state structures can also be combined into an array. </p>
	 * 
	 * <p> The Simulation Variables document includes a column indicating whether variables 
	 * can be written to or not. An exception will be returned if an attempt is made to write 
	 * to a variable that cannot be set in this way.  </p>
	 * 
	 * @param dataDefinitionID Specifies the ID of the client defined data definition.
	 * @param objectID Specifies the ID of the Flight Simulator object that the data should be about. This ID can be SIMCONNECT_OBJECT_ID_USER (to specify the user's aircraft) or obtained from a SIMCONNECT_RECV_SIMOBJECT_DATA_BYTYPE structure after a call to SimConnect_RequestDataOnSimObjectType.
	 * @param data Array of SimConnectData structures suitable for writing
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType)
	 * @see #clearDataDefinition(int)
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod)
	 * @see #requestDataOnSimObjectType(int, int, int, SimObjectType)
	 * @since 0.3
	 * @throws IOException
	 */
	public synchronized void setDataOnSimObject(int dataDefinitionID, int objectID, 
			SimConnectData data[]) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(dataDefinitionID);
		writeBuffer.putInt(objectID);
		writeBuffer.putInt(SimConnectConstants.DATA_SET_FLAG_DEFAULT);
		writeBuffer.putInt(data.length);
		writeBuffer.putInt(0);		// size
		for (SimConnectData sd : data) {
			sd.write(writeBuffer);
		}
		writeBuffer.putInt(32, writeBuffer.position() - 36);
		sendPacket(0x10);
	}

	/**
	 * A little wrapper for data definitions containing only Doubles.
	 * 
	 * @param dataDefinitionID Specifies the ID of the client defined data definition.
	 * @param objectID Specifies the ID of the Flight Simulator object that the data should be about. This ID can be SIMCONNECT_OBJECT_ID_USER (to specify the user's aircraft) or obtained from a SIMCONNECT_RECV_SIMOBJECT_DATA_BYTYPE structure after a call to SimConnect_RequestDataOnSimObjectType.
	 * @param data Array of SimConnectData structures suitable for writing
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType)
	 * @see #clearDataDefinition(int)
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod)
	 * @see #requestDataOnSimObjectType(int, int, int, SimObjectType)
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void setDataOnSimObject(Enum dataDefinitionID, int objectID, 
			double ... data) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(dataDefinitionID.ordinal());
		writeBuffer.putInt(objectID);
		writeBuffer.putInt(SimConnectConstants.DATA_SET_FLAG_DEFAULT);
		writeBuffer.putInt(1);
		writeBuffer.putInt(8 * data.length);
		for (double d : data) {
			writeBuffer.putDouble(d);
		}
		sendPacket(0x10);
	}

	/**
	 * A little wrapper for data definitions containing only Doubles.
	 * 
	 * @param dataDefinitionID Specifies the ID of the client defined data definition.
	 * @param objectID Specifies the ID of the Flight Simulator object that the data should be about. This ID can be SIMCONNECT_OBJECT_ID_USER (to specify the user's aircraft) or obtained from a SIMCONNECT_RECV_SIMOBJECT_DATA_BYTYPE structure after a call to SimConnect_RequestDataOnSimObjectType.
	 * @param data Array of SimConnectData structures suitable for writing
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType)
	 * @see #clearDataDefinition(int)
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod)
	 * @see #requestDataOnSimObjectType(int, int, int, SimObjectType)
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void setDataOnSimObject(int dataDefinitionID, int objectID, 
			double ... data) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(dataDefinitionID);
		writeBuffer.putInt(objectID);
		writeBuffer.putInt(SimConnectConstants.DATA_SET_FLAG_DEFAULT);
		writeBuffer.putInt(1);
		writeBuffer.putInt(8 * data.length);
		for (double d : data) {
			writeBuffer.putDouble(d);
		}
		sendPacket(0x10);
	}


	/**
	 * The <code>setDataOnSimObject</code> function is used to make changes to the data properties of an object.
	 * <p> The data that is set on an object is defined in a data definition 
	 * (see {@link #addToDataDefinition(int, String, String, SimConnectDataType)}). 
	 * This data can include the following structures: {@link flightsim.simconnect.data.Waypoint},
	 * {@link InitPosition} and {@link flightsim.simconnect.data.MarkerState}. Any number 
	 * of waypoints can be given to an AI object using a single call to this function, and
	 * any number of marker state structures can also be combined into an array. </p>
	 * 
	 * <p> The Simulation Variables document includes a column indicating whether variables 
	 * can be written to or not. An exception will be returned if an attempt is made to write 
	 * to a variable that cannot be set in this way.  </p>
	 * 
	 * @param dataDefinitionID Specifies the ID of the client defined data definition.
	 * @param objectID Specifies the ID of the Flight Simulator object that the data should be about. This ID can be SIMCONNECT_OBJECT_ID_USER (to specify the user's aircraft) or obtained from a SIMCONNECT_RECV_SIMOBJECT_DATA_BYTYPE structure after a call to SimConnect_RequestDataOnSimObjectType.
	 * @param data Array of SimConnectData structures suitable for writing
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType)
	 * @see #clearDataDefinition(int)
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod)
	 * @see #requestDataOnSimObjectType(int, int, int, SimObjectType)
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void setDataOnSimObject(Enum dataDefinitionID, int objectID, 
			SimConnectData data[]) throws IOException {
		setDataOnSimObject(dataDefinitionID.ordinal(), objectID, data);
	}

	/**
	 * The <code>setDataOnSimObject</code> function is used to make changes to the data properties of an object.
	 * <p> The data that is set on an object is defined in a data definition 
	 * (see {@link #addToDataDefinition(int, String, String, SimConnectDataType)}). 
	 * This data can include the following structures: {@link flightsim.simconnect.data.Waypoint},
	 * {@link InitPosition} and {@link flightsim.simconnect.data.MarkerState}. Any number 
	 * of waypoints can be given to an AI object using a single call to this function, and
	 * any number of marker state structures can also be combined into an array. </p>
	 * 
	 * <p> The Simulation Variables document includes a column indicating whether variables 
	 * can be written to or not. An exception will be returned if an attempt is made to write 
	 * to a variable that cannot be set in this way.  </p>
	 * 
	 * @param dataDefinitionID Specifies the ID of the client defined data definition.
	 * @param objectID Specifies the ID of the Flight Simulator object that the data should be about. This ID can be SIMCONNECT_OBJECT_ID_USER (to specify the user's aircraft) or obtained from a SIMCONNECT_RECV_SIMOBJECT_DATA_BYTYPE structure after a call to SimConnect_RequestDataOnSimObjectType.
	 * @param tagged The data to be set is being sent in tagged format. Refer to SimConnect_RequestDataOnSimObject for more details on the tagged format. 
	 * @param arrayCount Specifies the number of elements in the data array. A count of zero is interpreted as one element. 
	 * @param data DataWrapper containing the data to write
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType)
	 * @see #clearDataDefinition(int)
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod)
	 * @see #requestDataOnSimObjectType(int, int, int, SimObjectType)
	 * @since 0.2
	 * @throws IOException
	 */
	public synchronized void setDataOnSimObject(int dataDefinitionID, int objectID, 
			boolean tagged, int arrayCount, DataWrapper data) throws IOException {
		setDataOnSimObject(dataDefinitionID, objectID, tagged, arrayCount, data.getBuffer());
	}

	/**
	 * The <code>setDataOnSimObject</code> function is used to make changes to the data properties of an object.
	 * <p> The data that is set on an object is defined in a data definition 
	 * (see {@link #addToDataDefinition(int, String, String, SimConnectDataType)}). 
	 * This data can include the following structures: {@link flightsim.simconnect.data.Waypoint},
	 * {@link InitPosition} and {@link flightsim.simconnect.data.MarkerState}. Any number 
	 * of waypoints can be given to an AI object using a single call to this function, and
	 * any number of marker state structures can also be combined into an array. </p>
	 * 
	 * <p> The Simulation Variables document includes a column indicating whether variables 
	 * can be written to or not. An exception will be returned if an attempt is made to write 
	 * to a variable that cannot be set in this way.  </p>
	 * 
	 * @param dataDefinitionID Specifies the ID of the client defined data definition.
	 * @param objectID Specifies the ID of the Flight Simulator object that the data should be about. This ID can be SIMCONNECT_OBJECT_ID_USER (to specify the user's aircraft) or obtained from a SIMCONNECT_RECV_SIMOBJECT_DATA_BYTYPE structure after a call to SimConnect_RequestDataOnSimObjectType.
	 * @param tagged The data to be set is being sent in tagged format. Refer to SimConnect_RequestDataOnSimObject for more details on the tagged format. 
	 * @param arrayCount Specifies the number of elements in the data array. A count of zero is interpreted as one element. 
	 * @param data DataWrapper containing the data to write
	 * @see #addToDataDefinition(int, String, String, SimConnectDataType)
	 * @see #clearDataDefinition(int)
	 * @see #requestDataOnSimObject(int, int, int, SimConnectPeriod)
	 * @see #requestDataOnSimObjectType(int, int, int, SimObjectType)
	 * @since 0.2
	 * @throws IOException
	 */
	public synchronized void setDataOnSimObject(Enum dataDefinitionID, int objectID, 
			boolean tagged, int arrayCount, DataWrapper data) throws IOException {
		setDataOnSimObject(dataDefinitionID.ordinal(), objectID, tagged, arrayCount, data.getBuffer());
	}

	
	/**
	 * The <code>mapInputEventToClientEvent</code> function is used to connect input events 
	 * (such as keystrokes, joystick or mouse movements) with the sending of appropriate event notifications.
	 * 
	 * <p> The maximum number of events that can be added to an input group is 1000. </p>
	 * <p> For the keyboard the input definition can include a maximum of two modifiers (Shift, Ctrl, Alt) 
	 * and two keys (case senstive). </p>
	 * <p> For joysticks the input definition is in the form <pre>"joystick:n:input[:i]".</pre> Where <i>n</i> 
	 * is the joystick number (starting from 0), <i>input</i> is the input name, and <i>i</i> is an optional 
	 * index number that might be required by the input name (<pre>joystick:0:button:0</pre> for example). </p>
	 * 
	 * @param inputGroupID Specifies the ID of the client defined input group that the input event is to be added to. 
	 * @param inputDefinition string containing the definition of the input events (keyboard keys, mouse or joystick events, for example). 
	 * @param clientEventDownID Specifies the ID of the down, and default, event. This is the client defined event that is triggered when the input event occurs. 
	 * 				If only an up event is required, set this to {@link SimConnectConstants#UNUSED}.
	 * @throws IOException
	 */
	public synchronized void mapInputEventToClientEvent(int inputGroupID, 
			String inputDefinition, 
			int clientEventDownID) throws IOException {
		mapInputEventToClientEvent(inputGroupID, inputDefinition, clientEventDownID, 0, UNUSED, 0, false);
	}

	/**
	 * The <code>mapInputEventToClientEvent</code> function is used to connect input events 
	 * (such as keystrokes, joystick or mouse movements) with the sending of appropriate event notifications.
	 * 
	 * <p> The maximum number of events that can be added to an input group is 1000. </p>
	 * <p> For the keyboard the input definition can include a maximum of two modifiers (Shift, Ctrl, Alt) 
	 * and two keys (case senstive). </p>
	 * <p> For joysticks the input definition is in the form <pre>"joystick:n:input[:i]".</pre> Where <i>n</i> 
	 * is the joystick number (starting from 0), <i>input</i> is the input name, and <i>i</i> is an optional 
	 * index number that might be required by the input name (<pre>joystick:0:button:0</pre> for example). </p>
	 * 
	 * @param inputGroupID Specifies the ID of the client defined input group that the input event is to be added to. 
	 * @param inputDefinition string containing the definition of the input events (keyboard keys, mouse or joystick events, for example). 
	 * @param clientEventDownID Specifies the ID of the down, and default, event. This is the client defined event that is triggered when the input event occurs. 
	 * 				If only an up event is required, set this to {@link SimConnectConstants#UNUSED}.
	 * @throws IOException
	 * @since 0.4
	 */
	public synchronized void mapInputEventToClientEvent(Enum inputGroupID, 
			String inputDefinition, 
			Enum clientEventDownID) throws IOException {
		mapInputEventToClientEvent(inputGroupID.ordinal(), inputDefinition, clientEventDownID.ordinal(), 0, UNUSED, 0, false);
	}

	/**
	 * The <code>mapInputEventToClientEvent</code> function is used to connect input events 
	 * (such as keystrokes, joystick or mouse movements) with the sending of appropriate event notifications.
	 * 
	 * <p> The maximum number of events that can be added to an input group is 1000. </p>
	 * <p> For the keyboard the input definition can include a maximum of two modifiers (Shift, Ctrl, Alt) 
	 * and two keys (case senstive). </p>
	 * <p> For joysticks the input definition is in the form <pre>"joystick:n:input[:i]".</pre> Where <i>n</i> 
	 * is the joystick number (starting from 0), <i>input</i> is the input name, and <i>i</i> is an optional 
	 * index number that might be required by the input name (<pre>joystick:0:button:0</pre> for example). </p>
	 * 
	 * @param inputGroupID Specifies the ID of the client defined input group that the input event is to be added to. 
	 * @param inputDefinition string containing the definition of the input events (keyboard keys, mouse or joystick events, for example). 
	 * @param clientEventDownID Specifies the ID of the down, and default, event. This is the client defined event that is triggered when the input event occurs. 
	 * 				If only an up event is required, set this to {@link SimConnectConstants#UNUSED}.
	 * @param downValue  Specifies an optional numeric value, which will be returned when the down event occurs. 
	 * @param clientEventUpID  Specifies the ID of the up event. This is the client defined event that is triggered when the up action occurs. 
	 * @param UpValue Specifies an optional numeric value, which will be returned when the up event occurs.
	 * @param maskable  If set to true, specifies that the client will mask the event, and no other lower priority clients will receive it.
	 * @see #removeInputEvent(int, String)
	 * @see #clearInputGroup(int)
	 * @see #setInputGroupPriority(int, NotificationPriority)
	 * @throws IOException
	 */
	public synchronized void mapInputEventToClientEvent(int inputGroupID, 
			String inputDefinition, 
			int clientEventDownID, int downValue, 
			int clientEventUpID, int UpValue, 
			boolean maskable) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(inputGroupID);
		putString(writeBuffer, inputDefinition, 256);
		writeBuffer.putInt(clientEventDownID);
		writeBuffer.putInt(downValue);
		writeBuffer.putInt(clientEventUpID);
		writeBuffer.putInt(UpValue);
		writeBuffer.putInt(maskable ? 1 : 0);
		sendPacket(0x11);
	}

	/**
	 * The <code>mapInputEventToClientEvent</code> function is used to connect input events 
	 * (such as keystrokes, joystick or mouse movements) with the sending of appropriate event notifications.
	 * 
	 * <p> The maximum number of events that can be added to an input group is 1000. </p>
	 * <p> For the keyboard the input definition can include a maximum of two modifiers (Shift, Ctrl, Alt) 
	 * and two keys (case senstive). </p>
	 * <p> For joysticks the input definition is in the form <pre>"joystick:n:input[:i]".</pre> Where <i>n</i> 
	 * is the joystick number (starting from 0), <i>input</i> is the input name, and <i>i</i> is an optional 
	 * index number that might be required by the input name (<pre>joystick:0:button:0</pre> for example). </p>
	 * 
	 * @param inputGroupID Specifies the ID of the client defined input group that the input event is to be added to. 
	 * @param inputDefinition string containing the definition of the input events (keyboard keys, mouse or joystick events, for example). 
	 * @param clientEventDownID Specifies the ID of the down, and default, event. This is the client defined event that is triggered when the input event occurs. 
	 * 				If only an up event is required, set this to {@link SimConnectConstants#UNUSED}.
	 * @param downValue  Specifies an optional numeric value, which will be returned when the down event occurs. 
	 * @param clientEventUpID  Specifies the ID of the up event. This is the client defined event that is triggered when the up action occurs. 
	 * @param UpValue Specifies an optional numeric value, which will be returned when the up event occurs.
	 * @param maskable  If set to true, specifies that the client will mask the event, and no other lower priority clients will receive it.
	 * @see #removeInputEvent(int, String)
	 * @see #clearInputGroup(int)
	 * @see #setInputGroupPriority(int, NotificationPriority)
	 * @since 0.4
	 * @throws IOException
	 */
	public synchronized void mapInputEventToClientEvent(Enum inputGroupID, 
			String inputDefinition, 
			Enum clientEventDownID, int downValue, 
			Enum clientEventUpID, int UpValue, 
			boolean maskable) throws IOException {
		mapInputEventToClientEvent(inputGroupID.ordinal(), inputDefinition, 
				clientEventDownID.ordinal(), downValue, clientEventUpID.ordinal(), UpValue, maskable);
	}

	/**
	 * The <code>setInputGroupPriority</code> function is used to set the priority for a specified input group object.
	 * @param inputGroupID Specifies the ID of the client defined input group that the priority setting is to apply to. 
	 * @param priority Specifies the priority setting for the input group
	 * @see NotificationPriority
	 * @see #mapInputEventToClientEvent(int, String, int, int, int, int, boolean)
	 * @see #removeInputEvent(int, String)
	 * @see #clearInputGroup(int)
	 * @see #setInputGroupState(int, boolean)
	 * @throws IOException
	 */
	public synchronized void setInputGroupPriority(int inputGroupID, NotificationPriority priority) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(inputGroupID);
		writeBuffer.putInt(priority.value());
		sendPacket(0x12);
	}

	/**
	 * The <code>setInputGroupPriority</code> function is used to set the priority for a specified input group object.
	 * @param inputGroupID Specifies the ID of the client defined input group that the priority setting is to apply to. 
	 * @param priority Specifies the priority setting for the input group
	 * @see NotificationPriority
	 * @see #mapInputEventToClientEvent(int, String, int, int, int, int, boolean)
	 * @see #removeInputEvent(int, String)
	 * @see #clearInputGroup(int)
	 * @see #setInputGroupState(int, boolean)
	 * @throws IOException
	 * @since 0.4
	 */
	public synchronized void setInputGroupPriority(Enum inputGroupID, NotificationPriority priority) throws IOException {
		setInputGroupPriority(inputGroupID.ordinal(), priority);
	}

	/**
	 * The <code>removeInputEvent</code> method is used to remove an input event from a specified input group object.
	 * 
	 * <p> The input string definitions must match exactly, before anything is removed from the group definition. 
	 * For example, the string defintiions "A+B" and "a+B" do not match. </p>
	 * @param inputGroupID Specifies the ID of the client defined input group from which the event is to be removed. 
	 * @param inputDefinition string containing the input definition.
	 * @throws IOException
	 */
	public synchronized void removeInputEvent(int inputGroupID, String inputDefinition) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(inputGroupID);
		putString(writeBuffer, inputDefinition, 256);
		sendPacket(0x13);
	}

	/**
	 * The <code>removeInputEvent</code> method is used to remove an input event from a specified input group object.
	 * 
	 * <p> The input string definitions must match exactly, before anything is removed from the group definition. 
	 * For example, the string defintiions "A+B" and "a+B" do not match. </p>
	 * @param inputGroupID Specifies the ID of the client defined input group from which the event is to be removed. 
	 * @param inputDefinition string containing the input definition.
	 * @throws IOException
	 * @since 0.4
	 */
	public synchronized void removeInputEvent(Enum inputGroupID, String inputDefinition) throws IOException {
		removeInputEvent(inputGroupID.ordinal(), inputDefinition);
	}
	

	/**
	 * The <code>clearInputGroup</code> method is used to remove all the input events from a specified input group object.
	 * 
	 * <p> Use this function to permanently delete an input group. Use the {@link #setInputGroupState(int, boolean)}
	 * method to temporarily suspend input group notifications . </p>
	 * 
	 * @param inputGroupID Specifies the ID of the client defined input group that is to have all its events removed.
	 * @throws IOException
	 */
	public synchronized void clearInputGroup(int inputGroupID) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(inputGroupID);
		sendPacket(0x14);
	}

	/**
	 * The <code>clearInputGroup</code> method is used to remove all the input events from a specified input group object.
	 * 
	 * <p> Use this function to permanently delete an input group. Use the {@link #setInputGroupState(int, boolean)}
	 * method to temporarily suspend input group notifications . </p>
	 * 
	 * @param inputGroupID Specifies the ID of the client defined input group that is to have all its events removed.
	 * @throws IOException
	 * @since 0.4
	 */
	public synchronized void clearInputGroup(Enum inputGroupID) throws IOException {
		clearInputGroup(inputGroupID.ordinal());
	}

	/**
	 * The <code>setInputGroupState</code> method is used to turn requests for input event 
	 * information from the server on and off.
	 * 
	 * <p> The default state for input groups is to be inactive, so make sure to call this function 
	 * each time an input group is to become active. </p>
	 * @param inputGroupID Specifies the ID of the client defined input group that is to have its state changed.
	 * @param state the new state
	 * @see #mapInputEventToClientEvent(int, String, int, int, int, int, boolean)
	 * @see #setInputGroupPriority(int, NotificationPriority)
	 * @see #removeInputEvent(int, String)
	 * @see #clearInputGroup(int)
	 * @throws IOException
	 */
	public synchronized void setInputGroupState(int inputGroupID, boolean state) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(inputGroupID);
		writeBuffer.putInt(state ? 1 : 0);
		sendPacket(0x15);
	}

	/**
	 * The <code>setInputGroupState</code> method is used to turn requests for input event 
	 * information from the server on and off.
	 * 
	 * <p> The default state for input groups is to be inactive, so make sure to call this function 
	 * each time an input group is to become active. </p>
	 * @param inputGroupID Specifies the ID of the client defined input group that is to have its state changed.
	 * @param state the new state
	 * @see #mapInputEventToClientEvent(int, String, int, int, int, int, boolean)
	 * @see #setInputGroupPriority(int, NotificationPriority)
	 * @see #removeInputEvent(int, String)
	 * @see #clearInputGroup(int)
	 * @throws IOException
	 * @since 0.4
	 */
	public synchronized void setInputGroupState(Enum inputGroupID, boolean state) throws IOException {
		setInputGroupState(inputGroupID.ordinal(), state);
	}
	
	
	/**
	 * The <code>requestReservedKey</code> method is used to request a specific keyboard TAB-key 
	 * combination applies only to this client.
	 * 
	 * <p> A successful call to this function will result in a {@link flightsim.simconnect.recv.RecvReservedKey} 
	 * class being returned, with the key that has been assigned to this client. 
	 * The first of the three that can be assigned will be the choice, unless all three are already taken, 
	 * in which case a null string will be returned. </p>
	 * 
	 * <p> The <code>keyChoice</code> parameters should be a single character (such as "A"), which 
	 * is requesting that the key combination TAB-A is reserved for this client. 
	 * All reserved keys are TAB-key combinations. </p>
	 * 
	 * @param eventID Specifies the client defined event ID. 
	 * @param keyChoice1 string containing the first key choice. Refer to the document Key Strings for a 
	 * 					full list of choices that can be entered for these three
	 * @see #menuAddItem(String, int, int)
	 * @throws IOException
	 */
	public synchronized void requestReservedKey(int eventID, 
			String keyChoice1) throws IOException {
		requestReservedKey(eventID, keyChoice1, "", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * The <code>requestReservedKey</code> method is used to request a specific keyboard TAB-key 
	 * combination applies only to this client.
	 * 
	 * <p> A successful call to this function will result in a {@link flightsim.simconnect.recv.RecvReservedKey} 
	 * class being returned, with the key that has been assigned to this client. 
	 * The first of the three that can be assigned will be the choice, unless all three are already taken, 
	 * in which case a null string will be returned. </p>
	 * 
	 * <p> The <code>keyChoice</code> parameters should be a single character (such as "A"), which 
	 * is requesting that the key combination TAB-A is reserved for this client. 
	 * All reserved keys are TAB-key combinations. </p>
	 * 
	 * @param eventID Specifies the client defined event ID. 
	 * @param keyChoice1 string containing the first key choice. Refer to the document Key Strings for a 
	 * 					full list of choices that can be entered for these three
	 * @param keyChoice2 string containing the second key choice. 
	 * @see #menuAddItem(String, int, int)
	 * @throws IOException
	 */
	public synchronized void requestReservedKey(int eventID, 
			String keyChoice1, String keyChoice2) throws IOException {
		requestReservedKey(eventID, keyChoice1, keyChoice2, ""); //$NON-NLS-1$
	}
	
	/**
	 * The <code>requestReservedKey</code> method is used to request a specific keyboard TAB-key 
	 * combination applies only to this client.
	 * 
	 * <p> A successful call to this function will result in a {@link flightsim.simconnect.recv.RecvReservedKey} 
	 * class being returned, with the key that has been assigned to this client. 
	 * The first of the three that can be assigned will be the choice, unless all three are already taken, 
	 * in which case a null string will be returned. </p>
	 * 
	 * <p> The <code>keyChoice</code> parameters should be a single character (such as "A"), which 
	 * is requesting that the key combination TAB-A is reserved for this client. 
	 * All reserved keys are TAB-key combinations. </p>
	 * 
	 * @param eventID Specifies the client defined event ID. 
	 * @param keyChoice1 string containing the first key choice. Refer to the document Key Strings for a 
	 * 					full list of choices that can be entered for these three
	 * @param keyChoice2 string containing the second key choice. 
	 * @param keyChoice3 string containing the second key choice. 
	 * @see #menuAddItem(String, int, int)
	 * @throws IOException
	 */
	public synchronized void requestReservedKey(int eventID, 
			String keyChoice1, String keyChoice2, String keyChoice3) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(eventID);
		putString(writeBuffer, (keyChoice1 == null) ? "" : keyChoice1, 30); //$NON-NLS-1$
		putString(writeBuffer, (keyChoice2 == null) ? "" : keyChoice2, 30); //$NON-NLS-1$
		putString(writeBuffer, (keyChoice3 == null) ? "" : keyChoice3, 30); //$NON-NLS-1$
		sendPacket(0x16);
	}
	
	/**
	 * The <code>weatherRequestInterpolatedObservation</code> method is used to send a request for 
	 * weather data that is interpolated from the weather at the nearest three weather stations.
	 * 
	 * <p>The weather data will be returned in a {@link flightsim.simconnect.recv.RecvWeatherObservation} 
	 * structure. A number of errors apply specifically to weather data, see the 
	 * {@link flightsim.simconnect.recv.RecvException} enumeration. </p>
	 * 
	 * <p> Interpolated weather data can be used to identify suitable locations for thermals and 
	 * other local weather systems. The process of using the three nearest stations is not without 
	 * its drawbacks, as all three stations could be in one direction of the specified point, 
	 * and not a reasonable spread in different directions. Weather data is returned in Metar data format. </p>
	 * 
	 * @param dataRequestID Specifies the ID of the client defined request. 
	 * 					This is used later by the client to identify which data has been received. 
	 * @param lat  Specifies latitude in degrees.
	 * @param lon Specifies longitude in degrees.
	 * @param alt Specifies altitude in feet above sea level. 
	 * 				This differs from most weather data altitudes, which are feet above ground level.
	 * @see #weatherSetObservation(int, String)
	 * @see #weatherSetModeServer(int, int)
	 * @see #weatherRequestObservationAtStation(int, String)
	 * @see #weatherRequestObservationAtNearestStation(int, float, float)
	 * @throws IOException 
	 */
	public synchronized void weatherRequestInterpolatedObservation(int dataRequestID, 
			float lat, float lon, float alt) throws IOException {
		// packet size 0x20
		// packet id 0x19
		
		clean(writeBuffer);
		writeBuffer.putInt(dataRequestID);
		writeBuffer.putFloat(lat);
		writeBuffer.putFloat(lon);
		writeBuffer.putFloat(alt);
		sendPacket(0x19);
	}
	
	
	/**
	 * The <code>weatherRequestObservationAtStation</code> method requests the weather data from a 
	 * weather station identified by its ICAO code.
	 * 
	 * <p> The weather data will be returned in a {@link flightsim.simconnect.recv.RecvWeatherObservation}
	 * structure. A number of errors apply specifically to weather data, see the {@link flightsim.simconnect.recv.RecvException}
	 * enumeration. Weather data is returned in Metar data format. </p>
	 * 
	 * @param dataRequestID Specifies the ID of the client defined request. 
	 * 				This is used later by the client to identify which data has been received. 
	 * @param ICAO string specifyin the ICAO identification code of the weather station. 
	 * 				Typically this is an airport. Set to GLOB to retrieve global weather.
	 * @see #weatherRequestObservationAtNearestStation(int, float, float)
	 * @see #weatherRequestInterpolatedObservation(int, float, float, float)
	 * @see #weatherSetObservation(int, String)
	 * @see #weatherSetModeServer(int, int)
	 * @throws IOException
	 */
	public synchronized void weatherRequestObservationAtStation(int dataRequestID, String ICAO) throws IOException {

		// packet size 0x19
		// packet id 0x1A
		
		clean(writeBuffer);
		writeBuffer.putInt(dataRequestID);
		putString(writeBuffer, ICAO, 5);		// ICAO is 4 chars, null terminated
		sendPacket(0x1A);
	}
	
	/**
	 * The <code>weatherRequestObservationAtNearestStation</code> function is used to send a 
	 * request for the weather data from the weather station nearest to the specified lat/lon position.
	 * 
	 * <p> The weather data will be returned in a {@link flightsim.simconnect.recv.RecvWeatherObservation}
	 * structure. A number of errors apply specifically to weather data, see the {@link flightsim.simconnect.recv.RecvException}
	 * enumeration. Weather data is returned in Metar data format. </p>
	 * 
	 * @param dataRequestID Specifies the ID of the client defined request. 
	 * This is used later by the client to identify which data has been received
	 * @param lat Specifies latitude in degrees.
	 * @param lon Specifies longitude in degrees.
	 * @see #weatherSetObservation(int, String)
	 * @see #weatherSetModeServer(int, int)
	 * @see #weatherRequestObservationAtStation(int, String)
	 * @see #weatherRequestObservationAtNearestStation(int, float, float)
	 * @throws IOException
	 */
	public synchronized void weatherRequestObservationAtNearestStation(int dataRequestID, float lat, float lon) throws IOException {

		// packet size 0x1C
		// packet id 0x1B
		
		clean(writeBuffer);
		writeBuffer.putInt(dataRequestID);
		writeBuffer.putFloat(lat);
		writeBuffer.putFloat(lon);
		sendPacket(0x1B);
	}

	/**
	 * The <code>weatherRequestObservationAtNearestStation</code> function is used to send a 
	 * request for the weather data from the weather station nearest to the specified lat/lon position.
	 * 
	 * <p> The weather data will be returned in a {@link flightsim.simconnect.recv.RecvWeatherObservation}
	 * structure. A number of errors apply specifically to weather data, see the {@link flightsim.simconnect.recv.RecvException}
	 * enumeration. Weather data is returned in Metar data format. </p>
	 * 
	 * @param dataRequestID Specifies the ID of the client defined request. 
	 * This is used later by the client to identify which data has been received
	 * @param lat Specifies latitude in degrees.
	 * @param lon Specifies longitude in degrees.
	 * @see #weatherSetObservation(int, String)
	 * @see #weatherSetModeServer(int, int)
	 * @see #weatherRequestObservationAtStation(int, String)
	 * @see #weatherRequestObservationAtNearestStation(int, float, float)
	 * @throws IOException
	 * @since 0.4
	 */
	public synchronized void weatherRequestObservationAtNearestStation(Enum dataRequestID, float lat, float lon) throws IOException {
		weatherRequestObservationAtNearestStation(dataRequestID.ordinal(), lat, lon);
	}

	/**
	 * The <code>weatherCreateStation</code> method creates a weather station at the given ICAO location.
	 * 
	 * <p> If an attempt is made to create a weather station at an airport that already has one, 
	 * the error {@link flightsim.simconnect.recv.SimConnectException#WEATHER_UNABLE_TO_CREATE_STATION}
	 * will be returned. </p>
	 * 
	 * <p> Once a weather station has been successfully created, its weather output can be set 
	 * with a call to {@link #weatherSetObservation(int, String)}, and retrieved with a call 
	 * to {@link #weatherRequestObservationAtStation(int, String)}. </p>
	 * 
	 * @param dataRequestID Specifies the client defined request ID. 
	 * @param ICAO Specifies the ICAO string. 
	 * 				This can be an existing airport ICAO string, as long as the airport does 
	 * 				not already have a weather station, or it can be a unique new ICAO code to
	 * 				be used just for the purposes of this weather station.
	 * @param name Specifies a descriptive name for the new weather station. 
	 * 					This name will appear on the weather map in the weather dialogs of Flight Simulator. 
	 * @param lat Specifies the latitude of the station in degrees. The latitude, longitude and 
	 * 				altitude parameters should be set to 0 if the ICAO code supplied is from an existing airport. 
	 * @param lon Specifies the longitude of the station in degrees. 
	 * @param alt Specifies the altitude of the station in feet, above ground level.
	 * @see #weatherRemoveStation(int, String)
	 * @throws IOException
	 */
	public synchronized void weatherCreateStation(int dataRequestID, String ICAO, String name, 
			float lat, float lon, float alt) throws IOException {

		// packet size 0x125
		// packet id 0x1C
		
		clean(writeBuffer);
		writeBuffer.putInt(dataRequestID);
		putString(writeBuffer, ICAO, 5);
		putString(writeBuffer, name, 256);
		writeBuffer.putFloat(lat);
		writeBuffer.putFloat(lon);
		writeBuffer.putFloat(alt);
		sendPacket(0x1C);
	}
	
	/**
	 * The <code>weatherRemoveStation</code> method requests that the weather station 
	 * identified by the given ICAO string is removed.
	 * 
	 * <p> If a call is made to remove a weather station created by another client, or 
	 * an exiting one within Flight Simulator, the error {@link flightsim.simconnect.recv.SimConnectException#WEATHER_UNABLE_TO_REMOVE_STATION}
	 * will be returned. </p>
	 * 
	 * <p> If the client is closed, then all weather stations created by the client will be removed. </p>
	 *  
	 * @param dataRequestID Specifies the client defined request ID. 
	 * @param ICAO  Specifies the ICAO string of the station to remove. 
	 * 			The station must be one that was created by the same SimConnect client.
	 * @see #weatherCreateStation(int, String, String, float, float, float)
	 * @throws IOException 
	 */
	public synchronized void weatherRemoveStation(int dataRequestID, String ICAO) throws IOException {

		// packet size 0x19
		// packet id 0x1D
		
		clean(writeBuffer);
		writeBuffer.putInt(dataRequestID);
		putString(writeBuffer, ICAO, 5);
		sendPacket(0x1D);
	}
	
	/**
	 * The <code>weatherSetObservation</code> method is used to set the weather at a specific weather station, 
	 * identified from within the Metar data string.
	 * 
	 * <p> A number of errors apply specifically to weather data, see the {@link flightsim.simconnect.recv.RecvException}
	 * enumeration. </p>
	 * 
	 * @param seconds Specifies the time in seconds that the current weather should merge into the new weather
	 * @param metar string containing the METAR data.
	 * @see #weatherRequestInterpolatedObservation(int, float, float, float)
	 * @see #weatherRequestObservationAtNearestStation(int, float, float)
	 * @see #weatherRequestObservationAtStation(int, String)
	 * @see #weatherSetModeServer(int, int)
	 * @throws IOException 
	 */
	public synchronized void weatherSetObservation(int seconds, String metar) throws IOException {

		// packet size variable (metar)
		// packet id 0x1E
		
		clean(writeBuffer);
		writeBuffer.putInt(seconds);
		byte[] metarData = metar.getBytes();
		writeBuffer.put(metarData);
		writeBuffer.put((byte) 0);		// null terminated
		sendPacket(0x1E);
	}
	
	/**
	 * The <code>weatherSetModeServer</code> method is used to switch to a local server for 
	 * weather observation data.
	 * 
	 * <p> Calling this function sets the weather mode to "Real-world weather " in the Weather 
	 * dialog of Flight Simulator X. The Weather dialog only allows two update rates 
	 * (none and 15 minutes), whereas this call gives much greater flexibility over the update 
	 * rate. Setting the port number allows a local weather server to be used, rather than the default.<p>
	 * 
	 * <p> A number of errors apply specifically to weather data, see the {@link flightsim.simconnect.recv.RecvException}
	 * enumeration. </p>
	 * 
	 * @param port the port number of the weather server. 
	 * 					Set this to zero to reset the weather to normal operation. 
	 * @param seconds the amount of time, in seconds, that should elapse between each update. 
	 * 					There is a minimum of 60 seconds.
	 * @see #weatherSetModeCustom()
	 * @see #weatherSetModeGlobal()
	 * @see #weatherSetModeCustom()
	 * @throws IOException 
	 */
	public synchronized void weatherSetModeServer(int port, int seconds) throws IOException {
		// packet size 0x18
		// packet id 0x1F
		
		clean(writeBuffer);
		writeBuffer.putInt(port);
		writeBuffer.putInt(seconds);
		sendPacket(0x1F);
	}
	
	/**
	 * The <code>weatherSetModeTheme</code> method is used to set the weather to a particular theme.
	 * 
	 * <p> Calling this function sets the weather mode to "Weather themes " in the weather dialog of 
	 * Flight Simulator X. </p>
	 * 
	 * <p> There are three files associated with a weather theme in Flight Simulator X, for 
	 * example: grayrain.wt, grayrain.bmp and grayrain.wtb. The wt file contains the 
	 * description that will appear in the Current Conditions box in the Weather dialog, 
	 * the bmp file contains the image that will also appear in the weather dialog, 
	 * and the wtb file contains data in a propriety format that contains the weather information. </p>
	 * 
	 * @param themeName string containing the theme filename. 
	 * 				The corresponding files should exist in the Microsoft Flight Simulator X/weather/themes folder. 
	 * 				For example, enter "grayrain" to set the same theme as if the user had selected Gray and Rainy from the Weather dialog.
	 * @throws IOException 
	 * @see #weatherSetModeCustom()
	 * @see #weatherSetModeGlobal()
	 * @see #weatherSetModeServer(int, int)
	 */
	public synchronized void weatherSetModeTheme(String themeName) throws IOException {
		// packet size 0x110
		// packet id 0x20
		
		clean(writeBuffer);
		putString(writeBuffer, themeName, 256);
		sendPacket(0x20);
	}
	
	/**
	 * The <code>weatherSetModeGlobal</code> method sets the weather mode to global, 
	 * so the same weather data is used everywhere.
	 * 
	 * <p> There is not an equivalent setting in the weather dialog of Flight Simulator X.</p>
	 * @throws IOException 
	 * @see #weatherSetModeCustom()
	 * @see #weatherSetModeServer(int, int)
	 * @see #weatherSetModeTheme(String)
	 *
	 */
	public synchronized void weatherSetModeGlobal() throws IOException {
		// packet size 0x10
		// packet id 0x21
		
		clean(writeBuffer);
		sendPacket(0x21);
	}
	
	/**
	 * The <code>weatherSetModeCustom</code> method sets the weather mode to user-defined.
	 * 
	 * <p> Calling this function sets the weather mode to "User-defined weather" in the 
	 * Weather dialog of Flight Simulator X, so whatever the user has entered for the weather will be used. </p>
	 * 
	 * @throws IOException 
	 *
	 */
	public synchronized void weatherSetModeCustom() throws IOException {
		// packet size 0x10
		// packet id 0x22
		
		clean(writeBuffer);
		sendPacket(0x22);
	}
	
	/**
	 * The <code>weatherSetDynamicUpdateRate</code> function is used to set the rate at 
	 * which cloud formations change.
	 * 
	 * 
	 * @param rate Integer containing the rate. 
	 * 					A value of zero indicates that cloud formations do not change at all. 
	 * 					Values between 1 and 5 indicate that cloud formations should change from 1 
	 * 					(the slowest) to 5 (the fastest). These settings match those than can be set
	 * 					through the dialogs of Flight Simulator X.
	 * @see #weatherSetModeCustom()
	 * @see #weatherSetModeServer(int, int)
	 * @see #weatherSetModeTheme(String)
	 * @see #weatherSetModeGlobal()
	 * @throws IOException 
	 */
	public synchronized void weatherSetDynamicUpdateRate(int rate) throws IOException {
		// packet size 0x14
		// packet id 0x23
		
		clean(writeBuffer);
		writeBuffer.putInt(rate);
		sendPacket(0x23	);
	}
	
	/**
	 * The <code>weatherRequestCloudState</code> method requests cloud density information on a given area.
	 * 
	 * <p> The main purpose of this function is to enable weather radar. If the call is successful, 
	 * the cloud state information will be returned in a {@link flightsim.simconnect.recv.RecvCloudState}
	 * structure. This structure will contain a two dimensional array of byte data. 
	 * The array will be 64 x 64 bytes in size, and each byte will contain a value 
	 * indicating the cloud density for each cell. A value of zero would mean no clouds, 
	 * to a maximum of 255. The area defined in this call is divided into 64 by 64 cells, 
	 * so the size of each cell will be determined by the values given for the parameters above. 
	 * Note that the entire World's weather is not simulated all the time, but only a region 
	 * around the user aircraf, with a radius of approximately 128 kilometers, is modeled at
	 * any one time. A request for cloud data outside this region will simply return zeros. </p>
	 *  
	 * <p>The defined area can cross the Equator or the Greenwich Meridian, but it cannot cross the
	 *  Poles or the International Date Line. </p>
	 *  
	 * @param dataRequestID Specifies the client-defined request ID. 	
	 * @param minLat Specifies the minimum latitude of the required area. This should simply be the lower of the two latitude numbers.
	 * @param minLon Specifies the minimum longitude of the required area. This should simply be the lower of the two longitude numbers. 
	 * @param minAlt Specifies the minimum altitude of the required area, in feet. 
	 * @param maxLat Specifies the maximum latitude of the required area. 
	 * @param maxLon Specifies the maximum longitude of the required area. 
	 * @param maxAlt Specifies the maximum altitude of the required area, in feet. 
	 * @see #weatherSetModeCustom()
	 * @see #weatherSetModeServer(int, int)
	 * @see #weatherSetModeTheme(String)
	 * @see #weatherSetModeGlobal()
	 * @see #weatherSetDynamicUpdateRate(int)
	 * @param flags Reserved for future use.
	 * @throws IOException 
	 */
	public synchronized void weatherRequestCloudState(int dataRequestID, 
			float minLat, float minLon, float minAlt, 
			float maxLat, float maxLon, float maxAlt, 
			int flags) throws IOException {
		// packet size 0x30
		// packet id 0x24
		
		clean(writeBuffer);
		writeBuffer.putInt(dataRequestID);
		writeBuffer.putFloat(minLat);
		writeBuffer.putFloat(minLon);
		writeBuffer.putFloat(minAlt);
		writeBuffer.putFloat(maxLat);
		writeBuffer.putFloat(maxLon);
		writeBuffer.putFloat(maxAlt);
		writeBuffer.putInt(flags);
		sendPacket(0x24);
	}
	
	/**
	 * The <code>weatherRequestCloudState</code> method requests cloud density information on a given area.
	 * 
	 * <p> The main purpose of this function is to enable weather radar. If the call is successful, 
	 * the cloud state information will be returned in a {@link flightsim.simconnect.recv.RecvCloudState}
	 * structure. This structure will contain a two dimensional array of byte data. 
	 * The array will be 64 x 64 bytes in size, and each byte will contain a value 
	 * indicating the cloud density for each cell. A value of zero would mean no clouds, 
	 * to a maximum of 255. The area defined in this call is divided into 64 by 64 cells, 
	 * so the size of each cell will be determined by the values given for the parameters above. 
	 * Note that the entire World's weather is not simulated all the time, but only a region 
	 * around the user aircraf, with a radius of approximately 128 kilometers, is modeled at
	 * any one time. A request for cloud data outside this region will simply return zeros. </p>
	 *  
	 * <p>The defined area can cross the Equator or the Greenwich Meridian, but it cannot cross the
	 *  Poles or the International Date Line. </p>
	 *  
	 * @param dataRequestID Specifies the client-defined request ID. 	
	 * @param minLat Specifies the minimum latitude of the required area. This should simply be the lower of the two latitude numbers.
	 * @param minLon Specifies the minimum longitude of the required area. This should simply be the lower of the two longitude numbers. 
	 * @param minAlt Specifies the minimum altitude of the required area, in feet. 
	 * @param maxLat Specifies the maximum latitude of the required area. 
	 * @param maxLon Specifies the maximum longitude of the required area. 
	 * @param maxAlt Specifies the maximum altitude of the required area, in feet. 
	 * @see #weatherSetModeCustom()
	 * @see #weatherSetModeServer(int, int)
	 * @see #weatherSetModeTheme(String)
	 * @see #weatherSetModeGlobal()
	 * @see #weatherSetDynamicUpdateRate(int)
	 * @throws IOException 
	 */
	public synchronized void weatherRequestCloudState(int dataRequestID, 
			float minLat, float minLon, float minAlt, 
			float maxLat, float maxLon, float maxAlt) throws IOException {
		weatherRequestCloudState(dataRequestID, minLat, minLon, minAlt, maxLat, maxLon, maxAlt, 0);
	}


	/**
	 * The <code>weatherCreateThermal</code> method is used to create a thermal at a specific location. 
	 * The method is filled with default parameters. (3.0, 0.05, 3.0, 0.2, 0.4, 0.1, 0.4, 0.1)
	 * 
	 * <p> There is no limit to the number of thermals that can be created.
	 *  Within the simulator a thermal is defined as a cylinder with a Core layer and a Sink layer </p>
	 * 
	 * 
	 * @param dataRequestID Specifies the client defined request ID. 
	 * @param lat Specifies the latitude of the thermal in degrees. 
	 * @param lon Specifies the longitude of the thermal in degrees. 
	 * @param alt Specifies the altitude of the thermal in feet, above ground level. 
	 * @param radius Specifies the radius of the thermal, in meters. The maximum radius of a thermal is 100Km.
	 * @param height Specifies the height of the thermal, in meters. 
	 * @throws IOException 
	 * @see #weatherCreateThermal(int, float, float, float, float, float, float, float, float, float, float, float, float, float)
	 * @see #weatherRemoveThermal(int)
	 */
	public synchronized void weatherCreateThermal(int dataRequestID, 
			float lat, float lon, float alt, 
			float radius, float height) throws IOException {
		weatherCreateThermal(dataRequestID, lat, lon, alt, radius, height, 
				3.0f, 0.05f, 3.0f, 0.2f, 0.4f, 0.1f, 0.4f, 0.1f);
	}

	/**
	 * The <code>weatherCreateThermal</code> method is used to create a thermal at a specific location.
	 * 
	 * <p> There is no limit to the number of thermals that can be created.
	 *  Within the simulator a thermal is defined as a cylinder with a Core layer and a Sink layer </p>
	 * 
	 * 
	 * @param dataRequestID Specifies the client defined request ID. 
	 * @param lat Specifies the latitude of the thermal in degrees. 
	 * @param lon Specifies the longitude of the thermal in degrees. 
	 * @param alt Specifies the altitude of the thermal in feet, above ground level. 
	 * @param radius Specifies the radius of the thermal, in meters. The maximum radius of a thermal is 100Km.
	 * @param height Specifies the height of the thermal, in meters. 
	 * @param coreRate Specifies the lift value, in meters per second, within the Core layer. 
	 * 			A positive value will provide an updraft, a negative value a downdraft. 
	 * 			The maximum rate is 1000 meters/second. 
	 * @param coreTurbulence Specifies a variation in meters per second that is applied to the coreRate. 
	 * 			For example, if a value of 1.5 is entered, and the core rate is 5 m/s, 
	 * 			the actual core rate applied will be randomly varying between 3.5 m/s and 6.5 m/s. 
	 * @param sinkRate Specifies the lift value, in meters per second, within the Sink layer. 
	 * 			A positive value will provide an updraft, a negative value a downdraft. 
	 * 			The maximum rate is 1000 meters/second.
	 * @param sinkTurbulence Specifies a variation in meters per second that is applied to the sinkRate. 
	 * 			For example, if a value of 1.5 is entered, and the sink rate is 5 m/s, 
	 * 			the actual sink rate applied will be randomly varying between 3.5 m/s and 6.5 m/s. 
	 * @param coreSize Specifies the radius in meters of the Core of the thermal. 
	 * @param coreTransitionSize Specifies the width in meters of the transition layer between the Core and the Sink of the thermal. 
	 * 			Half of the width of this transition will be outside the Core, and half within.
	 * @param sinkLayerSize Specifies the radius in meters of the Sink of the thermal. 
	 * @param sinkTransitionSize Specifies the width in meters of the transition layer between the Sink and the atmosphere outside of the thermal. 
	 * 			Half of the width of this transition will be outside the radius of the Sink layer, and half within.
	 * @throws IOException 
	 * @see #weatherRemoveThermal(int)
	 */
	public synchronized void weatherCreateThermal(int dataRequestID, 
			float lat, float lon, float alt, 
			float radius, float height, 
			float coreRate, float coreTurbulence, 
			float sinkRate, float sinkTurbulence, 
			float coreSize, float coreTransitionSize, 
			float sinkLayerSize, float sinkTransitionSize) throws IOException {
		// packet size 0x48
		// packet id 0x25
		
		clean(writeBuffer);
		writeBuffer.putInt(dataRequestID);
		writeBuffer.putFloat(lat);
		writeBuffer.putFloat(lon);
		writeBuffer.putFloat(alt);
		writeBuffer.putFloat(radius);
		writeBuffer.putFloat(height);
		writeBuffer.putFloat(coreRate);
		writeBuffer.putFloat(coreTurbulence);
		writeBuffer.putFloat(sinkRate);
		writeBuffer.putFloat(sinkTurbulence);
		writeBuffer.putFloat(coreSize);
		writeBuffer.putFloat(coreTransitionSize);
		writeBuffer.putFloat(sinkLayerSize);
		writeBuffer.putFloat(sinkTransitionSize);
		sendPacket(0x25);
	}
	
	/**
	 * The <code>weatherRemoveThermal</code> method removes a thermal.
	 * 
	 * <p> A client application can only remove thermals that it created, and 
	 * not thermals created by other clients or by Flight Simulator. 
	 * If the client is closed, then all thermals created by the client will be removed. </p>
	 * 
	 * @param objectID Specifies the object ID of the thermal to be removed.
	 * @see #weatherCreateThermal(int, float, float, float, float, float, float, float, float, float, float, float, float, float)
	 * @see #weatherCreateThermal(int, float, float, float, float, float)
	 * @throws IOException 
	 */
	public synchronized void weatherRemoveThermal(int objectID) throws IOException {
		// packet size 0x14
		// packet id 0x26
		
		clean(writeBuffer);
		writeBuffer.putInt(objectID);
		sendPacket(0x26);
	}
	
	/**
	 * The <code>aICreateParkedATCAircraft</code> function is used to create an AI controlled 
	 * aircraft that is currently parked and does not have a flight plan.
	 * 
	 * <p> Calling this function is no guarrantee that there is sufficient parking space at the 
	 * specified airport. An error will be returned if there is insufficient parking space, and 
	 * an aircraft will not be created. A number of errors, including {@link flightsim.simconnect.recv.SimConnectException#CREATE_OBJECT_FAILED}, 
	 * apply to AI objects (refer to the {@link flightsim.simconnect.recv.SimConnectException} 
	 * enum for more details). After creating an aircraft with this function, a call to 
	 * {@link #aISetAircraftFlightPlan(int, String, int)} will set the aircraft in motion. 
	 * Refer to the remarks for {@link #aICreateEnrouteATCAircraft(String, String, int, String, double, boolean, int)}.
	 * 
	 * @param containerTitle tring containing the container title. 
	 * 		The container title is case-sensitive and can found in the aircraft.cfg file
	 * 		for example: <b>title=Airbus A321</b>, <b>title= Aircreation582SL</b>, or <b>title=Boeing 737-800</b>. 
	 * @param tailNumber string containing the tail number. This should have a maximum of 12 characters. 
	 * @param airportID string containing the airport ID. This is the ICAO identification string, for example, KSEA for SeaTac International. 
	 * @param dataRequestID Specifies the client defined request ID.
	 * @throws IOException 
	 * @see #aICreateEnrouteATCAircraft(String, String, int, String, double, boolean, int)
	 * @see #aICreateNonATCAircraft(String, String, InitPosition, int)
	 * @see #aICreateSimulatedObject(String, InitPosition, int)
	 * @see #aIRemoveObject(int, int)
	 * @see #aISetAircraftFlightPlan(int, String, int)
	 */
	public synchronized void aICreateParkedATCAircraft(String containerTitle, 
			String tailNumber, String airportID, int dataRequestID) throws IOException {
		// packet size 0x125
		// packet id 0x27
		
		clean(writeBuffer);
		putString(writeBuffer, containerTitle, 256);
		putString(writeBuffer, tailNumber, 12);
		putString(writeBuffer, airportID, 5);
		writeBuffer.putInt(dataRequestID);
		sendPacket(0x27);
	}
	
	/**
	 * The <code>aICreateEnrouteATCAircraft</code> function is used to create an AI controlled aircraft that is about to start or is already underway on its flight plan.
	 * 
	 * <p> An enroute aircraft can be on the ground or airborne when it is created by this function. 
	 * Typically this will be an aircraft flying under IFR rules, and in constant radio contact with ATC. 
	 * A number of errors, including {@link flightsim.simconnect.recv.SimConnectException#CREATE_OBJECT_FAILED}, 
	 * apply to AI objects (refer to the {@link flightsim.simconnect.recv.SimConnectException} enum for more details).
	 * </p>
	 * 
	 *  <p> A {@link flightsim.simconnect.recv.RecvEventAddRemove} event notification can be subscribed to 
	 *  (see the {@link #subscribeToSystemEvent(int, String)} function), which will return a 
	 *  {@link flightsim.simconnect.recv.RecvEventAddRemove} structure whenever any client, including 
	 *  the one making the change, successfully adds or removes an AI controlled object. </p>
	 *  
	 *  <p> This function should be used for fixed-wing aircraft flying between airports on land. 
	 *  There is no internal AI pilot for helicopters, gliders or hot-air balloons. In order to 
	 *  add a helicopte, glider or balloon not controlled by the user, the SimConnect client must 
	 *  implement full control of the aircraft. Set up these objects with a call to 
	 *  {@link #aICreateSimulatedObject(String, InitPosition, int)}. </p>
	 *  
	 *  <p> For float-planes the recommended procedure is to control them using waypoints, and not the 
	 *  ATC system, as there is no concept of a "parking space" after a water landing. So, the 
	 *  waypoints of the route of the float-plane should include the route that it should follow 
	 *  before take off and after landing. For all these cases of controlling aircraft using the client, 
	 *  or using waypoints, set up the object using the 
	 *  {@link #aICreateNonATCAircraft(String, String, InitPosition, int)} call. </p>

	 * @param containerTitle tring containing the container title. 
	 * 		The container title is case-sensitive and can found in the aircraft.cfg file
	 * 		for example: <b>title=Airbus A321</b>, <b>title= Aircreation582SL</b>, or <b>title=Boeing 737-800</b>. 
	 * @param tailNumber string containing the tail number. This should have a maximum of 12 characters. 
	 * @param flightNumber  Integer containing the flight number. There is no specific maximum length of this number. Any negative number indicates that there is no flight number.
	 * @param flightPlanPath string containing the path to the flight plan file. 
	 * 			Flight plans have the extension .pln, but no need to enter an extension here. 
	 * 			The easiest way to create flight plans is to create them from within Flight Simulator 
	 * 			itself, and then save them off for use with the AI controlled aircraft. There is no need 
	 * 			to enter the full path to the file (just enter the filename) if the flight plan is in the default 
	 * 			Flight Simulator X Files directory. 
	 * @param flightPlanPosition Double floating point number containing the flight plan position. 
	 * 			The number before the point contains the waypoint index, and the number afterwards how far 
	 * 			along the route to the next waypoint the aircraft is to be positioned. 
	 * 			The first waypoint index is 0. For example, 0.0 indicates that the aircraft has not started on the 
	 * 			flight plan, 2.5 would indicate the aircraft is to be initialized halfway between the third and 
	 * 			fourth waypoints (which would have indexes 2 and 3). The waypoints are those recorded in the flight 
	 * 			plan, which may just be two airports, and do not include any taxiway points on the ground. Also 
	 * 			there is a threshold that will ignore requests to have an aircraft taxiing or taking off, or landing. 
	 * 			So set the value after the point to ensure the aircraft will be in level flight
	 * @param touchAndGo True indicating that landings should be touch and go, and not full stop landings. 
	 * @param dataRequestID Specifies the client defined request ID.
	 * @throws IOException 
	 */
	public synchronized void aICreateEnrouteATCAircraft(String containerTitle, String tailNumber, 
			int flightNumber, String flightPlanPath, double flightPlanPosition, 
			boolean touchAndGo, int dataRequestID) throws IOException {
		// packet size 0x234
		// packet id 0x28
		
		clean(writeBuffer);
		putString(writeBuffer, containerTitle, 256);
		putString(writeBuffer, tailNumber, 12);
		writeBuffer.putInt(flightNumber);
		putString(writeBuffer, flightPlanPath, 260);
		writeBuffer.putDouble(flightPlanPosition);
		writeBuffer.putInt(touchAndGo ? 1 : 0);
		writeBuffer.putInt(dataRequestID);
		sendPacket(0x28);
	}
	
	/**
	 * The <code>aICreateNonATCAircraft</code> function is used to create an aircraft that is not flying under 
	 * ATC control (so is typically flying under VFR rules).
	 * 
	 * <p> A non-ATC aircraft can be on the ground or airborne when it is created by this function. 
	 * A number of errors, including {@link flightsim.simconnect.recv.SimConnectException#CREATE_OBJECT_FAILED}, 
	 * apply to AI objects (refer to the {@link flightsim.simconnect.recv.SimConnectException} 
	 * enum for more details). </p>
	 * 
	 * <p> Refer to the remarks for {@link #aICreateEnrouteATCAircraft(String, String, int, String, double, boolean, int)}. </p>
	 * 
	 * @param containerTitle tring containing the container title. 
	 * 		The container title is case-sensitive and can found in the aircraft.cfg file
	 * 		for example: <b>title=Airbus A321</b>, <b>title= Aircreation582SL</b>, or <b>title=Boeing 737-800</b>. 
	 * @param tailNumber string containing the tail number. This should have a maximum of 12 characters. 
	 * @param initPos Specifies the initial position, using a {@link InitPosition} structure.
	 * @param dataRequestID Specifies the client defined request ID.
	 * @see #aICreateEnrouteATCAircraft(String, String, int, String, double, boolean, int)
	 * @see #aICreateParkedATCAircraft(String, String, String, int)
	 * @see #aICreateSimulatedObject(String, InitPosition, int)
	 * @see #aIRemoveObject(int, int)
	 * @throws IOException 
	 */
	public synchronized void aICreateNonATCAircraft(String containerTitle, String tailNumber, 
			InitPosition initPos, int dataRequestID) throws IOException {
		// packet size 0x158 (344)
		// packet id 0x29
		
		clean(writeBuffer);
		putString(writeBuffer, containerTitle, 256);
		putString(writeBuffer, tailNumber, 12);
		initPos.write(writeBuffer);
		writeBuffer.putInt(dataRequestID);
		sendPacket(0x29);
	}

	/**
	 * The <code>aICreateNonATCAircraft</code> function is used to create an aircraft that is not flying under 
	 * ATC control (so is typically flying under VFR rules).
	 * 
	 * <p> A non-ATC aircraft can be on the ground or airborne when it is created by this function. 
	 * A number of errors, including {@link flightsim.simconnect.recv.SimConnectException#CREATE_OBJECT_FAILED}, 
	 * apply to AI objects (refer to the {@link flightsim.simconnect.recv.SimConnectException} 
	 * enum for more details). </p>
	 * 
	 * <p> Refer to the remarks for {@link #aICreateEnrouteATCAircraft(String, String, int, String, double, boolean, int)}. </p>
	 * 
	 * @param containerTitle tring containing the container title. 
	 * 		The container title is case-sensitive and can found in the aircraft.cfg file
	 * 		for example: <b>title=Airbus A321</b>, <b>title= Aircreation582SL</b>, or <b>title=Boeing 737-800</b>. 
	 * @param tailNumber string containing the tail number. This should have a maximum of 12 characters. 
	 * @param initPos Specifies the initial position, using a {@link InitPosition} structure.
	 * @param dataRequestID Specifies the client defined request ID.
	 * @see #aICreateEnrouteATCAircraft(String, String, int, String, double, boolean, int)
	 * @see #aICreateParkedATCAircraft(String, String, String, int)
	 * @see #aICreateSimulatedObject(String, InitPosition, int)
	 * @see #aIRemoveObject(int, int)
	 * @throws IOException 
	 * @since 0.4
	 */
	public synchronized void aICreateNonATCAircraft(String containerTitle, String tailNumber, 
			InitPosition initPos, Enum dataRequestID) throws IOException {
		aICreateNonATCAircraft(containerTitle, tailNumber, initPos, dataRequestID.ordinal());
	}

	/**
	 * The <code>aICreateSimulatedObject</code> function is used to create AI controlled objects other than aircraft.
	 * 
	 * <p> This function can be used to create a stationary aircraft (such as an unflyable aircraft on 
	 * display outside a flight museaum), but is typically intended to create simulation objects 
	 * other than aircraft (such as ground vehicles, boats, and a number of special objects such as
	 *  humpback whales and hot-air balloons). A number of errors apply to AI objects 
	 *  (refer to the {@link flightsim.simconnect.recv.SimConnectException} enum for more details).</p>
	 *  
	 * @param containerTitle string containing the container title. The container title is case-sensitive and can be found in the sim.cfg file
	 * @param initPos Specifies the initial position, using a {@link InitPosition} structure.
	 * @param dataRequestID Specifies the client defined request ID.
	 * @throws IOException 
	 * @see #aICreateEnrouteATCAircraft(String, String, int, String, double, boolean, int)
	 * @see #aICreateParkedATCAircraft(String, String, String, int)
	 * @see #aICreateNonATCAircraft(String, String, InitPosition, int)
	 * @see #aIRemoveObject(int, int)
	 */
	public synchronized void aICreateSimulatedObject(String containerTitle, 
			InitPosition initPos, int dataRequestID) throws IOException {
		// packet size 0x14C
		// packet id 0x2A
		
		clean(writeBuffer);
		putString(writeBuffer, containerTitle, 256);
		initPos.write(writeBuffer);
		writeBuffer.putInt(dataRequestID);
		sendPacket(0x2A);
	}
	
	/**
	 * The <code>aICreateSimulatedObject</code> function is used to create AI controlled objects other than aircraft.
	 * 
	 * <p> This function can be used to create a stationary aircraft (such as an unflyable aircraft on 
	 * display outside a flight museaum), but is typically intended to create simulation objects 
	 * other than aircraft (such as ground vehicles, boats, and a number of special objects such as
	 *  humpback whales and hot-air balloons). A number of errors apply to AI objects 
	 *  (refer to the {@link flightsim.simconnect.recv.SimConnectException} enum for more details).</p>
	 *  
	 * @param containerTitle string containing the container title. The container title is case-sensitive and can be found in the sim.cfg file
	 * @param initPos Specifies the initial position, using a {@link InitPosition} structure.
	 * @param dataRequestID Specifies the client defined request ID.
	 * @throws IOException 
	 * @see #aICreateEnrouteATCAircraft(String, String, int, String, double, boolean, int)
	 * @see #aICreateParkedATCAircraft(String, String, String, int)
	 * @see #aICreateNonATCAircraft(String, String, InitPosition, int)
	 * @see #aIRemoveObject(int, int)
	 * @since 0.4
	 */
	public synchronized void aICreateSimulatedObject(String containerTitle, 
			InitPosition initPos, Enum dataRequestID) throws IOException {
		aICreateSimulatedObject(containerTitle, initPos, dataRequestID.ordinal());
	}


	/**
	 * The <code>aIReleaseControl</code> function is used to clear the AI control of a simulated object, 
	 * typically an aircraft, in order for it to be controlled by a SimConnect client.
	 * 
	 * <p> This function should be used to transfer the control of an aircraft, or other object, 
	 * from the AI system to the SimConnect client. If this is not done the AI system and client may 
	 * fight each other with unpredictable results. </p>
	 * 
	 * <p> The object ID can be obtained in a number of ways, refer to the {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod)}
	 *  call, and also the use of the {@link flightsim.simconnect.recv.RecvAssignedObjectID} structure. </p> 

	 * @param objectID Specifies the server defined object ID. 
	 * @param dataRequestID Specifies the client defined request ID.
	 * @see #aICreateEnrouteATCAircraft(String, String, int, String, double, boolean, int)
	 * @see #aICreateParkedATCAircraft(String, String, String, int)
	 * @see #aICreateNonATCAircraft(String, String, InitPosition, int)
	 * @see #aICreateSimulatedObject(String, InitPosition, int)
	 * @see #aISetAircraftFlightPlan(int, String, int)
	 * @throws IOException 
	 */
	public synchronized void aIReleaseControl(int objectID, int dataRequestID) throws IOException {
		// packet size 0x18
		// packet id 0x2B
		
		clean(writeBuffer);
		writeBuffer.putInt(objectID);
		writeBuffer.putInt(dataRequestID);
		sendPacket(0x2B);
	}

	/**
	 * The <code>aIReleaseControl</code> function is used to clear the AI control of a simulated object, 
	 * typically an aircraft, in order for it to be controlled by a SimConnect client.
	 * 
	 * <p> This function should be used to transfer the control of an aircraft, or other object, 
	 * from the AI system to the SimConnect client. If this is not done the AI system and client may 
	 * fight each other with unpredictable results. </p>
	 * 
	 * <p> The object ID can be obtained in a number of ways, refer to the {@link #requestDataOnSimObject(int, int, int, SimConnectPeriod)}
	 *  call, and also the use of the {@link flightsim.simconnect.recv.RecvAssignedObjectID} structure. </p> 

	 * @param objectID Specifies the server defined object ID. 
	 * @param dataRequestID Specifies the client defined request ID.
	 * @see #aICreateEnrouteATCAircraft(String, String, int, String, double, boolean, int)
	 * @see #aICreateParkedATCAircraft(String, String, String, int)
	 * @see #aICreateNonATCAircraft(String, String, InitPosition, int)
	 * @see #aICreateSimulatedObject(String, InitPosition, int)
	 * @see #aISetAircraftFlightPlan(int, String, int)
	 * @throws IOException 
	 * @since 0.4
	 */
	public synchronized void aIReleaseControl(int objectID, Enum dataRequestID) throws IOException {
		aIReleaseControl(objectID, dataRequestID.ordinal());
	}

	/**
	 * The <code>aIRemoveObject</code> function is used to remove any object created by the client 
	 * using one of the AI creation functions.
	 * 
	 * <p> A client application can only remove AI controlled objects that it created, not objects created 
	 * by other clients, or Flight Simulator itself. </p>
	 * 
	 * @param objectID Specifies the server defined object ID (refer to the {@link flightsim.simconnect.recv.RecvAssignedObjectID} structure). 
	 * @param dataRequestID  Specifies the client defined request ID.
	 * @see #aICreateEnrouteATCAircraft(String, String, int, String, double, boolean, int)
	 * @see #aICreateParkedATCAircraft(String, String, String, int)
	 * @see #aICreateNonATCAircraft(String, String, InitPosition, int)
	 * @see #aICreateSimulatedObject(String, InitPosition, int)
	 * @throws IOException 
	 */
	public synchronized void aIRemoveObject(int objectID, int dataRequestID) throws IOException {
		// packet size 0x18
		// packet id 0x2C
		
		clean(writeBuffer);
		writeBuffer.putInt(objectID);
		writeBuffer.putInt(dataRequestID);
		sendPacket(0x2C);
	}

	/**
	 * The <code>aIRemoveObject</code> function is used to remove any object created by the client 
	 * using one of the AI creation functions.
	 * 
	 * <p> A client application can only remove AI controlled objects that it created, not objects created 
	 * by other clients, or Flight Simulator itself. </p>
	 * 
	 * @param objectID Specifies the server defined object ID (refer to the {@link flightsim.simconnect.recv.RecvAssignedObjectID} structure). 
	 * @param dataRequestID  Specifies the client defined request ID.
	 * @see #aICreateEnrouteATCAircraft(String, String, int, String, double, boolean, int)
	 * @see #aICreateParkedATCAircraft(String, String, String, int)
	 * @see #aICreateNonATCAircraft(String, String, InitPosition, int)
	 * @see #aICreateSimulatedObject(String, InitPosition, int)
	 * @throws IOException 
	 * @since 0.4
	 */
	public synchronized void aIRemoveObject(int objectID, Enum dataRequestID) throws IOException {
		aIRemoveObject(objectID, dataRequestID.ordinal());
	}

	/**
	 * The <code>aISetAircraftFlightPlan</code> function is used to set or change the flight plan of 
	 * an AI controlled aircraft.
	 * 
	 * <p> A number of errors, including {@link flightsim.simconnect.recv.SimConnectException#LOAD_FLIGHTPLAN_FAILED}, 
	 * apply to AI objects (refer to the {@link flightsim.simconnect.recv.SimConnectException} enum for more details).
	 * </p>
	 * 
	 * <p> Typically this function would be used some time after the aircraft was created using the 
	 * {@link #aICreateParkedATCAircraft(String, String, String, int)} call. </p>
	 * 
	 * @param objectID Specifies the server defined object ID. 
	 * @param flightPlanPath string containing the path to the flight plan file. 
	 * 				Flight plans have the extension .pln, but no need to enter an extension here. 
	 * 				The easiest way to create flight plans is to create them from within Flight Simulator itself, 
	 * 				and then save them off for use with the AI controlled aircraft. 
	 * 				There is no need to enter the full path (just the filename) if the flight plan is in the default
	 * 				 Flight Simulator X Files directory.
	 * @param dataRequestID Specifies client defined request ID.
	 * @see #aICreateEnrouteATCAircraft(String, String, int, String, double, boolean, int)
	 * @see #aICreateParkedATCAircraft(String, String, String, int)
	 * @see #aICreateNonATCAircraft(String, String, InitPosition, int)
	 * @throws IOException 
	 */
	public synchronized void aISetAircraftFlightPlan(int objectID, String flightPlanPath, 
			int dataRequestID) throws IOException {
		// packet size 0x11C
		// packet id 0x2D
		
		clean(writeBuffer);
		writeBuffer.putInt(objectID);
		putString(writeBuffer, flightPlanPath, 260);
		writeBuffer.putInt(dataRequestID);
		sendPacket(0x2D);
	}
	
	/**
	 * The <code> function is used to execute the mission action specified by a GUID.
	 * 
	 * <p> The GUID must be exactly 16 bytes long </p>
	 * 
	 * <p> A mission is specified in an xml file (see the Mission Creation documentation). 
	 * In order to use SimConnect_ExecuteMissionAction, typically there should be at least one 
	 * custom action within the mission xml file. The custom action will initiate the sending of 
	 * a notification to the SimConnect client, and the client can then both do some processing 
	 * of its own and run, by calling {@link #executeMissionAction(byte[])}, one or more actions (spoken text,
	 *  for example) that are defined within the xml file. </p>

	 * @param guidInstanceId GUID of the Mission Action. The GUID should be found in the associated mission xml file.
	 * @throws IOException 
	 * @throws IllegalAccessException if GUID is not 16 bytes long
	 */
	public synchronized void executeMissionAction(byte[] guidInstanceId) throws IOException {
		// packet size 0x20
		// packet id 0x2E
		
		if (guidInstanceId.length != 16) throw new IllegalArgumentException(Messages.get("SimConnect.GUID_invalid_size")); //$NON-NLS-1$
		clean(writeBuffer);
		writeBuffer.put(guidInstanceId);
		sendPacket(0x2E);
	}
	
	/**
	 * The <code>completeCustomMissionAction</code> function is used to complete the mission action 
	 * specified by a GUID.
	 * 
	 * 
	 * <p> The GUID must be exactly 16 bytes long </p>
	 * 
	 * <p> A mission is specified in an xml file (see the Mission Creation documentation). 
	 * A custom action is defined within this xml file, and will look similar to the following:
	 * <pre>
<SimMission.CustomAction InstanceId="{ GUID }">
<PayLoadString>Any string goes here!</PayLoadString>
<WaitForCompletion>True</WaitForCompletion>
</SimMission.CustomAction>
</pre>
	* Custom actions provide a mechanism to add complex processing to the basically data-driven mission 
	* system within Flight Simulator.</p>
	* 
	* <p> The custom action would typically be triggered from within the mission xml file (a trigger 
	* referencing the GUID of the custom action), though it could be called from within the SimConnect 
	* client with a call to {@link #executeMissionAction(byte[])}. It is only necessary to call 
	* {@link #completeCustomMissionAction(byte[])} if the <i>WaitForCompletion</i> value is set to True. </p>
	* 
	* <p> If the client calls {@link #executeMissionAction(byte[])} from within the code for a 
	* custom action, and it is important that this action completes before any other actions are 
	* started (that is, WaifForCompletion is True) then a second custom action should be defined 
	* that calls {@link #completeCustomMissionAction(byte[])} after that action is complete, and with
	*  the GUID of the first custom action as its parameter. The working sample shows this process. </p>
	*  
	* <p> In order to received notifications that a custom action is to be executed, the SimConnect
	*  client should use the {@link #subscribeToSystemEvent(int, String)} call with the <code>SystemEventName</code>
	*  parameter set to <i>"CustomMissionActionExecuted"</i>. This will result in the GUID of the 
	*  custom action, and the PayLoadString, being sent to the client in a 
	*  {@link flightsim.simconnect.recv.RecvCustomAction} structure. </p>
	*  
	*  <p> If a mission requires additional processing on its completion the SimConnect client 
	*  should use the {@link #subscribeToSystemEvent(int, String)} call with the <code>SystemEventName</code>
	*  parameter set to <i>"MissionCompleted"</i>. </p>
	 * 
	 * @param guidInstanceId GUID of the Mission Action. The GUID should be found in the associated mission xml file.
	 * @throws IOException 
	 * @throws IllegalAccessException if GUID is not 16 bytes long
	 */
	public synchronized void completeCustomMissionAction(byte[] guidInstanceId) throws IOException {
		// packet size 0x20
		// packet id 0x2F
		
		if (guidInstanceId.length != 16) throw new IllegalArgumentException(Messages.get("SimConnect.GUID_invalid_size")); //$NON-NLS-1$
		clean(writeBuffer);
		writeBuffer.put(guidInstanceId);
		sendPacket(0x2F);
	}
	
	/**
	 * returns the ID of the last packet sent to the SimConnect server.
	 * 
	 * <p>
	 * This function should be used in conjunction with returned structures of type 
	 * {@link flightsim.simconnect.recv.RecvException} to help pinpoint errors (exceptions) 
	 * returned by the server. This is done by matching the send ID returned with the exception, 
	 * with the number returned by this function and stored appropriately. This function is 
	 * primarily intended to be used while debugging and testing the client application, rather
	 * than in a final retail build.</p>
	 * 
	 * 
	 */
	public synchronized int getLastSentPacketID() {
		return currentIndex - 1;
	}
	
	/**
	 * The <code>requestResponseTimes</code> function is used to provide some data on the performance of the client-server connection.
	 * 
	 * @param nCount Integer containing the number of elements in the array of floats. 
	 * This should be set to five for the full range of timings, but can be less if only the 
	 * first few are of interest. There is no point creating an array of greater than five floats.
	 * @return An array of <code>nCount</code> floats, containing the times. 
	 * 		The five elements will contain the following: 
	 * 0 - total round trip time, 
	 * 1 - time from the request till the packet is sent, 
	 * 2 - time from the request till the packet is received by the server, 
	 * 3 - time from the request till the response is made by the server, 
	 * 4 - time from the server response to the client receives the packet.
	 */
	public synchronized float[] requestResponseTimes(int nCount) throws IOException {
		// TODO: implement simconnect function
		// this one needs special care: it send a packet (id 0x03, one param : nCount) 
		// and receive 8 float data (with response id 0x00010001) . Some calculations
		// has to be done
		throw new UnsupportedOperationException(Messages.get("SimConnect.Unimplemented")); //$NON-NLS-1$
	}
	
	/**
	 * The <code>cameraSetRelative6DOF</code> function is used to adjust the user's aircraft view camera.
	 * 
	 * <p> Any one of the six parameters can be set to {@link SimConnectConstants#CAMERA_IGNORE_FIELD} 
	 * which indicates that the value for the camera should be taken unmodified from the reference point.
	 * </p>
	 * 
	 * @param deltaX Float containing the delta in the x-axis from the eyepoint reference point. See the [views] section of the Aircraft Configuration Files document for a description of the eyepoint. 
	 * @param deltaY  Float containing the delta in the y-axis from the eyepoint reference point. 
	 * @param deltaZ Float containing the delta in the z-axis from the eyepoint reference point. 
	 * @param pitchDeg Float containing the pitch in degrees (rotation about the x axis). 
	 * A postive value points the nose down, a negative value up. The range of allowable values 
	 * is +90 to -90 degrees.
	 * @param bankDeg Float containing the bank angle in degrees (rotation about the z axis). 
	 * The range of allowable values is +180 to -180 degrees.
	 * @param headingDeg Float containing the heading in degrees (rotation about the y axis). 
	 * A positive value rotates the view right, a negative value left. If the user is viewing the 2D cockpit, 
	 * the view will change to the Virtual Cockpit 3D view if the angle exceeds 45 degrees from the
	 *  view ahead. The Virtual Cockpit view will change back to the 2D cockpit view if the heading 
	 *  angle drops below 45 degrees. The range of allowable values is +180 to -180 degrees.
	 * @throws IOException 
	 */
	public synchronized void cameraSetRelative6DOF(float deltaX, float deltaY, float deltaZ, 
			float pitchDeg, float bankDeg, float headingDeg) throws IOException {
		// packet size 0x28
		// packet id 0x30
		
		clean(writeBuffer);
		writeBuffer.putFloat(deltaX);
		writeBuffer.putFloat(deltaY);
		writeBuffer.putFloat(deltaZ);
		writeBuffer.putFloat(pitchDeg);
		writeBuffer.putFloat(bankDeg);
		writeBuffer.putFloat(headingDeg);
		sendPacket(0x30);
	}
	
	/**
	 * The <code>menuAddItem</code> function is used to add a menu item, associated with a client event.
	 * 
	 * <p> The menu item will be added to the Add-ons menu. The Add-ons menu will only appear in Flight
	 * imulator if there is at least one menu entry. Sub-menu items can be associated with this menu item, 
	 * see {@link #menuAddSubItem(int, String, int, int)}. If the text for the menu item should change, 
	 * then remove the menu item first before adding the menu item with the correct text 
	 * (see {@link #menuDeleteItem(int)}). </p>
	 * 
	 * @param menuItem string containing the text for the menu item. 
	 * @param clientMenuEventID specifies the client defined event ID, that is to be 
	 * 			transmitted when the menu item is selected (in the {@link flightsim.simconnect.recv.RecvEvent#getEventID()} parameter of the 
	 * 			{@link flightsim.simconnect.recv.RecvEvent} structure).
	 * @param data  Contains a data value that the client can specifiy for its own use (
	 * 			it will be returned in the dwData parameter of the {@link flightsim.simconnect.recv.RecvEvent}
	 * 			structure
	 * @throws IOException
	 * @see #menuDeleteItem(int)
	 * @see #menuDeleteSubItem(int, int) 
	 */
	public synchronized void menuAddItem(String menuItem, int clientMenuEventID, int data) throws IOException {
		// packet size 0x118
		// packet id 0x31
		
		clean(writeBuffer);
		putString(writeBuffer, menuItem, 256);
		writeBuffer.putInt(clientMenuEventID);
		writeBuffer.putInt(data);
		sendPacket(0x31);
	}
	
	/**
	 * The <code>menuAddItem</code> function is used to add a menu item, associated with a client event.
	 * 
	 * <p> The menu item will be added to the Add-ons menu. The Add-ons menu will only appear in Flight
	 * imulator if there is at least one menu entry. Sub-menu items can be associated with this menu item, 
	 * see {@link #menuAddSubItem(int, String, int, int)}. If the text for the menu item should change, 
	 * then remove the menu item first before adding the menu item with the correct text 
	 * (see {@link #menuDeleteItem(int)}). </p>
	 * 
	 * @param menuItem string containing the text for the menu item. 
	 * @param clientMenuEventID specifies the client defined event ID, that is to be 
	 * 			transmitted when the menu item is selected (in the {@link flightsim.simconnect.recv.RecvEvent#getEventID()} parameter of the 
	 * 			{@link flightsim.simconnect.recv.RecvEvent} structure).
	 * @param data  Contains a data value that the client can specifiy for its own use (
	 * 			it will be returned in the dwData parameter of the {@link flightsim.simconnect.recv.RecvEvent}
	 * 			structure
	 * @throws IOException
	 * @see #menuDeleteItem(int)
	 * @see #menuDeleteSubItem(int, int) 
	 * @since 0.7 
	 */
	public synchronized void menuAddItem(String menuItem, Enum clientMenuEventID, int data) throws IOException {
		menuAddItem(menuItem, clientMenuEventID.ordinal(), data);
	}

	
	/**
	 * The <code>menuDeleteItem</code> function is used to remove a client defined menu item.
	 * 
	 * <p> Menu items should be removed before a client closes. Removing the main menu 
	 * item will remove any associated sub-menu items. Also see the remarks for {@link #menuAddItem(String, int, int)}. </p>
	 * 
	 * @param clientMenuEventID Specifies the client defined event ID.
	 * @see #menuDeleteSubItem(int, int)
	 * @throws IOException 
	 */
	public synchronized void menuDeleteItem(int clientMenuEventID) throws IOException {
		// packet size 0x14
		// packet id 0x32
		
		clean(writeBuffer);
		writeBuffer.putInt(clientMenuEventID);
		sendPacket(0x32);
	}

	/**
	 * The <code>menuDeleteItem</code> function is used to remove a client defined menu item.
	 * 
	 * <p> Menu items should be removed before a client closes. Removing the main menu 
	 * item will remove any associated sub-menu items. Also see the remarks for {@link #menuAddItem(String, int, int)}. </p>
	 * 
	 * @param clientMenuEventID Specifies the client defined event ID.
	 * @see #menuDeleteSubItem(int, int)
	 * @throws IOException 
	 * @since 0.7 
	 */
	public synchronized void menuDeleteItem(Enum clientMenuEventID) throws IOException {
		menuDeleteItem(clientMenuEventID.ordinal());
	}
	
	/**
	 * The <code>menuAddSubItem</code> function is used to add a sub-menu item, associated with a client event.
	 * 
	 * <p> A maximum of 16 sub-menu items may be added to any one main menu item. Sub-menu items are
	 *  always added to the end of the sub-menu item list. An exception, {@link flightsim.simconnect.recv.SimConnectException#TOO_MANY_OBJECTS},
	 *   will be returned if an attempt is made to add more than 16 sub-menu items. </p>
	 * 
	 * @param clientMenuEventID Specifies the client defined menu event ID. This is the ID of the menu item that 
	 * 			this item should be added to
	 * @param menuItem string containing the text for the sub-menu item. 
	 * @param clientSubMenuEventID specifies the client defined sub-menu ID, that is to be 
	 * 			transmitted when the menu item is selected (in the {@link flightsim.simconnect.recv.RecvEvent#getEventID()} parameter of the 
	 * 			{@link flightsim.simconnect.recv.RecvEvent} structure).
	 * 			This ID must be unique across all sub-menu items for the client. 
	 * @param data  Contains a data value that the client can specifiy for its own use (
	 * 			it will be returned in the dwData parameter of the {@link flightsim.simconnect.recv.RecvEvent}
	 * 			structure
	 * @see #menuAddItem(String, int, int)
	 * @see #menuDeleteItem(int)
	 * @see #menuDeleteSubItem(int, int)
	 * @throws IOException 
	 */
	public synchronized void menuAddSubItem(int clientMenuEventID, String menuItem, 
			int clientSubMenuEventID, int data) throws IOException {
		// packet size 0x11C
		// packet id 0x33
		
		clean(writeBuffer);
		writeBuffer.putInt(clientMenuEventID);
		putString(writeBuffer, menuItem, 256);
		writeBuffer.putInt(clientSubMenuEventID);
		writeBuffer.putInt(data);
		sendPacket(0x33);
	}
	
	/**
	 * The <code>menuAddSubItem</code> function is used to add a sub-menu item, associated with a client event.
	 * 
	 * <p> A maximum of 16 sub-menu items may be added to any one main menu item. Sub-menu items are
	 *  always added to the end of the sub-menu item list. An exception, {@link flightsim.simconnect.recv.SimConnectException#TOO_MANY_OBJECTS},
	 *   will be returned if an attempt is made to add more than 16 sub-menu items. </p>
	 * 
	 * @param clientMenuEventID Specifies the client defined menu event ID. This is the ID of the menu item that 
	 * 			this item should be added to
	 * @param menuItem string containing the text for the sub-menu item. 
	 * @param clientSubMenuEventID specifies the client defined sub-menu ID, that is to be 
	 * 			transmitted when the menu item is selected (in the {@link flightsim.simconnect.recv.RecvEvent#getEventID()} parameter of the 
	 * 			{@link flightsim.simconnect.recv.RecvEvent} structure).
	 * 			This ID must be unique across all sub-menu items for the client. 
	 * @param data  Contains a data value that the client can specifiy for its own use (
	 * 			it will be returned in the dwData parameter of the {@link flightsim.simconnect.recv.RecvEvent}
	 * 			structure
	 * @see #menuAddItem(String, int, int)
	 * @see #menuDeleteItem(int)
	 * @see #menuDeleteSubItem(int, int)
	 * @throws IOException 
	 * @since 0.7 
	 */
	public synchronized void menuAddSubItem(Enum clientMenuEventID, String menuItem, 
			Enum clientSubMenuEventID, int data) throws IOException {
		menuAddSubItem(clientMenuEventID.ordinal(), menuItem, clientSubMenuEventID.ordinal(), data);
	}
	
	/**
	 * The <code>menuDeleteSubItem</code> function is used to remove a specifed sub-menu item.
	 * 
	 * <p> If a sub-menu item is deleted from the middle of the sub-menu item list, the list will contract. </p>
	 * 
	 * @param clientMenuEventID Specifies the client defined menu event ID, from which the sub-menu item is to be removed. 
	 * @param clientSubMenuEventID Specifies the client defined sub-menu event ID.
	 * @throws IOException 
	 */
	public synchronized void menuDeleteSubItem(int clientMenuEventID, int clientSubMenuEventID) throws IOException {
		// packet size 0x18
		// packet id 0x34
		
		clean(writeBuffer);
		writeBuffer.putInt(clientMenuEventID);
		writeBuffer.putInt(clientSubMenuEventID);
		sendPacket(0x34);
	}

	/**
	 * The <code>menuDeleteSubItem</code> function is used to remove a specifed sub-menu item.
	 * 
	 * <p> If a sub-menu item is deleted from the middle of the sub-menu item list, the list will contract. </p>
	 * 
	 * @param clientMenuEventID Specifies the client defined menu event ID, from which the sub-menu item is to be removed. 
	 * @param clientSubMenuEventID Specifies the client defined sub-menu event ID.
	 * @throws IOException
	 * @since 0.7 
	 */
	public synchronized void menuDeleteSubItem(Enum clientMenuEventID, Enum clientSubMenuEventID) throws IOException {
		menuDeleteSubItem(clientMenuEventID.ordinal(), clientSubMenuEventID.ordinal());
	}

	/**
	 * 
	 * The <code>mapClientDataNameToID</code> method is used to asscociate an ID with a named 
	 * client data area.
	 * 
	 * <p> This function should be called once for each client data area: the client setting 
	 * up the data should call it just before a call to SimConnect_CreateClientData, and the 
	 * clients requesting the data should call it before any calls to 
	 * {@link #requestClientData(int, int, int, int, int)} are made. The name given to a client 
	 * data area must be unique, however by mapping an ID number to the name, calls to the 
	 * functions to set and request the data are made more efficient. </p>
	 * 
	 * @param clientDataName string containing the client data area name. This is the name 
	 * 		that another client will use to specify the data area. The name is not case-sensitive.
	 * @param clientDataID A unique ID for the client data area, specified by the client.
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #clearClientDataDefinition(int)
	 * @see #createClientData(int, int, boolean)
	 * @see #setClientData(int, int, int, int, int, byte[])
	 * @see #requestClientData(int, int, int, int, int)
	 * @throws IOException
	 */
	public synchronized void mapClientDataNameToID(String clientDataName, 
			int clientDataID) throws IOException {
		clean(writeBuffer);
		putString(writeBuffer, clientDataName, 256);
		writeBuffer.putInt(clientDataID);
		sendPacket(0x37);
	}
	
	/**
	 * 
	 * The <code>mapClientDataNameToID</code> method is used to asscociate an ID with a named 
	 * client data area.
	 * 
	 * <p> This function should be called once for each client data area: the client setting 
	 * up the data should call it just before a call to SimConnect_CreateClientData, and the 
	 * clients requesting the data should call it before any calls to 
	 * {@link #requestClientData(int, int, int, int, int)} are made. The name given to a client 
	 * data area must be unique, however by mapping an ID number to the name, calls to the 
	 * functions to set and request the data are made more efficient. </p>
	 * 
	 * @param clientDataName string containing the client data area name. This is the name 
	 * 		that another client will use to specify the data area. The name is not case-sensitive.
	 * @param clientDataID A unique ID for the client data area, specified by the client.
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #clearClientDataDefinition(int)
	 * @see #createClientData(int, int, boolean)
	 * @see #setClientData(int, int, int, int, int, byte[])
	 * @see #requestClientData(int, int, int, int, int)
	 * @throws IOException
	 * @since 0.5
	 */
	public synchronized void mapClientDataNameToID(String clientDataName, 
			Enum clientDataID) throws IOException {
		mapClientDataNameToID(clientDataName, clientDataID.ordinal());
	}
	
	/**
	 * The <code>createClientData</code> function is used to request the creation of a reserved 
	 * data area for this client.
	 * 
	 * <p> Use this function, along with the other client data functions, to reserve an area of 
	 * memory for client data on the server, that other clients can have read (or read and write) 
	 * access to. Specify the contents of the data area with the 
	 * {@link #addToClientDataDefinition(int, int, int, int)} call, and set the actual values with a 
	 * call to {@link #setClientData(int, int, int, int, int, byte[])}. Other clients can receive the 
	 * data with a call to {@link #requestClientData(int, int, int, int, int)}. </p>
	 * 
	 * <p> One client area can be referenced by any number of client data definitions. Typically 
	 * the name of the client area, and the data definitions, should be published appropriately 
	 * so other clients can be written to use them. Care should be taken to give the area a unique name. 
	 * </p>
	 * 
	 * @param clientDataID ID of the client data area. Before calling this function, call 
	 * 	{@link #mapClientDataNameToID(String, int)} to map an ID to a unique client area name.
	 * @param size the size of the data area in bytes. 
	 * @param readOnly Specify this flag if the data area can only be written to by this client 
	 * 		(the client creating the data area). By default other clients can write to this data area.
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #clearClientDataDefinition(int)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #setClientData(int, int, int, int, int, byte[])
	 * @see #requestClientData(int, int, int, int, int)
	 * @throws IOException
	 */
	public synchronized void createClientData(int clientDataID, int size, boolean readOnly) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(clientDataID);
		writeBuffer.putInt(size);
		writeBuffer.putInt(readOnly ? 1 : 0);
		sendPacket(0x38);
	}

	/**
	 * The <code>createClientData</code> function is used to request the creation of a reserved 
	 * data area for this client.
	 * 
	 * <p> Use this function, along with the other client data functions, to reserve an area of 
	 * memory for client data on the server, that other clients can have read (or read and write) 
	 * access to. Specify the contents of the data area with the 
	 * {@link #addToClientDataDefinition(int, int, int, int)} call, and set the actual values with a 
	 * call to {@link #setClientData(int, int, int, int, int, byte[])}. Other clients can receive the 
	 * data with a call to {@link #requestClientData(int, int, int, int, int)}. </p>
	 * 
	 * <p> One client area can be referenced by any number of client data definitions. Typically 
	 * the name of the client area, and the data definitions, should be published appropriately 
	 * so other clients can be written to use them. Care should be taken to give the area a unique name. 
	 * </p>
	 * 
	 * @param clientDataID ID of the client data area. Before calling this function, call 
	 * 	{@link #mapClientDataNameToID(String, int)} to map an ID to a unique client area name.
	 * @param size the size of the data area in bytes. 
	 * @param readOnly Specify this flag if the data area can only be written to by this client 
	 * 		(the client creating the data area). By default other clients can write to this data area.
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #clearClientDataDefinition(int)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #setClientData(int, int, int, int, int, byte[])
	 * @see #requestClientData(int, int, int, int, int)
	 * @since 0.5
	 * @throws IOException
	 */
	public synchronized void createClientData(Enum clientDataID, int size, boolean readOnly) throws IOException {
		createClientData(clientDataID.ordinal(), size, readOnly);
	}
	
	/**
	 * The <code>addToClientDataDefinition</code> function is used to add an offset and a 
	 * size in bytes, to a client data definition.
	 * 
	 * <p> This function must be called before a client data area can be written to or read 
	 * from. Typically this function would be called once for each variable that is going to be 
	 * read or written. Note that an error will not be given if the size of a data definition 
	 * exceeds the size of the client area - this is to allow for the case where definitions 
	 * are specified by one client before the relevant client area is created by another. </P>
	 * 
	 * <p> Whereas data definitions for client areas are defined in bytes, it is anticipated that 
	 * most clients will write in specific data types, and cast appropriately when the data is received.
	 * </p>
	 * 
	 * @param dataDefineID Specifies the ID of the client-defined client data definition.
	 * @param offset the offset into the client area, where the new addition is to start. 
	 * @param size the size of the new addition, in bytes. 
	 * @see #createClientData(int, int, boolean)
	 * @see #clearClientDataDefinition(int)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #setClientData(int, int, int, int, int, byte[])
	 * @see #requestClientData(int, int, int, int, int)
	 * @throws IOException
	 */
	public synchronized void addToClientDataDefinition(int dataDefineID, int offset, int size) throws IOException {
		addToClientDataDefinition(dataDefineID, offset, size, 0);
	}

	/**
	 * The <code>addToClientDataDefinition</code> function is used to add an offset and a 
	 * size in bytes, to a client data definition.
	 * 
	 * <p> This function must be called before a client data area can be written to or read 
	 * from. Typically this function would be called once for each variable that is going to be 
	 * read or written. Note that an error will not be given if the size of a data definition 
	 * exceeds the size of the client area - this is to allow for the case where definitions 
	 * are specified by one client before the relevant client area is created by another. </P>
	 * 
	 * <p> Whereas data definitions for client areas are defined in bytes, it is anticipated that 
	 * most clients will write in specific data types, and cast appropriately when the data is received.
	 * </p>
	 * 
	 * @param dataDefineID Specifies the ID of the client-defined client data definition.
	 * @param offset the offset into the client area, where the new addition is to start. 
	 * @param size the size of the new addition, in bytes. 
	 * @param reserved Reserved for future use
	 * @see #createClientData(int, int, boolean)
	 * @see #clearClientDataDefinition(int)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #setClientData(int, int, int, int, int, byte[])
	 * @see #requestClientData(int, int, int, int, int)
	 * @throws IOException
	 */
	public synchronized void addToClientDataDefinition(int dataDefineID, 
			int offset, int size, int reserved) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(dataDefineID);
		writeBuffer.putInt(offset);
		writeBuffer.putInt(size);
		writeBuffer.putInt(reserved);
		if (ourProtocol > 0x3)
			writeBuffer.putInt(-1);
		sendPacket(0x39);
	}
	
	/**
	 * The <code>addToClientDataDefinition</code> function is used to add an offset and a 
	 * size in bytes, to a client data definition.
	 * 
	 * <p> This function must be called before a client data area can be written to or read 
	 * from. Typically this function would be called once for each variable that is going to be 
	 * read or written. Note that an error will not be given if the size of a data definition 
	 * exceeds the size of the client area - this is to allow for the case where definitions 
	 * are specified by one client before the relevant client area is created by another. </P>
	 * 
	 * @param dataDefineID Specifies the ID of the client-defined client data definition.
	 * @param offset the offset into the client area, where the new addition is to start.  Set this to {@link SimConnectConstants#CLIENTDATAOFFSET_AUTO} 
	 * for the offsets to be calculated by the SimConnect server. 
	 * @param sizeOrType  Double word containing either the size of the client data in bytes, or one of the predefined values
	 * @param epsilon If data is requested only when it changes (see the flags parameter of {@link #requestClientData(int, int, int, ClientDataPeriod, int, int, int, int)} 
	 * a change will only be reported if it is greater than the value of this parameter (not greater than or equal 
	 * to). The default is zero, so even the tiniest change will initiate the transmission of data. Set this value 
	 * appropriately so insignificant changes are not transmitted. This can be used with integer data, the 
	 * floating point epsilon value is first truncated to its integer component before the comparison is made 
	 * (for example, an epsilon value of 2.9 truncates to 2, so a data change of 2 will not trigger a transmission, 
	 * and a change of 3 will do so). This parameter only applies if one of the six constant values listed above 
	 * has been set in the <code>sizeOrType</code> parameter, if a size has been specified SimConnect has no record
	 *  of the type of data being sent, so cannot do a meaningful comparison of values.
	 *  @param datumId Specifies a client defined datum ID. The default is zero. Use this to identify the data 
	 *  received if the data is being returned in tagged format (see the flags parameter of {@link #requestClientData(int, int, int, ClientDataPeriod, int, int, int, int)}. 
	 *  There is no need to specify datum IDs if the data is not being returned in tagged format.
	 *  
	 * @see #createClientData(int, int, boolean)
	 * @see #clearClientDataDefinition(int)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #setClientData(int, int, int, int, int, byte[])
	 * @see #requestClientData(int, int, int, int, int)
	 * @since 0.5
	 * @throws IOException
	 * @throws {@link UnsupportedOperationException} Invalid emulated protocol version (needed v3)
	 */
	public synchronized void addToClientDataDefinition(int dataDefineID, int offset, int sizeOrType, 
			float epsilon, int datumId) throws IOException {
		
        if (ourProtocol < 0x3)
            throw new UnsupportedOperationException(
                            Messages.getString("SimConnect.badversion")); //$NON-NLS-1$
    
		clean(writeBuffer);
		writeBuffer.putInt(dataDefineID);
		writeBuffer.putInt(offset);
		writeBuffer.putInt(sizeOrType);
		writeBuffer.putFloat(epsilon);
		writeBuffer.putInt(datumId);
		sendPacket(0x39);
	}

	/**
	 * The <code>addToClientDataDefinition</code> function is used to add an offset and a 
	 * size in bytes, to a client data definition.
	 * 
	 * <p> This function must be called before a client data area can be written to or read 
	 * from. Typically this function would be called once for each variable that is going to be 
	 * read or written. Note that an error will not be given if the size of a data definition 
	 * exceeds the size of the client area - this is to allow for the case where definitions 
	 * are specified by one client before the relevant client area is created by another. </P>
	 * 
	 * @param dataDefineID Specifies the ID of the client-defined client data definition.
	 * @param offset the offset into the client area, where the new addition is to start.  Set this to {@link SimConnectConstants#CLIENTDATAOFFSET_AUTO} 
	 * for the offsets to be calculated by the SimConnect server. 
	 * @param sizeOrType  Double word containing either the size of the client data in bytes, or one of the predefined values
	 * @param epsilon If data is requested only when it changes (see the flags parameter of {@link #requestClientData(int, int, int, ClientDataPeriod, int, int, int, int)} 
	 * a change will only be reported if it is greater than the value of this parameter (not greater than or equal 
	 * to). The default is zero, so even the tiniest change will initiate the transmission of data. Set this value 
	 * appropriately so insignificant changes are not transmitted. This can be used with integer data, the 
	 * floating point epsilon value is first truncated to its integer component before the comparison is made 
	 * (for example, an epsilon value of 2.9 truncates to 2, so a data change of 2 will not trigger a transmission, 
	 * and a change of 3 will do so). This parameter only applies if one of the six constant values listed above 
	 * has been set in the <code>sizeOrType</code> parameter, if a size has been specified SimConnect has no record
	 *  of the type of data being sent, so cannot do a meaningful comparison of values.
	 *  @param datumId Specifies a client defined datum ID. The default is zero. Use this to identify the data 
	 *  received if the data is being returned in tagged format (see the flags parameter of {@link #requestClientData(int, int, int, ClientDataPeriod, int, int, int, int)}. 
	 *  There is no need to specify datum IDs if the data is not being returned in tagged format.
	 *  
	 * @see #createClientData(int, int, boolean)
	 * @see #clearClientDataDefinition(int)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #setClientData(int, int, int, int, int, byte[])
	 * @see #requestClientData(int, int, int, int, int)
	 * @since 0.5
	 * @throws IOException
	 */
	public synchronized void addToClientDataDefinition(Enum dataDefineID, int offset, int sizeOrType, 
			float epsilon, int datumId) throws IOException {
		addToClientDataDefinition(dataDefineID.ordinal(), offset, sizeOrType, epsilon, datumId);
	}

	/**
	 * The <code>addToClientDataDefinition</code> function is used to add an offset and a 
	 * size in bytes, to a client data definition.
	 * 
	 * <p> This function must be called before a client data area can be written to or read 
	 * from. Typically this function would be called once for each variable that is going to be 
	 * read or written. Note that an error will not be given if the size of a data definition 
	 * exceeds the size of the client area - this is to allow for the case where definitions 
	 * are specified by one client before the relevant client area is created by another. </P>
	 * 
	 * @param dataDefineID Specifies the ID of the client-defined client data definition.
	 * @param sizeOrType  Double word containing either the size of the client data in bytes, or one of the predefined values
	 *  
	 * @see #createClientData(int, int, boolean)
	 * @see #clearClientDataDefinition(int)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #setClientData(int, int, int, int, int, byte[])
	 * @see #requestClientData(int, int, int, int, int)
	 * @since 0.5
	 * @throws IOException
	 */
	public synchronized void addToClientDataDefinition(Enum dataDefineID, int sizeOrType) 
			throws IOException {
		addToClientDataDefinition(dataDefineID.ordinal(), SimConnectConstants.CLIENTDATAOFFSET_AUTO, 
				sizeOrType, 0.0f, 0);
	}

	
	/**
	 * Clear the definition of the specified client data.
	 * @param dataDefineID Specifies the ID of the client defined client data definition.
	 * @throws IOException Standard IO Failure
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #requestClientData(int, int, int, int, int)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #createClientData(int, int, boolean)
	 */
	public synchronized void clearClientDataDefinition(int dataDefineID) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(dataDefineID);
		sendPacket(0x3A);
	}

	/**
	 * Request that the specified data in an area created by another client be sent to this client.
	 * @param clientDataID Specifies the ID of the client data area. 
	 * 				Before calling this function for the first time on one client area, 
	 * 				call {@link #mapClientDataNameToID(String, int)} to map an ID to the unique client 
	 * 				data area name. This name must match the name specified by the client creating
	 * 				the data area with the {@link #mapClientDataNameToID(String, int)} and 
	 * 				{@link #createClientData(int, int, boolean)}. 
	 * @param dataRequestID Specifies the ID of the client-defined request. This is used later by the client to identify which data has been received. This value should be unique for each request, re-using a RequestID will overwrite any previous request using the same ID. 
	 * @param clientDataDefineID Specifies the ID of the client-defined data definition. This definition specifies the data that should be sent to the client.
	 * @see #requestClientData(int, int, int, int, int)
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #requestClientData(int, int, int, int, int)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #createClientData(int, int, boolean)
	 * @throws IOException Standard IO failure
	 */
	public synchronized void requestClientData(int clientDataID, int dataRequestID, 
			int clientDataDefineID) throws IOException {
		requestClientData(clientDataID, dataRequestID, clientDataDefineID, UNUSED, 0);
	}
	
	/**
	 * Request that the specified data in an area created by another client be sent to this client.
	 * @param clientDataID Specifies the ID of the client data area. 
	 * 				Before calling this function for the first time on one client area, 
	 * 				call {@link #mapClientDataNameToID(String, int)} to map an ID to the unique client 
	 * 				data area name. This name must match the name specified by the client creating
	 * 				the data area with the {@link #mapClientDataNameToID(String, int)} and 
	 * 				{@link #createClientData(int, int, boolean)}. 
	 * @param dataRequestID Specifies the ID of the client-defined request. This is used later by the client to identify which data has been received. This value should be unique for each request, re-using a RequestID will overwrite any previous request using the same ID. 
	 * @param clientDataDefineID Specifies the ID of the client-defined data definition. This definition specifies the data that should be sent to the client.
	 * @param reserved1 Reserved for future use.
	 * @param reserved2 Reserved for future use.
	 * @see #requestClientData(int, int, int, int, int)
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #requestClientData(int, int, int, int, int)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #createClientData(int, int, boolean)
	 * @see #clearClientDataDefinition(int)
	 * @throws IOException Standard IO failure
	 */
	public synchronized void requestClientData(int clientDataID, int dataRequestID, 
			int clientDataDefineID, int reserved1, int reserved2) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(clientDataID);
		writeBuffer.putInt(dataRequestID);
		writeBuffer.putInt(clientDataDefineID);
		if (ourProtocol >= 0x3) {
			writeBuffer.putInt(SimConnectPeriod.ONCE.ordinal());
			writeBuffer.putInt(0);
			writeBuffer.putInt(0);
			writeBuffer.putInt(0);
			writeBuffer.putInt(0); 
		} else {
			// RTM
			writeBuffer.putInt(reserved1);
			writeBuffer.putInt(reserved2);
		}
		sendPacket(0x3B);
	}
	
	/**
	 * Request that the specified data in an area created by another client be sent to this client.
	 * @param clientDataID Specifies the ID of the client data area. 
	 * 				Before calling this function for the first time on one client area, 
	 * 				call {@link #mapClientDataNameToID(String, int)} to map an ID to the unique client 
	 * 				data area name. This name must match the name specified by the client creating
	 * 				the data area with the {@link #mapClientDataNameToID(String, int)} and 
	 * 				{@link #createClientData(int, int, boolean)}. 
	 * @param dataRequestID Specifies the ID of the client-defined request. This is used later by the client to identify which data has been received. This value should be unique for each request, re-using a RequestID will overwrite any previous request using the same ID. 
	 * @param clientDataDefineID Specifies the ID of the client-defined data definition. This definition specifies the data that should be sent to the client.
	 * @param period One member of the {@link SimConnectPeriod} enumeration type, specifying how often the data is to be sent by the server and received by the client.
	 * @param flags A flag {@link SimConnectConstants#CLIENT_DATA_REQUEST_FLAG_CHANGED} or {@link SimConnectConstants#CLIENT_DATA_REQUEST_FLAG_DEFAULT}  or {@link SimConnectConstants#CLIENT_DATA_REQUEST_FLAG_TAGGED}
	 * @param origin The number of Period events that should elapse before transmission of the data begins. 
	 * 		The default is zero, which means transmissions will start immediately.
	 * @param interval The number of Period events that should elapse between transmissions of the data. 
	 * 		The default is zero, which means the data is transmitted every Period
	 * @param limit The number of times the data should be transmitted before this communication is ended. 
	 * 		The default is zero, which means the data should be transmitted endlessly.
	 * @see #requestClientData(int, int, int, int, int)
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #requestClientData(int, int, int, int, int)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #createClientData(int, int, boolean)
	 * @see #clearClientDataDefinition(int)
	 * @since 0.5
	 * @throws IOException Standard IO failure
	 * @throws UnsupportedOperationException if the emulated protocol version is too low (need 0x3)
	 */
	public synchronized void requestClientData(int clientDataID, int dataRequestID, 
			int clientDataDefineID, ClientDataPeriod period, int flags,
			int origin, int interval, int limit) throws IOException {
		
		if (ourProtocol < 0x3)
			throw new UnsupportedOperationException(
					Messages.getString("SimConnect.badversion")); //$NON-NLS-1$

		clean(writeBuffer);
		writeBuffer.putInt(clientDataID);
		writeBuffer.putInt(dataRequestID);
		writeBuffer.putInt(clientDataDefineID);
		writeBuffer.putInt(period.ordinal());
		writeBuffer.putInt(flags);
		writeBuffer.putInt(origin);
		writeBuffer.putInt(interval);
		writeBuffer.putInt(limit);
		sendPacket(0x3B);
	}
	
	/**
	 * Request that the specified data in an area created by another client be sent to this client.
	 * @param clientDataID Specifies the ID of the client data area. 
	 * 				Before calling this function for the first time on one client area, 
	 * 				call {@link #mapClientDataNameToID(String, int)} to map an ID to the unique client 
	 * 				data area name. This name must match the name specified by the client creating
	 * 				the data area with the {@link #mapClientDataNameToID(String, int)} and 
	 * 				{@link #createClientData(int, int, boolean)}. 
	 * @param dataRequestID Specifies the ID of the client-defined request. This is used later by the client to identify which data has been received. This value should be unique for each request, re-using a RequestID will overwrite any previous request using the same ID. 
	 * @param clientDataDefineID Specifies the ID of the client-defined data definition. This definition specifies the data that should be sent to the client.
	 * @param period One member of the {@link SimConnectPeriod} enumeration type, specifying how often the data is to be sent by the server and received by the client.
	 * @param flags A flag {@link SimConnectConstants#CLIENT_DATA_REQUEST_FLAG_CHANGED} or {@link SimConnectConstants#CLIENT_DATA_REQUEST_FLAG_DEFAULT}  or {@link SimConnectConstants#CLIENT_DATA_REQUEST_FLAG_TAGGED}
	 * @param origin The number of Period events that should elapse before transmission of the data begins. 
	 * 		The default is zero, which means transmissions will start immediately.
	 * @param interval The number of Period events that should elapse between transmissions of the data. 
	 * 		The default is zero, which means the data is transmitted every Period
	 * @param limit The number of times the data should be transmitted before this communication is ended. 
	 * 		The default is zero, which means the data should be transmitted endlessly.
	 * @see #requestClientData(int, int, int, int, int)
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #requestClientData(int, int, int, int, int)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #createClientData(int, int, boolean)
	 * @see #clearClientDataDefinition(int)
	 * @since 0.5
	 * @throws IOException Standard IO failure
	 * @throws UnsupportedOperationException if the emulated protocol version is too low (need 0x3)
	 */
	public synchronized void requestClientData(Enum clientDataID, Enum dataRequestID, 
			Enum clientDataDefineID, ClientDataPeriod period, int flags,
			int origin, int interval, int limit) throws IOException {
		requestClientData(clientDataID.ordinal(), dataRequestID.ordinal(), clientDataDefineID.ordinal(), 
				period, flags, origin, interval, limit);
	}


	/**
	 * Write one or more units of data to a client data area.
	 * @param clientDataID	Specifies the ID of the client data area.
	 * @param clientDataDefineID Specifies the ID of the client defined client data definition.
	 * @param reserved Reserved for future use. Set to zero.
	 * @param arrayCount Reserved for future use. Set to zero.
	 * @param unitSize Specifies the size of the data set in bytes. The server will check that this size matches exactly the size of the data definition provided in the DefineID parameter. An exception will be returned if this is not the case. 
	 * @param data data to be written
	 * @throws IOException Standard IO failure
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #clearClientDataDefinition(int)
	 * @see #createClientData(int, int, boolean)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #requestClientData(int, int, int, int, int)
	 */
	public synchronized void setClientData(int clientDataID, int clientDataDefineID, 
			int reserved, int arrayCount, int unitSize, byte [] data) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(clientDataID);
		writeBuffer.putInt(clientDataDefineID);
		writeBuffer.putInt(0);		// do not use arg	
		writeBuffer.putInt(1);		// do not use arg
		writeBuffer.putInt(unitSize);
		writeBuffer.put(data);
		sendPacket(0x3C);
	}

	/**
	 * Write one or more units of data to a client data area.
	 * @param clientDataID	Specifies the ID of the client data area.
	 * @param clientDataDefineID Specifies the ID of the client defined client data definition.
	 * @param data data to be written
	 * @throws IOException Standard IO failure
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #clearClientDataDefinition(int)
	 * @see #createClientData(int, int, boolean)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #requestClientData(int, int, int, int, int)
	 * @since 0.4
	 */
	public synchronized void setClientData(int clientDataID, int clientDataDefineID, 
			byte [] data) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(clientDataID);
		writeBuffer.putInt(clientDataDefineID);
		writeBuffer.putInt(0);		// do not use arg	
		writeBuffer.putInt(1);		// do not use arg
		writeBuffer.putInt(data.length);
		writeBuffer.put(data);
		sendPacket(0x3C);
	}
	
	/**
	 * Write one or more units of data to a client data area.
	 * @param clientDataID	Specifies the ID of the client data area.
	 * @param clientDataDefineID Specifies the ID of the client defined client data definition.
	 * @param reserved Reserved for future use. Set to zero.
	 * @param arrayCount Reserved for future use. Set to zero.
	 * @param unitSize Specifies the size of the data set in bytes. The server will check that this size matches exactly the size of the data definition provided in the DefineID parameter. An exception will be returned if this is not the case. 
	 * @param data data to be written
	 * @throws IOException Standard IO failure
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #clearClientDataDefinition(int)
	 * @see #createClientData(int, int, boolean)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #requestClientData(int, int, int, int, int)
	 * @since 0.2
	 */
	public synchronized void setClientData(int clientDataID, int clientDataDefineID, 
			int reserved, int arrayCount, int unitSize, ByteBuffer data) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(clientDataID);
		writeBuffer.putInt(clientDataDefineID);
		writeBuffer.putInt(0);		// do not use arg	
		writeBuffer.putInt(1);		// do not use arg
		writeBuffer.putInt(unitSize);
		writeBuffer.put(data);
		sendPacket(0x3C);
	}

	/**
	 * Write one or more units of data to a client data area.
	 * @param clientDataID	Specifies the ID of the client data area.
	 * @param clientDataDefineID Specifies the ID of the client defined client data definition.
	 * @param reserved Reserved for future use. Set to zero.
	 * @param arrayCount Reserved for future use. Set to zero.
	 * @param unitSize Specifies the size of the data set in bytes. The server will check that this size matches exactly the size of the data definition provided in the DefineID parameter. An exception will be returned if this is not the case. 
	 * @param data data to be written
	 * @throws IOException Standard IO failure
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #clearClientDataDefinition(int)
	 * @see #createClientData(int, int, boolean)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #requestClientData(int, int, int, int, int)
	 * @since 0.2
	 */
	public synchronized void setClientData(int clientDataID, int clientDataDefineID, 
			int reserved, int arrayCount, int unitSize, DataWrapper data) throws IOException {
		setClientData(clientDataID, clientDataDefineID, reserved, arrayCount, unitSize, data.getBuffer());
	}

	/**
	 * Write one or more units of data to a client data area.
	 * @param clientDataID	Specifies the ID of the client data area.
	 * @param clientDataDefineID Specifies the ID of the client defined client data definition.
	 * @param data data to be written
	 * @throws IOException Standard IO failure
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #clearClientDataDefinition(int)
	 * @see #createClientData(int, int, boolean)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #requestClientData(int, int, int, int, int)
	 * @since 0.4
	 */
	public synchronized void setClientData(int clientDataID, int clientDataDefineID, 
			ByteBuffer data) throws IOException {
		clean(writeBuffer);
		writeBuffer.putInt(clientDataID);
		writeBuffer.putInt(clientDataDefineID);
		writeBuffer.putInt(0);		// do not use arg	
		writeBuffer.putInt(1);		// do not use arg
		writeBuffer.putInt(data.remaining());
		writeBuffer.put(data);
		sendPacket(0x3C);
	}

	/**
	 * Write one or more units of data to a client data area.
	 * @param clientDataID	Specifies the ID of the client data area.
	 * @param clientDataDefineID Specifies the ID of the client defined client data definition.
	 * @param data data to be written
	 * @throws IOException Standard IO failure
	 * @see #addToClientDataDefinition(int, int, int, int)
	 * @see #clearClientDataDefinition(int)
	 * @see #createClientData(int, int, boolean)
	 * @see #mapClientDataNameToID(String, int)
	 * @see #requestClientData(int, int, int, int, int)
	 * @since 0.4
	 */
	public synchronized void setClientData(int clientDataID, int clientDataDefineID, 
			DataWrapper data) throws IOException {
		setClientData(clientDataID, clientDataDefineID, data.getBuffer());
	}

	/**
	 * The <code>flightLoad</code> function is used to load an existing flight file.
	 * 
	 * @param fileName string containing the path to the flight file. The path can either be absolute, 
	 * 			or relative to the My Documents\Flight Simulator X folder. Flight files have the extension 
	 * 			.FLT, but no need to enter an extension here.
	 * @throws IOException 
	 * @see #flightSave(String, String, int)
	 * @see #flightPlanLoad(String)
	 * 
	 */
	public synchronized void flightLoad(String fileName) throws IOException {
		// packet size 0x114
		// packet id 0x3D
		
		clean(writeBuffer);
		putString(writeBuffer, fileName, MAX_PATH);
		sendPacket(0x3D);
	}
	
	/**
	 * The <code>flightSave</code> function is used to save the current state of a flight to a flight file.
	 * 
	 * <p> max description len is 2048 bytes </p>
	 * 
	 * @param fileName string containing the path to the flight file. The path can either be absolute, 
	 * 			or relative to the My Documents\Flight Simulator X folder. Flight files have the extension 
	 * 			.FLT, but no need to enter an extension here.
	 * @param description string containing the text to enter in the Description field of the flight file.
	 * @param flags Unused
	 * @see #flightLoad(String)
	 * @see #flightPlanLoad(String)
	 * @throws IOException 
	 */
	public synchronized void flightSave(String fileName, String description, int flags) throws IOException {
		
		if (ourProtocol >= 0x4) {
			flightSave(fileName, fileName, description, flags);
		} else {
			// packet size 0x918 (SP1), 0xA1C (SP2)
			// packet id 0x3E
			
			clean(writeBuffer);
			putString(writeBuffer, fileName, MAX_PATH);
			putString(writeBuffer, description, 2048);
			writeBuffer.putInt(UNUSED);
			sendPacket(0x3E);
		}
		
	}

	/**
	 * The <code>flightSave</code> function is used to save the current state of a flight to a flight file.
	 * 
	 * <p> max description len is 2048 bytes </p>
	 * 
	 * @param fileName string containing the path to the flight file. The path can either be absolute, 
	 * 			or relative to the My Documents\Flight Simulator X folder. Flight files have the extension 
	 * 			.FLT, but no need to enter an extension here.
	 * @param title String containing the title of the flight file. If null, filename will be used as title
	 * @param description string containing the text to enter in the Description field of the flight file.
	 * @param flags Unused
	 * @see #flightLoad(String)
	 * @see #flightPlanLoad(String)
	 * @throws IOException
	 * @throws UnsupportedOperationException if this simconnect client protocol version is too old (requires v3) 
	 * @since 0.7
	 */
	public synchronized void flightSave(String fileName, String title, 
			String description, int flags) throws IOException, UnsupportedOperationException {
		
		// packet size 0xA1C (SP2)
		// packet id 0x3E
		
		if (ourProtocol < 0x4)
			throw new UnsupportedOperationException(
					Messages.getString("SimConnect.badversion")); //$NON-NLS-1$
		
		if (title == null)
			title = fileName;
		
		clean(writeBuffer);
		putString(writeBuffer, fileName, MAX_PATH);
		putString(writeBuffer, title, MAX_PATH);
		putString(writeBuffer, description, 2048);
		writeBuffer.putInt(UNUSED);
		sendPacket(0x3E);
	}

	/**
	 * The <code>flightPlanLoad</code> function is used to load an existing flight plan file.
	 * 
	 * @param fileName string containing the path to the flight plan file. The path can either be absolute, 
	 * 			or relative to the My Documents\Flight Simulator X folder. Flight files have the extension 
	 * 			.PLN, but no need to enter an extension here.
	 * @throws IOException 
	 * @see #flightLoad(String)
	 */
	public synchronized void flightPlanLoad(String fileName) throws IOException {
		// packet size 0x114
		// packet id 0x3F
		
		clean(writeBuffer);
		putString(writeBuffer, fileName, MAX_PATH);
		sendPacket(0x3F);
	}
	
	
	/**
	 * The <code>text</code> function is used to display a text menu, or scrolling or static text, on the screen.
	 * 
	 * @param type One member of the {@link TextType} enumeration type. 
	 * @param timeSeconds The timeout value for the text or menu, in seconds. If zero is entered, the text or 
	 * menu will not timeout. For text only, this timeout value can be overridden if there are other text requests 
	 * waiting in the queue
	 * @param eventId Specifies the client defined event ID, which will be returned along with the {@link TextResult} 
	 * (in the data parameter) of a {@link RecvEvent} structure. 
	 * @param message message to send
	 * @throws UnsupportedOperationException if this simconnect client protocol version is too old (requires v3) 
	 * 
	 * <br/>
	 * Only one text and one menu request can appear on the screen at one time. Requests are queued as they are 
	 * received, with only the topmost in the queue being displayed. There is one queue for menus and another 
	 * for static and scrolling text. When a request is first displayed the {@link TextResult#DISPLAYED} event
	 * will be sent to the client. If a request joins the queue and cannot immediately be displayed the event
	 * {@link TextResult#QUEUED} will be sent. When it is the turn of the new request to be displayed the 
	 * {@link TextResult#DISPLAYED} event will be sent. If the request is for a menu, and the user selects one
	 * f the menu entries, one of the {@link TextResult#MENU_SELECT_1} events will be returned (see the
	 * {@link TextResult} enumeration), and the menu closed.
	 * <br/> 
	 * The default location for static or scrolling text is along the top of the screen. A user can move and 
	 * resize the window that the text is being displayed in, but it is not possible to specify an alternative 
	 * location for the text programmatically.
	 * 
	 * <br/>
	 * If the timeout requested exceeds the minimum display time, and another text request is waiting, the timeout
	 * value is overridden and the text will only be displayed for the minimum display time. The default minimum
	 * display time is two seconds for static text and 10 seconds for scrolling text. These can be changed in
	 * the [SimConnect] section of FSX.cfg file using the TextMinPrintTimeSeconds and TextMinScrollTimeSeconds 
	 * settings. 
	 * 
	 * 
	 * @since 0.5
	 */
	public synchronized void text(int type, float timeSeconds, int eventId,
			String message) throws IOException, UnsupportedOperationException {
		
		if (ourProtocol < 0x3)
			throw new UnsupportedOperationException(
					Messages.getString("SimConnect.badversion")); //$NON-NLS-1$

		// packet id 0x40
		
		clean(writeBuffer);
		writeBuffer.putInt(type);
		writeBuffer.putFloat(timeSeconds);
		writeBuffer.putInt(eventId);
		if (message != null && message.length() > 0) {
			byte[] messageBytes = message.getBytes();
			writeBuffer.putInt(messageBytes.length+1);
			writeBuffer.put(messageBytes);
		} else {
			writeBuffer.putInt(1);
		}
		writeBuffer.put((byte) 0);
		sendPacket(0x40);
	}
	
	/**
	 * The <code>text</code> function is used to display a scrolling or static text, on the screen.
	 * 
	 * @param type One member of the {@link TextType} enumeration type. 
	 * @param timeSeconds The timeout value for the text or menu, in seconds. If zero is entered, the text or 
	 * menu will not timeout. For text only, this timeout value can be overridden if there are other text requests 
	 * waiting in the queue
	 * @param eventId Specifies the client defined event ID, which will be returned along with the {@link TextResult} 
	 * (in the data parameter) of a {@link RecvEvent} structure. 
	 * @param message message to send
	 * @throws UnsupportedOperationException if this simconnect client protocol version is too old (requires v3) 
	 * 
	 * <br/>
	 * Only one text and one menu request can appear on the screen at one time. Requests are queued as they are 
	 * received, with only the topmost in the queue being displayed. There is one queue for menus and another 
	 * for static and scrolling text. When a request is first displayed the {@link TextResult#DISPLAYED} event
	 * will be sent to the client. If a request joins the queue and cannot immediately be displayed the event
	 * {@link TextResult#QUEUED} will be sent. When it is the turn of the new request to be displayed the 
	 * {@link TextResult#DISPLAYED} event will be sent. If the request is for a menu, and the user selects one
	 * f the menu entries, one of the {@link TextResult#MENU_SELECT_1} events will be returned (see the
	 * {@link TextResult} enumeration), and the menu closed.
	 * <br/> 
	 * The default location for static or scrolling text is along the top of the screen. A user can move and 
	 * resize the window that the text is being displayed in, but it is not possible to specify an alternative 
	 * location for the text programmatically.
	 * 
	 * <br/>
	 * If the timeout requested exceeds the minimum display time, and another text request is waiting, the timeout
	 * value is overridden and the text will only be displayed for the minimum display time. The default minimum
	 * display time is two seconds for static text and 10 seconds for scrolling text. These can be changed in
	 * the [SimConnect] section of FSX.cfg file using the TextMinPrintTimeSeconds and TextMinScrollTimeSeconds 
	 * settings. 
	 * 
	 * @since 0.5
	 */
	public synchronized void text(TextType type, float timeSeconds, int eventId,
			String message) throws IOException, UnsupportedOperationException {
		
		text(type.value(), timeSeconds, eventId, message);
	}
	
	/**
	 * The <code>text</code> function is used to display a scrolling or static text, on the screen.
	 * 
	 * @param type One member of the {@link TextType} enumeration type. 
	 * @param timeSeconds The timeout value for the text or menu, in seconds. If zero is entered, the text or 
	 * menu will not timeout. For text only, this timeout value can be overridden if there are other text requests 
	 * waiting in the queue
	 * @param eventId Specifies the client defined event ID, which will be returned along with the {@link TextResult} 
	 * (in the data parameter) of a {@link RecvEvent} structure. 
	 * @param message message to send
	 * @throws UnsupportedOperationException if this simconnect client protocol version is too old (requires v3) 
	 * 
	 * <br/>
	 * Only one text and one menu request can appear on the screen at one time. Requests are queued as they are 
	 * received, with only the topmost in the queue being displayed. There is one queue for menus and another 
	 * for static and scrolling text. When a request is first displayed the {@link TextResult#DISPLAYED} event
	 * will be sent to the client. If a request joins the queue and cannot immediately be displayed the event
	 * {@link TextResult#QUEUED} will be sent. When it is the turn of the new request to be displayed the 
	 * {@link TextResult#DISPLAYED} event will be sent. If the request is for a menu, and the user selects one
	 * f the menu entries, one of the {@link TextResult#MENU_SELECT_1} events will be returned (see the
	 * {@link TextResult} enumeration), and the menu closed.
	 * <br/>
	 * If the <code>message</code> is set to null, the text will be removed from with the queue, with
	 * a {@link TextResult#REMOVED} event being returned to the client
	 * <br/> 
	 * The default location for static or scrolling text is along the top of the screen. A user can move and 
	 * resize the window that the text is being displayed in, but it is not possible to specify an alternative 
	 * location for the text programmatically.
	 * 
	 * <br/>
	 * If the timeout requested exceeds the minimum display time, and another text request is waiting, the timeout
	 * value is overridden and the text will only be displayed for the minimum display time. The default minimum
	 * display time is two seconds for static text and 10 seconds for scrolling text. These can be changed in
	 * the [SimConnect] section of FSX.cfg file using the TextMinPrintTimeSeconds and TextMinScrollTimeSeconds 
	 * settings. 
	 * 
	 * @since 0.5
	 */
	public synchronized void text(TextType type, float timeSeconds, Enum eventId,
			String message) throws IOException, UnsupportedOperationException {
		
		text(type.value(), timeSeconds, eventId.ordinal(), message);
	}
	
	/**
	 * The <code>text</code> function is used to display a text menu.
	 * 
	 * @param timeSeconds The timeout value for the menu, in seconds. If zero is entered, the 
	 * menu will not timeout. 
	 * @param eventId Specifies the client defined event ID, which will be returned along with the {@link TextResult} 
	 * (in the data parameter) of a {@link RecvEvent} structure. 
	 * @param title Menu title
	 * @param prompt Menu prompt
	 * @param items Menu items (maximum 10)
	 * @throws UnsupportedOperationException if this simconnect client protocol version is too old (requires v3) 
	 * 
	 * <br/>
	 * Only one text and one menu request can appear on the screen at one time. Requests are queued as they are 
	 * received, with only the topmost in the queue being displayed. There is one queue for menus and another 
	 * for static and scrolling text. When a request is first displayed the {@link TextResult#DISPLAYED} event
	 * will be sent to the client. If a request joins the queue and cannot immediately be displayed the event
	 * {@link TextResult#QUEUED} will be sent. When it is the turn of the new request to be displayed the 
	 * {@link TextResult#DISPLAYED} event will be sent. If the request is for a menu, and the user selects one
	 * f the menu entries, one of the {@link TextResult#MENU_SELECT_1} events will be returned (see the
	 * {@link TextResult} enumeration), and the menu closed.
	 * <br/>
	 * If all parameters (title, prompt and items) are set to null, the menu will be removed from with the queue, with
	 * a {@link TextResult#REMOVED} event being returned to the client
	 * <br/> 
	 * The default location for static or scrolling text is along the top of the screen. A user can move and 
	 * resize the window that the text is being displayed in, but it is not possible to specify an alternative 
	 * location for the text programmatically.
	 * 
	 * <br/>
	 * If the timeout requested exceeds the minimum display time, and another text request is waiting, the timeout
	 * value is overridden and the text will only be displayed for the minimum display time. The default minimum
	 * display time is two seconds for static text and 10 seconds for scrolling text. These can be changed in
	 * the [SimConnect] section of FSX.cfg file using the TextMinPrintTimeSeconds and TextMinScrollTimeSeconds 
	 * settings. 
	 * 
	 * @since 0.5
	 */
	public synchronized void menu(float timeSeconds, int eventId,
			String title, String prompt, String... items) throws IOException, UnsupportedOperationException {
		
		if (ourProtocol < 0x3)
			throw new UnsupportedOperationException(
					Messages.getString("SimConnect.badversion")); //$NON-NLS-1$
		
		// packet id 0x40
		
		clean(writeBuffer);
		writeBuffer.putInt(TextType.MENU.value());
		writeBuffer.putFloat(timeSeconds);
		writeBuffer.putInt(eventId);
		writeBuffer.putInt(0);		// size, will be set later
		if (title == null && prompt == null && items == null) {
			writeBuffer.put((byte) 0);
		} else {
			writeBuffer.put(title.getBytes());
			writeBuffer.put((byte) 0);
			writeBuffer.put(prompt.getBytes());
			writeBuffer.put((byte) 0);
			for (String s : items) {
				if (s != null) {
					byte[] itemBytes = s.getBytes();
					writeBuffer.put(itemBytes);
					writeBuffer.put((byte) 0);
				}
			}
		}
		// set size
		writeBuffer.putInt(28, writeBuffer.position() - 32);
		
		sendPacket(0x40);
	}

	/**
	 * The <code>text</code> function is used to display a text menu.
	 * 
	 * @param timeSeconds The timeout value for the menu, in seconds. If zero is entered, the 
	 * menu will not timeout. 
	 * @param eventId Specifies the client defined event ID, which will be returned along with the {@link TextResult} 
	 * (in the data parameter) of a {@link RecvEvent} structure. 
	 * @param title Menu title
	 * @param prompt Menu prompt
	 * @param items Menu items (maximum 10)
	 * @throws {@link UnsupportedOperationException} if this simconnect client protocol version is too old (requires v3) 
	 * 
	 * <br/>
	 * Only one text and one menu request can appear on the screen at one time. Requests are queued as they are 
	 * received, with only the topmost in the queue being displayed. There is one queue for menus and another 
	 * for static and scrolling text. When a request is first displayed the {@link TextResult#DISPLAYED} event
	 * will be sent to the client. If a request joins the queue and cannot immediately be displayed the event
	 * {@link TextResult#QUEUED} will be sent. When it is the turn of the new request to be displayed the 
	 * {@link TextResult#DISPLAYED} event will be sent. If the request is for a menu, and the user selects one
	 * f the menu entries, one of the {@link TextResult#MENU_SELECT_1} events will be returned (see the
	 * {@link TextResult} enumeration), and the menu closed.
	 * <br/>
	 * If all parameters (title, prompt and items) are set to null, the menu will be removed from with the queue, with
	 * a {@link TextResult#REMOVED} event being returned to the client
	 * <br/> 
	 * The default location for static or scrolling text is along the top of the screen. A user can move and 
	 * resize the window that the text is being displayed in, but it is not possible to specify an alternative 
	 * location for the text programmatically.
	 * 
	 * <br/>
	 * If the timeout requested exceeds the minimum display time, and another text request is waiting, the timeout
	 * value is overridden and the text will only be displayed for the minimum display time. The default minimum
	 * display time is two seconds for static text and 10 seconds for scrolling text. These can be changed in
	 * the [SimConnect] section of FSX.cfg file using the TextMinPrintTimeSeconds and TextMinScrollTimeSeconds 
	 * settings. 
	 * 
	 * @since 0.5
	 */
	public synchronized void menu(float timeSeconds, Enum eventId,
			String title, String prompt, String... items) throws IOException, UnsupportedOperationException {
		menu(timeSeconds, eventId.ordinal(), title, prompt, items);
	}

	/**
	 * The <code>requestFacilitiesList</code> function is used to request a list of all the facilities 
	 * of a given type currently held in the facilities cache.
	 * 
	 * @param type Specifies one member of the {@link FacilityListType} enumeration type. 
	 * @param eventId Specifies the client defined request ID. This will be returned along with the data.
	 * @since 0.5
	 * @throws IOException 
	 * @throws {@link UnsupportedOperationException} if this simconnect client protocol version is too old (requires v3) 
	 * 
	 * <br/>
	 * Flight Simulator X keeps a facilities cache of all the airports, waypoints, NDB and VOR stations within
	 * a certain radius of the user aircraft. This radius varies depending on where the aircraft is in the world, 
	 * but is at least large enough to encompass the whole of the reality bubble for airports and waypoints, 
	 * and can be over 200 miles for VOR and NDB stations. As the user aircraft moves facilities will be added 
	 * to, and removed from, the cache. However, in the interests of performance, hysteresis is built into 
	 * the system.
	 * <br/>
	 * To receive event notifications when a facility is added, use the {@link #subscribeToFacilities(FacilityListType, int)}
	 * function. When this function is first called, a full list from the cache will be sent, thereafter just 
	 * the additions will be transmitted. No notification is given when a facility is removed from the cache. 
	 * the Obviously to terminate these notifications use the {@link #unSubscribeToFacilities(FacilityListType)} function.
	 * <br/>
	 * When requesting types of facility information, one function call has to be made for each of the four 
	 * types of data. The data will be returned in one of the four structures:
	 * <ul>
	 * <li> {@link RecvAirportList}, which will contain a list of {@link FacilityAirport} structures. </li>
	 * <li> {@link RecvNDBList}, which will contain a list of {@link FacilityNDB} structures. </li>
	 * <li> {@link RecvVORList}, which will contain a list of {@link FacilityVOR} structures. </li>
	 * <li> {@link RecvWaypointList}, which will contain a list of {@link FacilityWaypoint} structures. </li>
	 * </ul>
	 * The four list structures inherit the data from the {@link RecvFacilitiesList} structure. Given that
	 * the list of returned facilities could be large, it may be split across several packets, and each packet 
	 * must be interpreted separately by the client. 
	 * 
	 */
	public synchronized void requestFacilitiesList(FacilityListType type, int eventId) throws IOException, UnsupportedOperationException {
		if (ourProtocol < 0x3)
			throw new UnsupportedOperationException(
					Messages.getString("SimConnect.badversion")); //$NON-NLS-1$
		// ID 0x43
		clean(writeBuffer);
		writeBuffer.putInt(type.ordinal());
		writeBuffer.putInt(eventId);
		sendPacket(0x43);
	}
	
	
	/**
	 * The <code>requestFacilitiesList</code> function is used to request a list of all the facilities 
	 * of a given type currently held in the facilities cache.
	 * 
	 * @param type Specifies one member of the {@link FacilityListType} enumeration type. 
	 * @param eventId Specifies the client defined request ID. This will be returned along with the data.
	 * @since 0.5
	 * @throws IOException 
	 * @throws {@link UnsupportedOperationException} if this simconnect client protocol version is too old (requires v3) 
	 * 
	 * <br/>
	 * Flight Simulator X keeps a facilities cache of all the airports, waypoints, NDB and VOR stations within
	 * a certain radius of the user aircraft. This radius varies depending on where the aircraft is in the world, 
	 * but is at least large enough to encompass the whole of the reality bubble for airports and waypoints, 
	 * and can be over 200 miles for VOR and NDB stations. As the user aircraft moves facilities will be added 
	 * to, and removed from, the cache. However, in the interests of performance, hysteresis is built into 
	 * the system.
	 * <br/>
	 * To receive event notifications when a facility is added, use the {@link #subscribeToFacilities(FacilityListType, int)} 
	 * function. When this function is first called, a full list from the cache will be sent, thereafter just 
	 * the additions will be transmitted. No notification is given when a facility is removed from the cache. 
	 * the Obviously to terminate these notifications use the {@link #unSubscribeToFacilities(FacilityListType)} function.
	 * <br/>
	 * When requesting types of facility information, one function call has to be made for each of the four 
	 * types of data. The data will be returned in one of the four structures:
	 * <ul>
	 * <li> {@link RecvAirportList}, which will contain a list of {@link FacilityAirport} structures. </li>
	 * <li> {@link RecvNDBList}, which will contain a list of {@link FacilityNDB} structures. </li>
	 * <li> {@link RecvVORList}, which will contain a list of {@link FacilityVOR} structures. </li>
	 * <li> {@link RecvWaypointList}, which will contain a list of {@link FacilityWaypoint} structures. </li>
	 * </ul>
	 * The four list structures inherit the data from the {@link RecvFacilitiesList} structure. Given that
	 * the list of returned facilities could be large, it may be split across several packets, and each packet 
	 * must be interpreted separately by the client. 
	 * 
	 */
	public synchronized void requestFacilitiesList(FacilityListType type, Enum eventId) throws IOException, UnsupportedOperationException {
		requestFacilitiesList(type, eventId.ordinal());
	}
	
	/**
	 * The <code>subscribeToFacilities</code> function is used to request notifications when a facility of 
	 * a certain type is added to the facilities cache.
	 * 
	 * @param type Specifies one member of the {@link FacilityListType} enumeration type. 
	 * @param eventId Specifies the client defined request ID. This will be returned along with the data.
	 * @see #requestFacilitiesList(FacilityListType, int)
	 * @since 0.5
	 * @throws IOException 
	 * @throws {@link UnsupportedOperationException} if this simconnect client protocol version is too old (requires v3) 
	 */
	public synchronized void subscribeToFacilities(FacilityListType type, int eventId) throws IOException, UnsupportedOperationException {
		if (ourProtocol < 0x3)
			throw new UnsupportedOperationException(
					Messages.getString("SimConnect.badversion")); //$NON-NLS-1$
		
		// ID 0x41
		clean(writeBuffer);
		writeBuffer.putInt(type.ordinal());
		writeBuffer.putInt(eventId);
		sendPacket(0x41);
	}

	/**
	 * The <code>subscribeToFacilities</code> function is used to request notifications when a facility of 
	 * a certain type is added to the facilities cache.
	 * 
	 * @param type Specifies one member of the {@link FacilityListType} enumeration type. 
	 * @param eventId Specifies the client defined request ID. This will be returned along with the data.
	 * @see #requestFacilitiesList(FacilityListType, int)
	 * @since 0.5
	 * @throws IOException 
	 * @throws {@link UnsupportedOperationException} if this simconnect client protocol version is too old (requires v3) 
	 */
	public synchronized void subscribeToFacilities(FacilityListType type, Enum eventId) throws IOException, UnsupportedOperationException {
		subscribeToFacilities(type, eventId.ordinal());
	}

	/**
	 * The <code>unSubscribeToFacilities</code> function is used to request that notifications of additions 
	 * to the facilities cache are not longer sent.
	 * 
	 * @param type Specifies one member of the {@link FacilityListType} enumeration type. 
	 * @see #requestFacilitiesList(FacilityListType, int)
	 * @since 0.5
	 * @throws IOException
	 * @throws {@link UnsupportedOperationException} if this simconnect client protocol version is too old (requires v3) 
	 */
	public synchronized void unSubscribeToFacilities(FacilityListType type) throws IOException, UnsupportedOperationException {
		if (ourProtocol < 0x3)
			throw new UnsupportedOperationException(
					Messages.getString("SimConnect.badversion")); //$NON-NLS-1$
		
		// ID 0x42
		clean(writeBuffer);
		writeBuffer.putInt(type.ordinal());
		sendPacket(0x42);
	}
	
	
	
	

	/**
	 * Process the next simConnect message received through the specified
	 * callback function.
	 * @param dispatcher the callback object
	 * @see #getNextData()
	 * @see AbstractDispatcher
	 * @see DispatcherTask
	 * @throws IOException Standard IO Failure
	 * @throws NullPointerException the dispatcher is null
	 */
	public void callDispatch(Dispatcher dispatcher) throws IOException {
		pumpNextData();
		dispatcher.dispatch(this, readBuffer);
	}
	
	/**
	 * Get next data from socket and put it into the embedded read
	 * buffer
	 * @throws IOException
	 */
	private void pumpNextData() throws IOException {
		readBuffer.clear();
		// set limit to 4 so we only get the size of upcoming packet
		readBuffer.limit(4);
		int rlen = sc.read(readBuffer);	
		if (rlen != 4) throw new IOException(Messages.get("SimConnect.Invalid_read"));		// bad //$NON-NLS-1$
		
		
		int dlen = readBuffer.getInt(0);
		if (dlen > readBuffer.capacity()) {
			throw new IOException(Messages.getString("SimConnect.PacketTooLarge")); //$NON-NLS-1$
		}
		
		readBuffer.position(4);		// pos to begin
		readBuffer.limit(dlen);		// limit at end
		rlen = 4;
		while (rlen < dlen) {
			rlen += sc.read(readBuffer);
		}
		packetsReceived ++;
		bytesReceived += dlen;
		if (rlen != dlen) throw new IOException(Messages.get("SimConnect.Short_read") +  //$NON-NLS-1$
				" (" + Messages.get("SimConnect.expected") + " " + dlen + " " +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				Messages.get("SimConnect.got") + " " + rlen + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		readBuffer.position(0);
	}
	
	/**
	 * Process the next simConnect message without using a dispatch function.
	 * Equivalent to <code> SimConnect_GetNextDispatch </code>
	 * @return a ByteBuffer containing the next received message
	 * @see #callDispatch(Dispatcher)
	 * @throws IOException standard IO failure
	 */
	public ByteBuffer getNextData() throws IOException {
		pumpNextData();
		return readBuffer;
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (sc.isConnected()) {
			sc.close();
		}
	}
	
	/**
	 * Statistics: return the number of bytes received
	 * @return number of bytes
	 */
	public int getReceivedBytes() {
		return bytesReceived;
	}
	/**
	 * Statistics: return the number of bytes sent
	 * @return number of bytes
	 */
	public int getSentBytes() {
		return bytesSent;
	}
	/**
	 * Statistics: return the number of packets received
	 * @return number of packets
	 */
	public int getReceivedPackets() {
		return packetsReceived;
	}
	/**
	 * Statistics: return the number of packets sent
	 * @return number of packets
	 */
	public int getSentPackets() {
		return packetsSent;
	}
	
	/**
	 * Returns the version number of the emulated protocol. 
	 * As of jsimconnect 0.7, returned values are 
	 * <ul>
	 * <li> 0x2 (from FSX RTM) </li>
	 * <li> 0x3 (from FSX SP1, supports enhanced client data, facilites, and modeless ui) </li>
	 * <li> 0x4 (from FSX SP2/Acceleration, racing and another flight save </li>
	 * </ul> 
	 * @since 0.7
	 * @return emulated protocol version
	 */
	public int getProtocolVersion() {
		return ourProtocol;
	}
}