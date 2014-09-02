package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.NotificationPriority;
import flightsim.simconnect.SimConnect;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.EventHandler;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvEvent;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;

public class InputEvent implements EventHandler, OpenHandler {

	enum GROUP_ID {
		GROUP0;
		
		public boolean isEvent(RecvEvent re) {
			return ordinal() == re.getEventID();
		}
	};

	enum EVENT_ID {
		EVENT_BRAKES,
	};

	enum INPUT_ID {
		INPUT0,
	};

	private InputEvent() throws IOException, ConfigurationNotFoundException {

		SimConnect sc = new SimConnect("Input Event", 0);
		
		sc.mapClientEventToSimEvent(EVENT_ID.EVENT_BRAKES, "brakes");
		sc.addClientEventToNotificationGroup(GROUP_ID.GROUP0,
				EVENT_ID.EVENT_BRAKES);
		sc.setNotificationGroupPriority(GROUP_ID.GROUP0,
				NotificationPriority.HIGHEST);

		// Note that this does not override "." for brakes - both with be
		// transmitted

		sc.mapInputEventToClientEvent(INPUT_ID.INPUT0, "shift+ctrl+u",
				EVENT_ID.EVENT_BRAKES);
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
		if (e.getEventID() == EVENT_ID.EVENT_BRAKES.ordinal()) {
			System.out.println("Event brakes: " + e.getData());
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			@SuppressWarnings("unused")
			InputEvent sample = new InputEvent();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
