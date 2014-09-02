package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

/**
 * The RecvEventMultiplayerServerStarted structure is 
 * sent to the host when the session is visible to other users in the lobby.
 * 
 *  This event is sent only to the host of the session.
 * @author lc0277
 * @since 0.7
 *
 */
public class RecvEventMultiplayerServerStarted extends RecvEvent {

	RecvEventMultiplayerServerStarted(ByteBuffer bf) {
		super(bf, RecvID.ID_EVENT_MULTIPLAYER_SERVER_STARTED);
	}

}
