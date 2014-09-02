package flightsim.simconnect.samples;

import java.io.IOException;


import flightsim.simconnect.NotificationPriority;
import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectConstants;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;

public class SendEvent {

	public static void main(String[] args) throws IOException, ConfigurationNotFoundException {
		if (args.length < 1) {
			System.err.println("Usage: SendEvent <event name> [cid] [param]");
			System.err.println("Cid is in hex form");
			System.exit(0);
		}
		
		SimConnect sc = new SimConnect("SendEvent", 0);
		
		sc.mapClientEventToSimEvent(1, args[0]);
		int cid = 0;
		int param = 0;
		if (args.length > 1) {
			cid = Integer.parseInt(args[1], 16);
		}
		if (args.length > 2) {
			param = Integer.parseInt(args[2]);
		}
		System.out.println("Sending to " + Integer.toHexString(cid));
		sc.transmitClientEvent(cid, 1, param, NotificationPriority.HIGHEST.ordinal(), SimConnectConstants.EVENT_FLAG_GROUPID_IS_PRIORITY);
		
		DispatcherTask dt = new DispatcherTask(sc);
		dt.addOpenHandler(new OpenHandler(){
			public void handleOpen(SimConnect sender, RecvOpen e) {
				System.out.println("Connected");
				
			}
		});
		dt.addExceptionHandler(new ExceptionHandler(){
			public void handleException(SimConnect sender, RecvException e) {
				System.out.println("Exception (" + e.getException() +") packet " + e.getSendID());
			}
		});
		while (true) {
			sc.callDispatch(dt);
		}
		
	}
}
