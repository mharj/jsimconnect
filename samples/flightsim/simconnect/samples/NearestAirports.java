package flightsim.simconnect.samples;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lc0277lib.geography.GeoCalc;

import flightsim.simconnect.*;
import flightsim.simconnect.recv.*;

public class NearestAirports implements OpenHandler, ExceptionHandler, EventHandler, FacilitiesListHandler, SimObjectDataHandler {
	
	private SimConnect sc;
	private DispatcherTask dt;
	
	private boolean inSim = false;
	
	private double userLat;
	private double userLon;
	
	public NearestAirports() throws IOException {
		sc = new SimConnect("NearestAirports", "10.1.0.6", 48447);
		
		sc.subscribeToSystemEvent(1, "Sim");
		sc.subscribeToSystemEvent(2, "1sec");
	
		sc.addToDataDefinition(1, "PLANE LATITUDE", "DEGREES", SimConnectDataType.FLOAT64);
		sc.addToDataDefinition(1, "PLANE LONGITUDE", "DEGREES", SimConnectDataType.FLOAT64);
		
		sc.requestDataOnSimObject(1, 1, 1, SimConnectPeriod.SIM_FRAME);
		
		dt = new DispatcherTask(sc);
		dt.addOpenHandler(this);
		dt.addExceptionHandler(this);
		dt.addEventHandler(this);
		dt.addFacilitiesListHandler(this);
		dt.addSimObjectDataHandler(this);
		
	}
	
	public void start(){
		dt.createThread().start();
	}
	
	public void handleEvent(SimConnect sender, RecvEvent e) {
		if (e.getEventID() == 1) {
			inSim = e.getData() == 1;
		} else if (e.getEventID() == 2 && inSim){
			try {
				sender.requestFacilitiesList(FacilityListType.AIRPORT, 3);
			} catch (IOException e1) {}
		}
	}
	private static DecimalFormat df = new DecimalFormat("###.0");
	
	class Airport implements Comparable<Airport> {
		String icao;
		double dist;
		double hdg;
		
		
		@Override
		public String toString() {
			return icao +" (" + df.format(dist/1000.0) + " nm)";
		}
		
		public int compareTo(Airport o) {
			return (int) Math.signum(dist - o.dist);
		}
	}
	
	public void handleAirportList(SimConnect sender, RecvAirportList list) {
		List<Airport> lap = new ArrayList<Airport>();
		
		for (FacilityAirport fa : list.getFacilities()) {
			
			if (userLat != 0 && userLon != 0) {
				double apLat = fa.getLatitude();
				double apLon = fa.getLongitude();
				double dist = GeoCalc.distance(userLat, userLon, apLat, apLon);
				double hdg = GeoCalc.heading(userLat, userLon, apLat, apLon);
				
				Airport ap = new Airport();
				ap.icao = fa.getIcao();
				ap.dist = dist;
				ap.hdg = hdg;
				lap.add(ap);
				
			}
		}
		if (lap.isEmpty()) return;
		Collections.sort(lap);
		int n = Math.min(6, lap.size());
		String [] array = new String[n];
		for (int i = 0; i < n; i++) {
			array[i] = lap.get(i).toString();
		}
		try {
			sender.menu(1.0f, 5, "Airports", "Nearest airports", array);
		} catch (IOException e) {
		}
		
	}
	
	public void handleSimObject(SimConnect sender, RecvSimObjectData e) {
		userLat = e.getDataFloat64();
		userLon = e.getDataFloat64();
	}
	
	public void handleNDBList(SimConnect sender, RecvNDBList list) {
	}
	public void handleVORList(SimConnect sender, RecvVORList list) {
	}
	public void handleWaypointList(SimConnect sender, RecvWaypointList list) {
	}
	
	
	public void handleException(SimConnect sender, RecvException e) {
		System.out.println("Exception : " +e.getException());
	}
	
	public void handleOpen(SimConnect sender, RecvOpen e) {
		System.out.println("Connected to " + e.getApplicationName());
	}
	
	public static void main(String[] args) throws Exception {
		NearestAirports nn = new NearestAirports();
		nn.start();
	}
}
