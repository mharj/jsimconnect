package flightsim.simconnect.samples.annotations;

import flightsim.simconnect.wrappers.FlightSimData;

public class SampleDataClass2 {

	@FlightSimData(variable = "ATC FLIGHT NUMBER")
	String atcID;
	
	
	public String getAtcID() {
		return atcID;
	}
	
	public void setAtcID(String atcID) {
		this.atcID = atcID;
	}
	
	@Override
	public String toString() {
		return "ATC ID: '" + atcID +"'";
	}
}
