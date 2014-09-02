package flightsim.simconnect.samples;

import java.io.IOException;
import flightsim.simconnect.NotificationPriority;
import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectConstants;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.SimConnectPeriod;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.RecvSimObjectData;
import flightsim.simconnect.recv.SimObjectDataHandler;

public class TerrainFollow implements SimObjectDataHandler {

	private SimConnect sc;
	private DispatcherTask dt;
	private int level;
	
	public TerrainFollow(int simcoNumber, int level) throws IOException, ConfigurationNotFoundException {
		this.sc = new SimConnect("TerrainFollow", simcoNumber);
		this.level = level;
		
		this.dt = new DispatcherTask(sc);
		dt.addSimObjectDataHandler(this);
		
		sc.addToDataDefinition(1, "GROUND ALTITUDE", "FEET", SimConnectDataType.FLOAT64);
		sc.mapClientEventToSimEvent(1, "AP_ALT_VAR_SET_ENGLISH");
		
		sc.requestDataOnSimObject(1, 1, 0, SimConnectPeriod.SIM_FRAME);

	}
	
	public void start() {
		dt.createThread().start();
	}
	
	private int last = 0;
	
	public void handleSimObject(SimConnect sender, RecvSimObjectData e) {
		double radioHeight = e.getDataFloat64();
		int apVal = (int) ((radioHeight + level) / 50) * 50;
		if (apVal != last || last == 0) {
			try {
				sc.transmitClientEvent(0, 1, apVal, 
						NotificationPriority.HIGHEST.ordinal(), 
						SimConnectConstants.EVENT_FLAG_GROUPID_IS_PRIORITY);
			} catch (IOException e1) {
			}
			last = apVal;
			System.out.println("Setting autopilot to " + apVal);
		}
		
	}
	
	
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: TerrainFollow <alt> [cfg index]");
			System.err.println("Alt is in feet over ground");
			System.exit(0);
		}
		
		int alt = Integer.parseInt(args[0]);
		int simco = (args.length > 1) ? Integer.parseInt(args[1]) : 0;
		
		TerrainFollow tf = new TerrainFollow(simco, alt);
		tf.start();
	}
}
