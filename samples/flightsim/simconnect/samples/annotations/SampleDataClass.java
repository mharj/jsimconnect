package flightsim.simconnect.samples.annotations;

import flightsim.simconnect.wrappers.FlightSimData;

/**
 * Sample data class demonstrating use of annotations to request/send 
 * flight simulator variable 
 * @author lc
 *
 */
public class SampleDataClass {

	/**
	 * Like beans, the sample data class must have a public constructor, needed to build
	 * objects if necessary.
	 *
	 */
	public SampleDataClass() {
		
	}
	
	@FlightSimData(variable = "PLANE LATITUDE", units = "DEGREES")
	private double latitude;
	@FlightSimData(variable = "PLANE LONGITUDE", units = "DEGREES")
	private double longitude;
	
	@FlightSimData(variable = "AUTOPILOT MASTER")
	private boolean autopilot;
	
	private int noFsData;
	
	public void setNoFsData(int noFsData) {
		this.noFsData = noFsData;
	}
	
	public int getNoFsData() {
		return noFsData;
	}
	
	
	@Override
	public String toString() {
		return "latitude: " + latitude + ", longitude: " + longitude +
				", autopilot: " + autopilot + " (NOT FS: " +
				noFsData + ")";
	}
}
