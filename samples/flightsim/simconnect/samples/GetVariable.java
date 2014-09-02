package flightsim.simconnect.samples;

import java.io.IOException;


import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.SimConnectPeriod;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.recv.RecvSimObjectData;
import flightsim.simconnect.recv.SimObjectDataHandler;

public class GetVariable {

	public static void main(final String[] args) throws IOException, ConfigurationNotFoundException {
		if (args.length < 1) {
			System.err.println("Usage: GetVariable <variable name> [units] [cid] [repeat]");
			System.err.println("Units may be NULL which means no units sent");
			System.err.println("CID is in hex form. 0 = user plane");
			System.err.println("Repeat type may be SECOND, ONCE (default), SIM_FRAME, etc");
			System.exit(0);
		}
		
		SimConnect sc = new SimConnect("GetVariable", 0);
		int cid = 0;
		if (args.length > 2) {
			cid = Integer.parseInt(args[2], 16);
		}
		SimConnectPeriod p = SimConnectPeriod.ONCE;
		if (args.length > 3) {
			p = SimConnectPeriod.valueOf(args[3].toUpperCase());
		}
		String units = null;
		if (args.length > 1) units = args[1];
		if ("null".equalsIgnoreCase(units)) units = null;
		sc.addToDataDefinition(1, args[0],units, SimConnectDataType.FLOAT64);
		sc.requestDataOnSimObject(1, 1, cid, p);
		
		DispatcherTask dt = new DispatcherTask(sc);
		dt.addOpenHandler(new OpenHandler(){
			public void handleOpen(SimConnect sender, RecvOpen e) {
				System.out.println("Connected to " + e.getApplicationName());
			}
		});
		dt.addExceptionHandler(new ExceptionHandler(){
			public void handleException(SimConnect sender, RecvException e) {
				System.out.println("Exception (" + e.getException() +") packet " + e.getSendID());
			}
		});
		dt.addSimObjectDataHandler(new SimObjectDataHandler(){
			public void handleSimObject(SimConnect sender, RecvSimObjectData e) {
				System.out.println("Value of '" + args[0] + "' = " + e.getDataFloat64());
				
			}
		});
		while (true) {
			sc.callDispatch(dt);
		}
		
	}
}
