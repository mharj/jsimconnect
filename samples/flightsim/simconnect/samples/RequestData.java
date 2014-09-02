package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.SimObjectType;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.EventHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvEvent;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.recv.RecvSimObjectDataByType;
import flightsim.simconnect.recv.SimObjectDataTypeHandler;

public class RequestData implements EventHandler, OpenHandler,
		SimObjectDataTypeHandler {

	static enum EVENT_ID {
		EVENT_SIM_START,
	};

	static enum DATA_DEFINE_ID {
		DEFINITION_1,
	};

	static enum DATA_REQUEST_ID {
		REQUEST_1,
	};

	public RequestData() throws IOException, ConfigurationNotFoundException {
		SimConnect sc = new SimConnect("Request data", 0);

		// Set up the data definition, but do not yet do anything with it
		sc.addToDataDefinition(DATA_DEFINE_ID.DEFINITION_1, "Title", null,
				SimConnectDataType.STRING256);
		sc.addToDataDefinition(DATA_DEFINE_ID.DEFINITION_1, "Plane Latitude",
				"degrees", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(DATA_DEFINE_ID.DEFINITION_1, "Plane Longitude",
				"degrees", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(DATA_DEFINE_ID.DEFINITION_1, "Plane Altitude",
				"feet", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(DATA_DEFINE_ID.DEFINITION_1,
				"Kohlsman setting hg", "inHg", SimConnectDataType.FLOAT64);

		// Request an event when the simulation starts
		sc.subscribeToSystemEvent(EVENT_ID.EVENT_SIM_START, "SimStart");

		// dispatcher
		DispatcherTask dt = new DispatcherTask(sc);
		dt.addOpenHandler(this);
		dt.addEventHandler(this);
		dt.addSimObjectDataTypeHandler(this);
		dt.createThread().start();

	}

	public void handleOpen(SimConnect sender, RecvOpen e) {
		System.out.println("Connected to : " + e.getApplicationName() + " "
				+ e.getApplicationVersionMajor() + "."
				+ e.getApplicationVersionMinor());
	}

	public void handleEvent(SimConnect sender, RecvEvent e) {

		// Now the sim is running, request information on the user aircraft
		try {
			sender.requestDataOnSimObjectType(DATA_REQUEST_ID.REQUEST_1,
					DATA_DEFINE_ID.DEFINITION_1, 0, SimObjectType.USER);
		} catch (IOException e1) {
		}
	}

	public void handleSimObjectType(SimConnect sender, RecvSimObjectDataByType e) {
		if (e.getRequestID() == DATA_REQUEST_ID.REQUEST_1.ordinal()) {
			//
			// notice that we cannot cast directly a RecvSimObjectDataByType 
			// to a structure. this is forbidden by java language
			//
			
			System.out.println("ObjectID=" + e.getObjectID() + " Title='"
					+ e.getDataString256() + "'");
			System.out.println("Lat=" + e.getDataFloat64() + " Lon="
					+ e.getDataFloat64() + " Alt=" + e.getDataFloat64()
					+ " Kohlsman=" + e.getDataFloat64());
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			@SuppressWarnings("unused")
			RequestData sample = new RequestData();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
