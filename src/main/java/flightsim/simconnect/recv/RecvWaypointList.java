package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

/**
 * The <code>RecvAirportList</code> structure is used to return a list of {@link FacilityNDB} structures.
 * @since 0.5
 * @author lc0277
 *
 */
public class RecvWaypointList extends RecvFacilitiesList {
	private FacilityWaypoint[] waypoints;
	
	RecvWaypointList(ByteBuffer bf) {
		super(bf, RecvID.ID_WAYPOINT_LIST);
		
		// parse all airports
		int size = getArraySize();
		waypoints = new FacilityWaypoint[size];
		
		for (int i = 0; i < size; i++) {
			waypoints[i] = new FacilityWaypoint(bf);
		}
	}

	/**
	 * Returns all the airports contained in this packet
	 */
	@Override
	public FacilityWaypoint[] getFacilities() {
		return waypoints;
	}
	
	

}
