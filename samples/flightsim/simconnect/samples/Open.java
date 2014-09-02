package flightsim.simconnect.samples;


import flightsim.simconnect.SimConnect;
import flightsim.simconnect.recv.*;

public class Open {

	public static void main(String[] args) throws Exception {
		int protoVersion = 0x2;
		
		final SimConnect sc = new SimConnect("Open", "10.1.0.3", 48447, protoVersion);
		
		AbstractDispatcher dispatcher = new AbstractDispatcher() {
			@Override
			public void onDispatch(SimConnect simConnect, RecvPacket recv) {
				switch (recv.getID()) {
				case ID_OPEN:
					RecvOpen ro = (RecvOpen) recv;
					System.out.println("Connected to " + 
							ro.getApplicationName() + 
							" version " +
							ro.getApplicationVersionMajor() + "." +
							ro.getApplicationVersionMinor() + "." +
							ro.getApplicationBuildMajor() + "." +
							ro.getApplicationBuildMinor() + 
							" simconnect " +
							ro.getSimConnectVersionMajor() + "." +
							ro.getSimConnectVersionMinor() + "." +
							ro.getSimConnectBuildMajor() + "." +
							ro.getSimConnectBuildMinor());
					System.out.println("Protocol version: " + ro.getVersion());
					
					break;
					
				case ID_EXCEPTION:
					RecvException re = (RecvException) recv;
					System.out.println("Exception " + re.getException());
					break;
					
				default:
					System.out.println("Received packet id " + recv.getID());
				}
				
				
			}
		};
		
		while (true) {
			sc.callDispatch(dispatcher);
		}
	}
}
