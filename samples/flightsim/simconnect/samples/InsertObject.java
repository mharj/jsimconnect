package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.NotificationPriority;
import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.SimConnectPeriod;
import flightsim.simconnect.data.InitPosition;
import flightsim.simconnect.data.LatLonAlt;
import flightsim.simconnect.recv.AssignedObjectHandler;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvAssignedObjectID;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.recv.RecvSimObjectData;
import flightsim.simconnect.recv.SimObjectDataHandler;

public class InsertObject {

	
	public static void main(String[] args) throws Exception {
		
		if (args.length < 1) {
			System.err.println("Usage: InsertObject [-noslew] <Name>");
			System.err.println("Will insert object (as simulated obj) at user position");
			System.exit(0);
		}
		String argObj = args[0];
		boolean argSlew = true;
		
		if (args[0].equals("-noslew")) {
			argSlew = false;
			argObj = args[1];
		}
		
		final String objName = argObj;
		final boolean setSlew = argSlew;
		
		// connect to simconnect
		final SimConnect sc = new SimConnect("Insert", 0);
		//final String obj = args[0];
		
		sc.addToDataDefinition(1, "PLANE LATITUDE", "DEGREES", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "PLANE LONGITUDE", "DEGREES", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "PLANE ALTITUDE", "METERS", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "PLANE PITCH DEGREES", "RADIANS", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "PLANE BANK DEGREES", "RADIANS", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "PLANE HEADING DEGREES TRUE", "RADIANS", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "AIRSPEED TRUE", "KNOTS", SimConnectDataType.FLOAT64);
		
		sc.mapClientEventToSimEvent(30, "SLEW_SET");
		
		sc.requestDataOnSimObject(1, 1, 0, SimConnectPeriod.ONCE);
		DispatcherTask dt = new DispatcherTask(sc);
		
		// just for esthetic purposes
		dt.addOpenHandler(new OpenHandler() {
			public void handleOpen(SimConnect sender, RecvOpen e) {
				System.out.println("Connected to " + e.getApplicationName());
			}});
		
		dt.addExceptionHandler(new ExceptionHandler() {

			public void handleException(SimConnect sender, RecvException e) {
				System.out.println("Error: " + e.getException());
				
			}});
		
		
		dt.addSimObjectDataHandler(new SimObjectDataHandler(){
			public void handleSimObject(SimConnect sender, RecvSimObjectData e) {
				if (e.getRequestID() == 1) {
					LatLonAlt lla = e.getLatLonAlt();
					double pit = e.getDataFloat64();
					double bnk = e.getDataFloat64();
					double hdg = e.getDataFloat64();
					double spd = e.getDataFloat64();
					
					InitPosition ip = new InitPosition();
					ip.setLatLonAlt(lla);
					ip.onGround = true;
					
					ip.pitch = Math.toDegrees(pit);
					ip.bank= Math.toDegrees(bnk);
					ip.heading = Math.toDegrees(hdg);
					ip.airspeed = (int) spd;
					
					try {
//						sender.aICreateNonATCAircraft(obj, "F-GAL", ip, 3);
						sender.aICreateSimulatedObject(objName, ip, 33);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		
		
		dt.addAssignedObjectHandler(new AssignedObjectHandler() {

			public void handleAssignedObject(SimConnect sender, RecvAssignedObjectID e) {
				System.out.println("Assigned 0x" + Integer.toHexString(e.getObjectID()));
				if (setSlew) {
					try {
						sender.transmitClientEvent(e.getObjectID(), 30, 1, 
								NotificationPriority.HIGHEST.ordinal(), 
								SimConnect.EVENT_FLAG_GROUPID_IS_PRIORITY);
					} catch (IOException e1) {
					}
				}
				
			}});
		
		
		// spawn receiver thread
		new Thread(dt).start();


	}

}
