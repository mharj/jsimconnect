package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

/**
 * The <code>RecvAirportList</code> structure is used to return a list of {@link FacilityNDB} structures.
 * @since 0.5
 * @author lc0277
 *
 */
public class RecvNDBList extends RecvFacilitiesList {
	private FacilityNDB[] ndbs;
	
	RecvNDBList(ByteBuffer bf) {
		super(bf, RecvID.ID_NDB_LIST);
		
		// parse all airports
		int size = getArraySize();
		ndbs = new FacilityNDB[size];
		
		for (int i = 0; i < size; i++) {
			ndbs[i] = new FacilityNDB(bf);
		}
	}

	/**
	 * Returns all the airports contained in this packet
	 */
	@Override
	public FacilityNDB[] getFacilities() {
		return ndbs;
	}
	
	

}
