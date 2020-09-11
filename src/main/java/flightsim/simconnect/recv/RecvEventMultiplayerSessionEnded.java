package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

/**
 * The RecvEventMultiplayerSessionEnded structure is sent to a client when they have requested to leave a race, 
 * or to all players when the session is terminated by the host.
 * 
 * This is the only event that is broadcast to all the players in a multi-player race, in the situation where 
 * the host terminates, or simply leaves, the race. If a client ends their own participation in the race, 
 * they will be the only one to receive the event. 
 * @author lc0277
 * @since 0.7
 *
 */
public class RecvEventMultiplayerSessionEnded extends RecvEvent {
	
	RecvEventMultiplayerSessionEnded(ByteBuffer bf) {
		super(bf, RecvID.ID_EVENT_MULTIPLAYER_SESSION_ENDED);
	}
	
}
