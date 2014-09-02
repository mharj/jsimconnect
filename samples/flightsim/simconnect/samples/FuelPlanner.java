package flightsim.simconnect.samples;

import java.text.DecimalFormat;
import java.text.Format;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.SimConnectPeriod;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvSimObjectData;
import flightsim.simconnect.recv.SimObjectDataHandler;

public class FuelPlanner {

	private static final Format F = new DecimalFormat("##.0#");
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		SimConnect sc = new SimConnect("FuelPlanner", "10.1.0.6", 48447);
		System.out.println("Please wait before I calculate");
		sc.addToDataDefinition(1, "GROUND VELOCITY", "KNOTS", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "FUEL TOTAL QUANTITY WEIGHT", "POUNDS", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "TURB ENG FUEL FLOW PPH:0", "POUNDS PER HOUR", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "TURB ENG FUEL FLOW PPH:1", "POUNDS PER HOUR", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "TURB ENG FUEL FLOW PPH:2", "POUNDS PER HOUR", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "TURB ENG FUEL FLOW PPH:3", "POUNDS PER HOUR", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "TURB ENG FUEL FLOW PPH:4", "POUNDS PER HOUR", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "TURB ENG FUEL FLOW PPH:5", "POUNDS PER HOUR", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "TURB ENG FUEL FLOW PPH:6", "POUNDS PER HOUR", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "TURB ENG FUEL FLOW PPH:7", "POUNDS PER HOUR", SimConnectDataType.FLOAT64);
		
		sc.requestDataOnSimObject(1, 1, 0, SimConnectPeriod.SECOND);
		
		DispatcherTask dt = new DispatcherTask(sc);
		
		dt.addSimObjectDataHandler(new SimObjectDataHandler(){
			public void handleSimObject(SimConnect sender, RecvSimObjectData e) {
				double gs = e.getDataFloat64();
				double qty = e.getDataFloat64();
				double ff = 0;
				
				while (e.hasRemaining()) {
					ff += e.getDataFloat64();
				}
				
				double rem = qty/ff;
				double dist = gs * rem;
				// efficiencey: nm with 100 lbs
				double eff = 100 * gs / (ff);
				
				System.out.println("Remaining: " + F.format(qty) + " lbs, " +  
						F.format(rem) + " h, " + 
						F.format(dist) + " nm, " +
						F.format(eff) + " eff (nm / 100 lbs) " +
						F.format(ff) + " lbs/h");
				
			}
		});
		dt.addExceptionHandler(new ExceptionHandler(){
			public void handleException(SimConnect sender, RecvException e) {
				System.out.println("Error: " + e.getException());
				
			}
		});
		dt.createThread().start();
	}

}
