package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

import flightsim.simconnect.Dispatcher;
import flightsim.simconnect.SimConnect;

/**
 * A dispatcher that creates data structures of correct types and pass them
 * to the subclass.
 * 
 * @author lc0277
 *
 */
public abstract class AbstractDispatcher implements Dispatcher {
	
	/**
	 * Process the received packet. <code>RecvPacket</code> is a subclass
	 * of {@link RecvPacket}, or an instance of {@link RecvPacket} itself
	 * if the data is of unknown type.
	 * 
	 * @param simConnect simconnect instance that received the packet
	 * @param recv data
	 */
	public abstract void onDispatch(SimConnect simConnect, RecvPacket recv);
	
	private static RecvPacket buildCustomPacket(ByteBuffer data) {
		RecvPacket packet = null;
		
		// get the type of data
		int id = data.getInt(8);
		RecvID rid = RecvID.type(id);
		
		switch (rid) {
		case ID_EVENT:
			packet = new RecvEvent(data);
			break;
		case ID_EXCEPTION:
			packet = new RecvException(data);
			break;
		case ID_OPEN:
			packet = new RecvOpen(data);
			break;
		case ID_EVENT_FILENAME:
			packet = new RecvEventFilename(data);
			break;
		case ID_CUSTOM_ACTION:
			packet = new RecvCustomAction(data);
			break;
		case ID_EVENT_FRAME:
			packet = new RecvEventFrame(data);
			break;
		case ID_SIMOBJECT_DATA:
			packet = new RecvSimObjectData(data);
			break;
		case ID_EVENT_OBJECT_ADDREMOVE:
			packet = new RecvEventAddRemove(data);
			break;
		case ID_SIMOBJECT_DATA_BYTYPE:
			packet = new RecvSimObjectDataByType(data);
			break;
		case ID_QUIT:
			packet = new RecvQuit(data);
			break;
		case ID_SYSTEM_STATE:
			packet = new RecvSystemState(data);
			break;
		case ID_CLIENT_DATA:
			packet = new RecvClientData(data);
			break;
		case ID_ASSIGNED_OBJECT_ID:
			packet = new RecvAssignedObjectID(data);
			break;
		case ID_CLOUD_STATE:
			packet = new RecvCloudState(data);
			break;
		case ID_RESERVED_KEY:
			packet = new RecvReservedKey(data);
			break;
		case ID_WEATHER_OBSERVATION:
			packet = new RecvWeatherObservation(data);
			break;
		case ID_EVENT_WEATHER_MODE:
			packet = new RecvEventWeatherMode(data);
			break;
		case ID_AIRPORT_LIST:
			packet = new RecvAirportList(data);
			break;
		case ID_NDB_LIST:
			packet = new RecvNDBList(data);
			break;
		case ID_VOR_LIST:
			packet = new RecvVORList(data);
			break;
		case ID_WAYPOINT_LIST:
			packet = new RecvWaypointList(data);
			break;
		case ID_EVENT_MULTIPLAYER_CLIENT_STARTED:
			packet = new RecvEventMultiplayerClientStarted(data);
			break;
		case ID_EVENT_MULTIPLAYER_SERVER_STARTED:
			packet = new RecvEventMultiplayerServerStarted(data);
			break;
		case ID_EVENT_MULTIPLAYER_SESSION_ENDED:
			packet = new RecvEventMultiplayerSessionEnded(data);
			break;
		case ID_EVENT_RACE_END:
			packet = new RecvEventRaceEnd(data);
			break;
		case ID_EVENT_RACE_LAP:
			packet = new RecvEventRaceLap(data);
			break;
			
		case ID_NULL:
		default:
				// means not yet implemented
			packet = new RecvPacket(data);
		}
		return packet;
	}

	public void dispatch(SimConnect simConnect, ByteBuffer data) {
		RecvPacket packet = buildCustomPacket(data);
		onDispatch(simConnect, packet);
	}
}
