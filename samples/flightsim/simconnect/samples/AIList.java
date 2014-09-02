package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.SimObjectType;
import flightsim.simconnect.data.LatLonAlt;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.EventHandler;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvEvent;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.recv.RecvSimObjectDataByType;
import flightsim.simconnect.recv.SimObjectDataTypeHandler;

/**
 * List of active ai planes
 * @author lc0277
 *
 */
public class AIList {

	public static void main(String[] args) throws IOException {
		final int dataDefID = 1;
		final int requestID = 1;
		final int fourSeceventID = 1;
		// connect to simconnect
		SimConnect sc = new SimConnect("AIList", "10.1.0.6", 48447);
		
		// build data definition
		sc.addToDataDefinition(dataDefID, "STRUCT LATLONALT", null, SimConnectDataType.LATLONALT);
		sc.addToDataDefinition(dataDefID, "ATC TYPE", null, SimConnectDataType.STRING32);
		sc.addToDataDefinition(dataDefID, "TITLE", null, SimConnectDataType.STRING32);
		sc.addToDataDefinition(dataDefID, "AI TRAFFIC FROMAIRPORT", null, SimConnectDataType.STRING8);
		sc.addToDataDefinition(dataDefID, "AI TRAFFIC TOAIRPORT", null, SimConnectDataType.STRING8);
		
		// get warned every 4 seconds when in sim mode
		sc.subscribeToSystemEvent(fourSeceventID, "1sec");
		
		DispatcherTask dt = new DispatcherTask(sc);
		
		// just for esthetic purposes
		dt.addOpenHandler(new OpenHandler() {
			public void handleOpen(SimConnect sender, RecvOpen e) {
				System.out.println("Connected to " + e.getApplicationName());
			}});
		
		// add an event handler to receive events every 4 seconds
		dt.addEventHandler(new EventHandler() {
			public void handleEvent(SimConnect sender, RecvEvent e) {
				if (e.getEventID() == fourSeceventID) {
					// request data for all aircrafts in the sim
					try {
						sender.requestDataOnSimObjectType(requestID, dataDefID, 199*1000, SimObjectType.AIRCRAFT);
					} catch (IOException ioe) {}
				}
			}});
		
		// handler called when received data requested by the call to
		// requestDataOnSimObjectType
		dt.addSimObjectDataTypeHandler(new SimObjectDataTypeHandler() {
			private LatLonAlt userPosition;
			public void handleSimObjectType(SimConnect sender, RecvSimObjectDataByType e) {
				// retrieve data structures from packet
				LatLonAlt position = e.getLatLonAlt();
				String atcType = e.getDataString32();
				String atcID = e.getDataString32();
				String from = e.getDataString8();
				String to = e.getDataString8();
				
				// memorize userposition for later
				if (e.getObjectID() == 1) {
					userPosition = position;
				}
				
				// print to users
				System.out.println("Plane id#" + Integer.toHexString(e.getObjectID()) + " no " + e.getEntryNumber() + "/" + e.getOutOf());
				System.out.println("\tPosition: " + position.toString());
				System.out.println("\tType/ID: " +  atcType + " " + atcID);
				if (e.getObjectID() != 1 && userPosition != null) {
					System.out.println("\tDistance: " + distance(position.latitude, position.longitude, userPosition.latitude, userPosition.longitude));
				}
				if (e.getObjectID() != 1) {
					System.out.println("\tPlan: " + from + " -> " +to);
				}
				// line separator
				if (e.getEntryNumber() == e.getOutOf()) System.out.println();	
			}});
		
		dt.addExceptionHandler(new ExceptionHandler(){
			public void handleException(SimConnect sender, RecvException e) {
				System.out.println("Error: " + e.getException());
				
			}
		});
		// spawn receiver thread
		new Thread(dt).start();

		
	}

	public static final double RADIUS_EARTH_M	=	6378137;

	/**
	 * Distance in METERS between points with lat/lon in DEGREES
	 * @param lat1
	 * @param lon1
	 * @param lat2
	 * @param lon2
	 * @return
	 */
    public static double distance(double lat1, double lon1, double lat2, double lon2) {
    	lat1 = Math.toRadians(lat1);
    	lat2 = Math.toRadians(lat2);
    	lon1 = Math.toRadians(lon1);
    	lon2 = Math.toRadians(lon2);
    	
    	return RADIUS_EARTH_M * 
    			Math.acos(Math.cos(lat1) * 	Math.cos(lat2) * Math.cos(lon1-lon2) + 
    					Math.sin(lat1)*Math.sin(lat2));
    }


}
