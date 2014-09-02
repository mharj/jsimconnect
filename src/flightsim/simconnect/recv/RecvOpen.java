package flightsim.simconnect.recv;

import java.nio.ByteBuffer;


/**
 * The <code>RecvOpen</code> structure is used to return information 
 * to the client, after a successful connection of a SimConnect object.

 * @author lc0277
 *
 */
public class RecvOpen extends RecvPacket {
	private final String applicationName;
	private final int applicationVersionMajor;
	private final int applicationVersionMinor;
	private final int applicationBuildMajor;
	private final int applicationBuildMinor;
	private final int simConnectVersionMajor;
	private final int simConnectVersionMinor;
	private final int simConnectBuildMajor;
	private final int simConnectBuildMinor;
	private final int reserved1;
	private final int reserved2;
	
	RecvOpen(ByteBuffer bf) {
		super(bf, RecvID.ID_OPEN);
		applicationName = makeString(bf, 256);
		applicationVersionMajor = bf.getInt();
		applicationVersionMinor = bf.getInt();
		applicationBuildMajor = bf.getInt();
		applicationBuildMinor = bf.getInt();
		simConnectVersionMajor = bf.getInt();
		simConnectVersionMinor = bf.getInt();
		simConnectBuildMajor = bf.getInt();
		simConnectBuildMinor = bf.getInt();
		reserved1 = bf.getInt();
		reserved2 = bf.getInt();
	}

	public int getApplicationBuildMajor() {
		return applicationBuildMajor;
	}

	public int getApplicationBuildMinor() {
		return applicationBuildMinor;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public int getApplicationVersionMajor() {
		return applicationVersionMajor;
	}

	public int getApplicationVersionMinor() {
		return applicationVersionMinor;
	}

	public int getReserved1() {
		return reserved1;
	}

	public int getReserved2() {
		return reserved2;
	}

	public int getSimConnectBuildMajor() {
		return simConnectBuildMajor;
	}

	public int getSimConnectBuildMinor() {
		return simConnectBuildMinor;
	}

	public int getSimConnectVersionMajor() {
		return simConnectVersionMajor;
	}

	public int getSimConnectVersionMinor() {
		return simConnectVersionMinor;
	}
	
	
	@Override
	public String toString() {
		return applicationName + " ( ver " + applicationVersionMajor + "." + applicationVersionMinor +
		 " build " + applicationBuildMajor + "." + applicationBuildMinor + " ) simconnect "  +
		 simConnectVersionMajor + "." + simConnectVersionMinor + " build " + simConnectBuildMajor + "." + simConnectBuildMinor;
	}
	
	
	
	
}
