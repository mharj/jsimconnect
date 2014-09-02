package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.*;
import flightsim.simconnect.recv.*;

public class VideoRecord {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		
		// connect to simconnect
		SimConnect sc = new SimConnect("Video", "10.1.0.6", 48447);
		
		sc.mapClientEventToSimEvent(1, "VIDEO_RECORD_TOGGLE");
		sc.addClientEventToNotificationGroup(1, 1);
		sc.setNotificationGroupPriority(1, NotificationPriority.HIGHEST);
		sc.transmitClientEvent(SimConnectConstants.OBJECT_ID_USER, 1, 0, 1, 
				SimConnectConstants.EVENT_FLAG_DEFAULT);
		
		DispatcherTask dt = new DispatcherTask(sc);
		
		dt.addOpenHandler(new OpenHandler() {
			public void handleOpen(SimConnect sender, RecvOpen e) {
				System.out.println("Connected to " + e.getApplicationName());
			}});
		
		dt.addExceptionHandler(new ExceptionHandler() {

			public void handleException(SimConnect sender, RecvException e) {
				System.out.println("Error: " + e.getException());
			}});
		
		// just process next event
		sc.callDispatch(dt);


	}

}
