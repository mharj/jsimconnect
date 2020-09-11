package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

/**
 * The <code>RecvAirportList</code> structure is used to return a list of {@link FacilityVOR} structures.
 * @since 0.5
 * @author lc0277
 *
 */
public class RecvVORList extends RecvFacilitiesList {
	private FacilityVOR[] vors;
	
	RecvVORList(ByteBuffer bf) {
		super(bf, RecvID.ID_VOR_LIST);
		
		// parse all airports
		int size = getArraySize();
		vors = new FacilityVOR[size];
		
		for (int i = 0; i < size; i++) {
			vors[i] = new FacilityVOR(bf);
		}
	}

	/**
	 * Returns all the airports contained in this packet
	 */
	@Override
	public FacilityVOR[] getFacilities() {
		return vors;
	}
}
