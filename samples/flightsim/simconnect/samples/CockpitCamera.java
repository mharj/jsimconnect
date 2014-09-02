package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.NotificationPriority;
import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectConstants;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.EventHandler;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvEvent;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.samples.InputEvent.INPUT_ID;

public class CockpitCamera implements OpenHandler, EventHandler {

	static enum GROUP_ID {
		GROUP0,
	};

	static enum EVENT_ID {
		EVENT_CAMERA_RIGHT, EVENT_CAMERA_LEFT,
	};

	private float cameraBank = 0.0f;

	private float normalize180(float v) {
		while (v < -180.0f)
			v += 360.0f;
		while (v > 180.0f)
			v -= 360.0f;
		return v;
	}

	CockpitCamera() throws IOException, ConfigurationNotFoundException {
		SimConnect sc = new SimConnect("Cockpit camera", 0);

		// Define private events
		sc.mapClientEventToSimEvent(EVENT_ID.EVENT_CAMERA_LEFT);
		sc.mapClientEventToSimEvent(EVENT_ID.EVENT_CAMERA_RIGHT);

		sc.addClientEventToNotificationGroup(GROUP_ID.GROUP0,
				EVENT_ID.EVENT_CAMERA_LEFT);
		sc.addClientEventToNotificationGroup(GROUP_ID.GROUP0,
				EVENT_ID.EVENT_CAMERA_RIGHT);

		sc.setNotificationGroupPriority(GROUP_ID.GROUP0,
				NotificationPriority.HIGHEST);

		// Map the keys , and . keys to the private events
		sc.mapInputEventToClientEvent(INPUT_ID.INPUT0, "VK_PERIOD",
				EVENT_ID.EVENT_CAMERA_RIGHT);
		sc.mapInputEventToClientEvent(INPUT_ID.INPUT0, "VK_COMMA",
				EVENT_ID.EVENT_CAMERA_LEFT);

		sc.setInputGroupState(INPUT_ID.INPUT0, true);

		// dispatcher
		DispatcherTask dt = new DispatcherTask(sc);
		dt.addOpenHandler(this);
		dt.addEventHandler(this);
		dt.addExceptionHandler(new ExceptionHandler() {
			public void handleException(SimConnect sender, RecvException e) {
				System.err
						.println("Exception: " + e.getException() + " packet "
								+ e.getSendID() + " index " + e.getIndex());
			}
		});
		dt.createThread().start();
	}

	public void handleOpen(SimConnect sender, RecvOpen e) {
		System.out.println("Connected to : " + e.getApplicationName() + " "
				+ e.getApplicationVersionMajor() + "."
				+ e.getApplicationVersionMinor());
	}

	public void handleEvent(SimConnect sender, RecvEvent e) {
		try {
			if (e.getEventID() == EVENT_ID.EVENT_CAMERA_RIGHT.ordinal()) {
				cameraBank = normalize180(cameraBank + 5.0f);
				sender.cameraSetRelative6DOF(0.0f, 0.0f, 0.0f,
						SimConnectConstants.CAMERA_IGNORE_FIELD, cameraBank,
						SimConnectConstants.CAMERA_IGNORE_FIELD);

			} else if (e.getEventID() == EVENT_ID.EVENT_CAMERA_LEFT.ordinal()) {
				cameraBank = normalize180(cameraBank - 5.0f);
				sender.cameraSetRelative6DOF(0.0f, 0.0f, 0.0f,
						SimConnectConstants.CAMERA_IGNORE_FIELD, cameraBank,
						SimConnectConstants.CAMERA_IGNORE_FIELD);
			}
		} catch (Exception ex) {
			// ignore
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			@SuppressWarnings("unused")
			CockpitCamera sample = new CockpitCamera();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
