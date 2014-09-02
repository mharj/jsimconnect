package flightsim.simconnect.samples;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.EventFrameHandler;
import flightsim.simconnect.recv.EventHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvEvent;
import flightsim.simconnect.recv.RecvEventFrame;
import flightsim.simconnect.recv.RecvOpen;

public class SystemEvents  {

	static enum EVENT_ID {
		EVENT_ID_FRAME,
		EVENT_ID_SIMSTART,
		EVENT_ID_SIMSTOP,
	}
	
	public static void main(String[] args) {
		try {
			SimConnect sc = new SimConnect("SystemEvents", "10.1.0.6", 48447);
			sc.subscribeToSystemEvent(EVENT_ID.EVENT_ID_FRAME, "Frame");
			sc.subscribeToSystemEvent(EVENT_ID.EVENT_ID_SIMSTART, "SimStart");
			sc.subscribeToSystemEvent(EVENT_ID.EVENT_ID_SIMSTOP, "SimStop");
			DispatcherTask dt = new DispatcherTask(sc);
			dt.addEventHandler(new EventHandler(){
				public void handleEvent(SimConnect sender, RecvEvent e) {
					if (e.getEventID() == EVENT_ID.EVENT_ID_SIMSTART.ordinal())
						System.out.println("Simulation started");
					
					if (e.getEventID() == EVENT_ID.EVENT_ID_SIMSTOP.ordinal())
						System.out.println("Simulation stopped");
				}
			});
			dt.addEventFrameHandler(new EventFrameHandler(){
				public void handleEventFrame(SimConnect sender, RecvEventFrame event) {
					System.out.println("Frame " + event.getFrameRate() +" fps at " + event.getSimSpeed() + "X");
				}
			});
			dt.addOpenHandler(new OpenHandler(){
				public void handleOpen(SimConnect sender, RecvOpen e) {
					System.out.println("Connected to FS");
					
				}
			});
			while (true) {
				sc.callDispatch(dt);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
