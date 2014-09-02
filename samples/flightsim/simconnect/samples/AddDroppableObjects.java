package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectConstants;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.wrappers.DataWrapper;

public class AddDroppableObjects {

	/**
	 * @param args
	 * @throws ConfigurationNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ConfigurationNotFoundException {
		
		if (args.length < 1) {
			System.err.println("Usage: AddDroppable <type> [count] [objectIdHEX]");
			return;
		}
		
		int count=4;
		if (args.length >= 2) {
			try { count = Integer.parseInt(args[1]); } catch (Exception e){}
		}
		
		int objid = SimConnectConstants.OBJECT_ID_USER;
		if (args.length >= 3) {
			objid = Integer.parseInt(args[2], 16);
		}
		
		// connect to simconnect
		SimConnect sc = new SimConnect("AddDroppable", 0);
		sc.addToDataDefinition(5, "DROPPABLE OBJECTS TYPE:" + count, null, SimConnectDataType.STRING128);
		
		DataWrapper dw = new DataWrapper(128);
		dw.putString128(args[0]);
		sc.setDataOnSimObject(5, objid, false, 1, dw);
		
		sc.getNextData();
		sc.getNextData();
	}

}
