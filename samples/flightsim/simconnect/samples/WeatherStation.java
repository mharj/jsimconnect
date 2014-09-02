package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.SimConnectPeriod;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.data.LatLonAlt;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.EventHandler;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvEvent;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.recv.RecvSimObjectData;
import flightsim.simconnect.recv.RecvWeatherObservation;
import flightsim.simconnect.recv.SimObjectDataHandler;
import flightsim.simconnect.recv.WeatherObservationHandler;

public class WeatherStation implements SimObjectDataHandler, EventHandler,
		WeatherObservationHandler {

	enum EVENT_ID {
		EVENT_SIM_START,
	};

	enum DATA_DEFINE_ID7 {
		DEFINTION_LLA,
	};

	enum DATA_REQUEST_ID7 {
		REQUEST_LLA, REQUEST_WEATHER,
	};

	public WeatherStation() throws IOException, ConfigurationNotFoundException {
		SimConnect sc = new SimConnect("Request data", 0);

		sc.addToDataDefinition(DATA_DEFINE_ID7.DEFINTION_LLA,
				"STRUCTLATLONALT", null, SimConnectDataType.LATLONALT);

		// Request a flight loaded event
		sc.subscribeToSystemEvent(EVENT_ID.EVENT_SIM_START, "SimStart");

		// dispatcher
		DispatcherTask dt = new DispatcherTask(sc);
		dt.addEventHandler(this);
		dt.addSimObjectDataHandler(this);
		dt.addWeatherObservationHandler(this);
		dt.addOpenHandler(new OpenHandler() {

			public void handleOpen(SimConnect sender, RecvOpen e) {
				System.out.println("Connected to : " + e.getApplicationName()
						+ " " + e.getApplicationVersionMajor() + "."
						+ e.getApplicationVersionMinor());
			}
		});
		dt.addExceptionHandler(new ExceptionHandler() {
			public void handleException(SimConnect sender, RecvException e) {
				System.err
						.println("Exception: " + e.getException() + " packet "
								+ e.getSendID() + " index " + e.getIndex());
			}
		});
		dt.createThread().start();

	}

	public void handleEvent(SimConnect sender, RecvEvent e) {
		if (e.getEventID() == EVENT_ID.EVENT_SIM_START.ordinal()) {
			try {
				sender.requestDataOnSimObject(DATA_REQUEST_ID7.REQUEST_LLA,
						DATA_DEFINE_ID7.DEFINTION_LLA,
						SimConnect.OBJECT_ID_USER, SimConnectPeriod.SECOND, 0,
						0, 10, 0);
			} catch (IOException e1) {
			}
		}
	}

	public void handleSimObject(SimConnect sender, RecvSimObjectData e) {
		if (e.getRequestID() == DATA_REQUEST_ID7.REQUEST_LLA.ordinal()) {
			LatLonAlt lla = e.getLatLonAlt();

			System.out.println("Position: " + lla.toString());
			// Now request the weather data - this will also be requested every
			// 10 seconds
			try {
				sender.weatherRequestObservationAtNearestStation(
						DATA_REQUEST_ID7.REQUEST_WEATHER, (float) lla.latitude,
						(float) lla.longitude);
			} catch (IOException e1) {
			}
		}
	}

	public void handleWeatherObservation(SimConnect sender,
			RecvWeatherObservation e) {
		if (e.getRequestID() == DATA_REQUEST_ID7.REQUEST_WEATHER.ordinal()) {
			System.out.println("Metar : '" + e.getMetar() + "'");
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			@SuppressWarnings("unused")
			WeatherStation sample = new WeatherStation();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
