package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

/**
 * The RecvEventMultiplayerClientStarted structure is sent to a client when they have successfully joined 
 * a multi-player race.
 * 
 *  This event is not transmitted to the host of the session, only to the client that has joined in. 
 *  To receive these events, refer to the SimConnect_SubscribeToSystemEvent function. 
 * 
 * @author lc0277
 * @since 0.7
 *
 */
public class RecvEventMultiplayerClientStarted extends RecvEvent {

	RecvEventMultiplayerClientStarted(ByteBuffer bf) {
		super(bf, RecvID.ID_EVENT_MULTIPLAYER_CLIENT_STARTED);
	}

}
