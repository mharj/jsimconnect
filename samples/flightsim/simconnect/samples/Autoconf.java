package flightsim.simconnect.samples;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.config.Configuration;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvOpen;


/**
 * 
 * This example just demonstrates "automatic" configuration, without having to probe simconnect port.
 * 
 * @since 0.6
 */
public class Autoconf {

	public static void main(String[] args) throws Exception {
		OpenHandler handler = new OpenHandler(){
			public void handleOpen(SimConnect sender, RecvOpen e) {
				System.out.println("Connected to " + e.getApplicationName() + " (proto version " + e.getVersion() + ")");
				System.out.println("(Remote host: " + sender.remoteAddress() + ")");
				// bye bye
				try {
					sender.close();
				} catch (Exception ex) {}
			}
		};
		DispatcherTask dt = new DispatcherTask(null);
		dt.addOpenHandler(handler);
		
		// Totally automatic
		SimConnect sc = new SimConnect("Autoconf Hello");
		
		// we don't need to loop over events, since the first "Open" will disconnect
		sc.callDispatch(dt);
		
		// Another one, trying only ipV4
		sc = new SimConnect("Autoconf Hello", "localhost", Configuration.findSimConnectPortIPv4());
		sc.callDispatch(dt);

		// Another one, forcing IPv6 (may fail if not available)
		Configuration conf = new Configuration();
		conf.setPort(Configuration.findSimConnectPortIPv6());
		conf.setProtocol(6);
		sc = new SimConnect("Autoconf Hello", conf);
		sc.callDispatch(dt);

	}
}
