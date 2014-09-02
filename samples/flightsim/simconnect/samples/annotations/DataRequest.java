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

/**
 * Sample data request class
 * @author lc
 *
 */
public class DataRequest implements ExceptionHandler, OpenHandler, SimObjectDataHandler {

	private DispatcherTask dt;
	private SimConnect sc;
	
	private AnnotationWrapper anWrapper;
	
	public DataRequest() throws IOException, ConfigurationNotFoundException, IllegalDataDefinition {
		sc = new SimConnect("DataRequest", 0);
		
		//
		// create annotation wrapper for this simconnect session
		anWrapper = new AnnotationWrapper(sc);
		// register the SampleDataClass and build a request for the user
		// aircraft, every seconds
		anWrapper.registerClass(SampleDataClass.class);
		anWrapper.requestSimObjectData(SampleDataClass.class, 
				SimConnectConstants.OBJECT_ID_USER,
				SimConnectPeriod.SECOND,
				false);
		
		dt = new DispatcherTask(sc);
		dt.addHandlers(this);	// will register all implemented handlers
	}
	
	/**
	 * Esthetic
	 */
	public void handleException(SimConnect sender, RecvException e) {
		System.out.println("Error: " + e.getException() +" packet " +e.getSendID());
	}
	
	/**
	 * Esthetic
	 */
	public void handleOpen(SimConnect sender, RecvOpen e) {
		System.out.println("Connected to " + e.getApplicationName());
	}
	
	public void handleSimObject(SimConnect sender, RecvSimObjectData e) {
		// build an object from the received packet
		SampleDataClass data = (SampleDataClass) anWrapper.unwrap(e);
		System.out.println("Got data: " +data);
	}
	
	
	public static void main(String[] args) throws IOException, ConfigurationNotFoundException, IllegalDataDefinition {
		DataRequest app = new DataRequest();
		app.dt.run();	// don't need to spawn a thread, unless you had something else to do
		
	}
}
