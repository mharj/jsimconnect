package flightsim.simconnect.samples;

import java.io.IOException;


import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.wrappers.DataWrapper;

public class SetVariable {

	public static void main(final String[] args) throws IOException, ConfigurationNotFoundException {
		if (args.length < 2) {
			System.err.println("Usage: SetVariable <variable name> <value> [units] [cid] ");
			System.err.println("Units may be NULL which means no units sent");
			System.err.println("CID is in hex form. 0 = user plane");
			System.exit(0);
		}
		
		SimConnect sc = new SimConnect("SetVariable", 0);
		int cid = 0;
		if (args.length > 2) {
			cid = Integer.parseInt(args[3], 16);
		}
		String units = args[2];
		if ("null".equalsIgnoreCase(units)) units = null;
		sc.addToDataDefinition(1, args[0],units, SimConnectDataType.FLOAT64);
		DataWrapper dw = new DataWrapper(8);
		dw.putFloat64(Double.parseDouble(args[1]));
		sc.setDataOnSimObject(1, cid, false, 1 , dw);
		
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
		while (true) {
			sc.callDispatch(dt);
		}
		
	}
}
