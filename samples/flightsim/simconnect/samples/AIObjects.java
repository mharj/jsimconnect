package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectConstants;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.data.InitPosition;
import flightsim.simconnect.data.Waypoint;
import flightsim.simconnect.recv.AssignedObjectHandler;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.EventHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvAssignedObjectID;
import flightsim.simconnect.recv.RecvEvent;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.wrappers.DataWrapper;

/**
 * This is the "AI Objects and Waypoints" Sample from the SDK
 * @author lc0277
 *
 */
public class AIObjects implements SimConnectConstants, EventHandler, OpenHandler, AssignedObjectHandler {

	private int ballonId = OBJECT_ID_USER;
	private int bellId = OBJECT_ID_USER;
	private int mooneyId = OBJECT_ID_USER;
	private int truckId = OBJECT_ID_USER;
	
	private enum EVENT_ID {
		SIM_START,
		Z,
		X,
		C,
		V
	}
	
	private enum DATA_REQUEST_ID {
		BALLOON,
		BELL,
		MOONEY,
		DOUGLAS,
		TRUCK,
		WHALE
	}
	
	private enum GROUP_ID {
		ZX
	}
	
	private enum INPUT_ID {
		ZX
	}
	
	private enum DEFINITION_ID {
		WAYPOINT,
		THROTTLE
	}
	

	//	 Set up flags so these operations only happen once
	private boolean plansSent = false;
	private boolean objectsCreated  = false;
	
	private float throttlePercent = 0;
	private DataWrapper throttleDataWrapper = new DataWrapper(4);	// size of float
	
	public AIObjects() throws IOException, ConfigurationNotFoundException {

		SimConnect sc = new SimConnect("AIObjects & Waypoints", 0);

		// Create some private events
		sc.mapClientEventToSimEvent(EVENT_ID.Z);
		sc.mapClientEventToSimEvent(EVENT_ID.X);
		sc.mapClientEventToSimEvent(EVENT_ID.C);
		sc.mapClientEventToSimEvent(EVENT_ID.V);

		// Link the private events to keyboard keys, and ensure the input events
		// are off
		sc.mapInputEventToClientEvent(INPUT_ID.ZX, "Z", EVENT_ID.Z);
		sc.mapInputEventToClientEvent(INPUT_ID.ZX, "X", EVENT_ID.X);
		sc.mapInputEventToClientEvent(INPUT_ID.ZX, "C", EVENT_ID.C);
		sc.mapInputEventToClientEvent(INPUT_ID.ZX, "V", EVENT_ID.V);
		sc.setInputGroupState(INPUT_ID.ZX, false);

		// Sign up for notifications
		sc.addClientEventToNotificationGroup(GROUP_ID.ZX, EVENT_ID.Z, false);
		sc.addClientEventToNotificationGroup(GROUP_ID.ZX, EVENT_ID.X, false);
		sc.addClientEventToNotificationGroup(GROUP_ID.ZX, EVENT_ID.C, false);
		sc.addClientEventToNotificationGroup(GROUP_ID.ZX, EVENT_ID.V, false);

		// Set up a definition for a waypoint list
		sc.addToDataDefinition(DEFINITION_ID.WAYPOINT, "AI Waypoint List",
				"number", SimConnectDataType.WAYPOINT);
		sc.addToDataDefinition(DEFINITION_ID.THROTTLE,
				"GENERAL ENG THROTTLE LEVER POSITION:1", "percent",
				SimConnectDataType.FLOAT32);

		// Request a simulation start event
		sc.subscribeToSystemEvent(EVENT_ID.SIM_START, "SimStart");

		// Set up standard dispatcher
		DispatcherTask dispatcher = new DispatcherTask(sc);
		dispatcher.addEventHandler(this);
		dispatcher.addAssignedObjectHandler(this);
		dispatcher.addOpenHandler(this);

		// Process all events in a loop. Exit on IO errors or remote
		// client exiting
		dispatcher.run();
	}

	private void setUpObjects(SimConnect sc) throws IOException {
		InitPosition Init = new InitPosition();

		// Add a parked museum aircraft, just west of the runway

		Init.altitude = 433.0; // Altitude of Sea-tac is 433 feet
		Init.latitude = 47 + (25.97 / 60); // Convert from 47 25.97 N
		Init.longitude = -122 - (18.51 / 60); // Convert from 122 18.51 W
		Init.pitch = 0.0;
		Init.bank = 0.0;
		Init.heading = 90.0;
		Init.onGround = true;
		Init.airspeed = 0;
		sc.aICreateSimulatedObject("Douglas DC-3", Init,
				DATA_REQUEST_ID.DOUGLAS);

		// Add a hot air balloon

		Init.altitude = 500.0; // Altitude of Sea-tac is 433 feet
		Init.latitude = 47 + (25.97 / 60); // Convert from 47 26.22 N
		Init.longitude = -122 - (18.45 / 60); // Convert from 122 18.45 W
		Init.pitch = 0.0;
		Init.bank = 0.0;
		Init.heading = 0.0;
		Init.onGround = false;
		Init.airspeed = 0;
		sc.aICreateSimulatedObject("Hot_Air_Balloon", Init,
				DATA_REQUEST_ID.BALLOON);

		// Add a helicopter

		Init.altitude = 433.0; // Altitude of Sea-tac is 433 feet
		Init.latitude = 47 + (26.22 / 60); // Convert from 47 26.22 N
		Init.longitude = -122 - (18.48 / 60); // Convert from 122 18.48 W
		Init.pitch = 0.0;
		Init.bank = 0.0;
		Init.heading = 0.0;
		Init.onGround = true;
		Init.airspeed = 100;

		// sc.aICreateNonATCAircraft("Bell 206B JetRanger", "H1000", Init,
		// DATA_REQUEST_ID.BELL.ordinal());

		// Initialize Mooney aircraft just in front of user aircraft
		// User aircraft is at 47 25.89 N, 122 18.48 W

		Init.altitude = 433.0; // Altitude of Sea-tac is 433 feet
		Init.latitude = 47 + (25.91 / 60); // Convert from 47 25.90 N
		Init.longitude = -122 - (18.48 / 60); // Convert from 122 18.48 W
		Init.pitch = 0.0;
		Init.bank = 0.0;
		Init.heading = 360.0;
		Init.onGround = true;
		Init.airspeed = 1;

		sc.aICreateNonATCAircraft("Mooney Bravo", "N1001", Init,
				DATA_REQUEST_ID.MOONEY);

		// Initialize truck just in front of user aircraft
		// User aircraft is at 47 25.89 N, 122 18.48 W

		Init.altitude = 433.0; // Altitude of Sea-tac is 433 feet
		Init.latitude = 47 + (25.91 / 60); // Convert from 47 25.90 N
		Init.longitude = -122 - (18.47 / 60); // Convert from 122 18.48 W
		Init.pitch = 0.0;
		Init.bank = 0.0;
		Init.heading = 360.0;
		Init.onGround = true;
		Init.airspeed = 0;

		sc.aICreateSimulatedObject("VEH_jetTruck", Init, DATA_REQUEST_ID.TRUCK);

		// Add a humpback whale

		Init.altitude = 433.0; // Altitude of Sea-tac is 433 feet
		Init.latitude = 47 + (25.89 / 60); // Convert from 47 25.89 N
		Init.longitude = -122 - (18.51 / 60); // Convert from 122 18.51 W
		Init.pitch = 0.0;
		Init.bank = 0.0;
		Init.heading = 0.0;
		Init.onGround = true;
		Init.airspeed = 0;
		sc.aICreateSimulatedObject("Humpbackwhale", Init,
						DATA_REQUEST_ID.WHALE);

	}

	private void sendFlightPlans(SimConnect sc) throws IOException {

        // Mooney aircraft should fly in circles across the North end of the
		// runway
		Waypoint[] wp = new Waypoint[3];
		for (int i = 0; i < wp.length; i++) wp[i] = new Waypoint();

        wp[0].flags             = WAYPOINT_SPEED_REQUESTED;
        wp[0].altitude  = 800;
        wp[0].latitude  = 47 + (27.79/60);      
        wp[0].longitude = -122 - (18.46/60);
        wp[0].speed  = 100;

        wp[1].flags             = WAYPOINT_SPEED_REQUESTED;
        wp[1].altitude  = 600;
        wp[1].latitude  = 47 + (27.79/60);      
        wp[1].longitude = -122 - (17.37/60);
        wp[1].speed  = 100;

        wp[2].flags             = WAYPOINT_WRAP_TO_FIRST | WAYPOINT_SPEED_REQUESTED;
        wp[2].altitude  = 800;
        wp[2].latitude  = 47 + (27.79/60);      
        wp[2].longitude = -122 - (19.92/60);
        wp[2].speed  = 100;
        
        sc.setDataOnSimObject(DEFINITION_ID.WAYPOINT, mooneyId, wp);

        // Truck goes down the runway
		Waypoint[] ft = new Waypoint[2];
		for (int i = 0; i < ft.length; i++) ft[i] = new Waypoint();
		
        ft[0].flags             = WAYPOINT_SPEED_REQUESTED;
        ft[0].altitude  = 433;
        ft[0].latitude  = 47 + (25.93/60);      
        ft[0].longitude = -122 - (18.46/60);
        ft[0].speed  = 75;

        ft[1].flags             = WAYPOINT_WRAP_TO_FIRST | WAYPOINT_SPEED_REQUESTED;
        ft[1].altitude  = 433;
        ft[1].latitude  = 47 + (26.25/60);      
        ft[1].longitude = -122 - (18.46/60);
        ft[1].speed  = 55;

        // Send the two waypoints to the truck
        sc.setDataOnSimObject(DEFINITION_ID.WAYPOINT, truckId, ft);


	}

	public void handleEvent(SimConnect sender, RecvEvent e) {
		int eventId = e.getEventID();
		
		if (eventId == EVENT_ID.SIM_START.ordinal()) {
			try {
				sender.setInputGroupState(INPUT_ID.ZX, true);
				System.out.println("Sim start");
			} catch (IOException e1) {}
		}
		
		else if (eventId == EVENT_ID.Z.ordinal()) {
			if (!objectsCreated) {
				try {
					setUpObjects(sender);
					objectsCreated = true;
				} catch (IOException ioe) {}
			}
		}

		else if (eventId == EVENT_ID.X.ordinal()) {
			if (!plansSent && objectsCreated) {
				try {
					sendFlightPlans(sender);
					plansSent = true;
				} catch (IOException e1) {}
			}
		}

		// give the balloon some throttle
		else if (eventId == EVENT_ID.C.ordinal()) {
			if (throttlePercent < 100.0f) {
				throttlePercent += 5.0f;
				throttleDataWrapper.reset();
				throttleDataWrapper.putFloat32(throttlePercent);
				try {
					sender.setDataOnSimObject(DEFINITION_ID.THROTTLE.ordinal(), OBJECT_ID_USER, false, 1, throttleDataWrapper);
				} catch (IOException e1) {}
			}
		}

		// give the balloon some throttle
		else if (eventId == EVENT_ID.V.ordinal()) {
			if (throttlePercent > 0.0f) {
				throttlePercent -= 5.0f;
				throttleDataWrapper.reset();
				throttleDataWrapper.putFloat32(throttlePercent);
				try {
					sender.setDataOnSimObject(DEFINITION_ID.THROTTLE.ordinal(), OBJECT_ID_USER, false, 1, throttleDataWrapper);
				} catch (IOException e1) {}
			}
		}

		
	}

	public void handleOpen(SimConnect sender, RecvOpen e) {
		System.out.println("Connected to : " + e.getApplicationName() + " " + 
				e.getApplicationVersionMajor() + "." + e.getApplicationVersionMinor());
	}

	private DATA_REQUEST_ID requestId(int i) {
		// we don't make checks. that's bad since we can receive some possibly
		// unknown id and it will throw an array exception
		//
		// the most correct usage is to add an "UNKNOWN" value to all enumerations
		// and fallback to this value for default switch blocks
		// and unknown ids
		//
		// if (i < 0 || i >= DATA_REQUEST_ID.values().length) return DATA_REQUEST_ID.UNKNOWN;
		//
		// another good usage is to place this method in the 
		// code of enumeration
		
		return DATA_REQUEST_ID.values()[i];
	}
	
	public void handleAssignedObject(SimConnect sender, RecvAssignedObjectID e) {
		
		DATA_REQUEST_ID req = requestId(e.getRequestID());
		switch (req) {
		case BALLOON:
			ballonId = e.getObjectID();
			System.out.println("Created balloon 1 id = " + ballonId);
			break;
		case BELL:
			bellId = e.getObjectID();
			System.out.println("Created Bell helicopter id = " + bellId);
			break;
		case MOONEY:
			mooneyId = e.getObjectID();
			System.out.println("Created Mooney bravo id = " + mooneyId);
			break;
		case DOUGLAS:
			System.out.println("Created stationary douglas DC3 id = " + e.getObjectID());
			break;
		case TRUCK:
			truckId = e.getObjectID();
			System.out.println("Created truck id = " + truckId);
			break;
		case WHALE:
			System.out.println("Created whake id = " + e.getObjectID());
			break;
        }

	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			@SuppressWarnings("unused")
			AIObjects aio = new AIObjects();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
