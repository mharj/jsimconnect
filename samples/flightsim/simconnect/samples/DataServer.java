package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectConstants;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.EventHandler;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvEvent;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.wrappers.DataWrapper;

/**
 * Expose an area of data called "XData.area" that can be shared with other
 * clients. See DataClient
 * @author lc0277
 *
 */
public class DataServer {
	static final int SEC_EVENT = 1;
	static final int DATA_AREA_ID = 3;
	static final int DATA_AREA_DEF_ID = 1;
	
	public static void main(String[] args) throws IOException {
		SimConnect sc = new SimConnect("SendEventA", "10.1.0.6", 48447);
		
		sc.mapClientDataNameToID("XData.area", DATA_AREA_ID);
//		sc.createClientData(DATA_AREA_ID, 24, false);
		sc.addToClientDataDefinition(DATA_AREA_DEF_ID, 
				SimConnectConstants.CLIENTDATAOFFSET_AUTO,
				SimConnectConstants.CLIENT_DATA_TYPE_INT32);
		sc.addToClientDataDefinition(DATA_AREA_DEF_ID, 
				SimConnectConstants.CLIENTDATAOFFSET_AUTO,
				SimConnectConstants.CLIENT_DATA_TYPE_INT32);
		sc.subscribeToSystemEvent(SEC_EVENT, "1sec");
	
		DispatcherTask dt = new DispatcherTask(sc);
		dt.addEventHandler(new EventHandler() {
			public void handleEvent(SimConnect sender, RecvEvent e) {
				if (e.getEventID() == SEC_EVENT) {
					int random = (int) (Math.random() * 256);
					System.out.println("Sending random byte " + random);
					DataWrapper dw = new DataWrapper(2*4);
					dw.putInt32((int) (Math.random() * 420));
					dw.putInt32((int) (Math.random() * 300));
					try {
						sender.setClientData(
								DATA_AREA_ID, 		// client data area ID
								DATA_AREA_DEF_ID,	// definition ID
								dw);
					} catch (IOException e1) {}
				}
			}});
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
