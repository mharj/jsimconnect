package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.recv.ClientDataHandler;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.EventHandler;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvClientData;
import flightsim.simconnect.recv.RecvEvent;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;

/**
 * Every second, read the content of the area "XData.area"
 * @author lc0277
 *
 */
public class DataClient {

	static final int SEC_EVENT = 1;
	static final int DATA_AREA_ID = 1;
	static final int DATA_AREA_REQ_ID = 3;
	static final int DATA_AREA_DEF_ID = 2;
	
	public static void main(String[] args) throws IOException {
		SimConnect sc = new SimConnect("SendEventA", "10.1.0.6", 48447);
		
		sc.mapClientDataNameToID("XData.area", DATA_AREA_ID);
		sc.addToClientDataDefinition(DATA_AREA_DEF_ID, 
				0,		// offset
				4		// size
				);
		sc.addToClientDataDefinition(DATA_AREA_DEF_ID, 
				4,		// offset
				4		// size
				);
		DispatcherTask dt = new DispatcherTask(sc);
		sc.subscribeToSystemEvent(SEC_EVENT, "1sec");
		
		dt.addEventHandler(new EventHandler() {
			public void handleEvent(SimConnect sender, RecvEvent e) {
				if (e.getEventID() == SEC_EVENT) {
					try {
						sender.requestClientData(DATA_AREA_ID, DATA_AREA_REQ_ID, DATA_AREA_DEF_ID);
					} catch (IOException e1) {}
				}
			}});
		dt.addOpenHandler(new OpenHandler() {
			public void handleOpen(SimConnect sender, RecvOpen e) {
				System.out.println("Connected : " + e.toString());
			}});
		dt.addClientDataHandler(new ClientDataHandler() {
			public void handleClientData(SimConnect sender, RecvClientData e) {
				System.out.println("Recv data: " + e.getDataInt32() + " req " + e.getRequestID() + " cli " + e.getDefineID() + " tot " + e.getDefineCount());
			}});
		dt.addExceptionHandler(new ExceptionHandler() {
			public void handleException(SimConnect sender, RecvException e) {
				System.out.println("Error: " + e.getException() + " packet " + e.getSendID() + " arg "+ e.getIndex());
			}});
		new Thread(dt).start();
	}

}
