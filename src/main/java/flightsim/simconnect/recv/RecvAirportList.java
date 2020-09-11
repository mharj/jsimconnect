package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

/**
 * The <code>RecvAirportList</code> structure is used to return a list of {@link FacilityAirport} structures.
 * @since 0.5
 * @author lc0277
 *
 */
public class RecvAirportList extends RecvFacilitiesList {
	private FacilityAirport[] airports;
	
	RecvAirportList(ByteBuffer bf) {
		super(bf, RecvID.ID_AIRPORT_LIST);
		
		// parse all airports
		int size = getArraySize();
		airports = new FacilityAirport[size];
		
		for (int i = 0; i < size; i++) {
			airports[i] = new FacilityAirport(bf);
		}
	}

	/**
	 * Returns all the airports contained in this packet
	 */
	@Override
	public FacilityAirport[] getFacilities() {
		return airports;
	}
	
	

}
