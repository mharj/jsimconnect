package flightsim.simconnect.samples.annotations;

import java.io.IOException;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectConstants;
import flightsim.simconnect.SimConnectPeriod;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.recv.RecvSimObjectData;
import flightsim.simconnect.recv.SimObjectDataHandler;
import flightsim.simconnect.wrappers.AnnotationWrapper;
import flightsim.simconnect.wrappers.IllegalDataDefinition;

public class DataRequest2 implements ExceptionHandler, OpenHandler, SimObjectDataHandler {

	private DispatcherTask dt;
	private SimConnect sc;
	
	private AnnotationWrapper anWrapper;
	
	public DataRequest2() throws IOException, ConfigurationNotFoundException, IllegalDataDefinition {
		sc = new SimConnect("DataRequest", 0);
		
		//
		// create annotation wrapper for this simconnect session
		anWrapper = new AnnotationWrapper(sc);
		// register the SampleDataClass and build a request for the user
		// aircraft, every seconds
		anWrapper.registerClass(SampleDataClass2.class);
		anWrapper.requestSimObjectData(SampleDataClass2.class, 
				SimConnectConstants.OBJECT_ID_USER,
				SimConnectPeriod.SECOND,
				false);
		
		dt = new DispatcherTask(sc);
		dt.addHandlers(this);	// will register all implemented handlers
	}
	
	
	public void handleException(SimConnect sender, RecvException e) {
		System.out.println("Error: " + e.getException() +" packet " +e.getSendID());
	}
	
	public void handleOpen(SimConnect sender, RecvOpen e) {
		System.out.println("Connected to " + e.getApplicationName());
	}
	
	public void handleSimObject(SimConnect sender, RecvSimObjectData e) {
		SampleDataClass2 data = (SampleDataClass2) anWrapper.unwrap(e);
		System.out.println("Got data: " +data);
		
		//
		// demonstrate how to use the setSimObjectData method. just change the atc id
		// and resend the packet
		data.setAtcID(randomString(4));
		try {
			anWrapper.setSimObjectData(data, SimConnectConstants.OBJECT_ID_USER);
		} catch (IOException e1) {
		} catch (IllegalDataDefinition e2) {
			// a more elegant exception management should be done here
			e2.printStackTrace();
		}
	}
	
	private String randomString(int len) {
		char[] garbageString = new char[len];
		for (int i = 0; i < len; i++) {
			garbageString[i] = (char) ('A' + (int)( Math.random() * 25));
		}
		return new String(garbageString);
	}
	
	
	public static void main(String[] args) throws IOException, ConfigurationNotFoundException, IllegalDataDefinition {
		DataRequest2 app = new DataRequest2();
		app.dt.run();	// don't need to spawn a thread, unless you had something else to do
		
	}
}
