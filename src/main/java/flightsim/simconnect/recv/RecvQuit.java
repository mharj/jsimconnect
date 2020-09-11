package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

/**
 * Event received when Flight Simulator has exited.
 * 
 * @author lc0277
 *
 */
public class RecvQuit extends RecvPacket {

	RecvQuit(ByteBuffer bf) {
		super(bf, RecvID.ID_QUIT);
	}

}
