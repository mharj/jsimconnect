package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.EventHandler;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvEvent;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;

public class Autosave {

	private static final String FLT_NAME = "Autosaved";
	private static final int SAVE_DELAY = 60;
	
	public static void main(String[] args) throws Exception {
		System.out.println("Autosave (flight name '" + FLT_NAME + "' every " + SAVE_DELAY + " seconds)");
		
		final SimConnect sc = new SimConnect("AutoSave", 0);
		sc.subscribeToSystemEvent(1, "1sec");
		DispatcherTask dt = new DispatcherTask(sc);
		// just for esthetic purposes
		dt.addOpenHandler(new OpenHandler() {
			public void handleOpen(SimConnect sender, RecvOpen e) {
				System.out.println("Connected to " + e.getApplicationName());
			}});
		
		dt.addExceptionHandler(new ExceptionHandler(){
			public void handleException(SimConnect sender, RecvException e) {
				System.out.println("Error: " + e.getException());
			}
		});
		dt.addEventHandler(new EventHandler(){
			int nev = 0;
			public void handleEvent(SimConnect sender, RecvEvent e) {
				if ((nev % SAVE_DELAY) == 0) {
					System.out.println("Saving at " +  new java.util.Date().toString());
					try {
						sc.flightSave(FLT_NAME, FLT_NAME, 0);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				nev++;
			}
		});
		dt.createThread().start();
	}
}
