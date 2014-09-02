package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.*;
import flightsim.simconnect.recv.*;

/**
 * Display frame rate
 * @author lc0277
 *
 */
public class FrameRate {

	public static void main(String[] args) throws IOException {
		final int requestID = 1;
		// connect to simconnect
		SimConnect sc = new SimConnect("FrameRate", "10.1.0.6", 48447);
		
		// build data definition
		sc.subscribeToSystemEvent(requestID, "frame");
		
		DispatcherTask dt = new DispatcherTask(sc);
		
		dt.addEventFrameHandler(new EventFrameHandler() {
			public void handleEventFrame(SimConnect sender, RecvEventFrame event) {
				System.out.println("Frame rate " + event.getFrameRate() + " fps, speed " + event.getSimSpeed());
			}});
		
		dt.addQuitHandler(new QuitHandler() {
			public void handleQuit(SimConnect sender, RecvQuit e) {
				System.out.println("FS Exiting");
			}});
		// spawn receiver thread
		new Thread(dt).start();

		
	}

}
