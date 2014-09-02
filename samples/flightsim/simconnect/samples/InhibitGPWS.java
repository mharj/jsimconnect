package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.wrappers.DataWrapper;

public class InhibitGPWS {

	public static void main(String[] args) throws IOException {
		SimConnect sc = new SimConnect("InhibitGPWS", "10.1.0.6", 48447);
		
		sc.addToDataDefinition(1, "GPWS SYSTEM ACTIVE", "", SimConnectDataType.INT32);
		DataWrapper dw = new DataWrapper(4);
		dw.putInt32(0);
		sc.setDataOnSimObject(1, 1, false, 1,  dw);
		
		DispatcherTask dt = new DispatcherTask(sc);
		dt.addOpenHandler(new OpenHandler() {
			public void handleOpen(SimConnect sender, RecvOpen e) {
				System.out.println("Connected : " + e.toString());
			}});
		dt.addExceptionHandler(new ExceptionHandler() {
			public void handleException(SimConnect sender, RecvException e) {
				System.out.println("Error: " + e.getException() + " packet " + e.getSendID() + " arg "+ e.getIndex());
			}});
		new Thread(dt).start();
		
	}

}
