package flightsim.simconnect;

import java.nio.ByteBuffer;

import flightsim.simconnect.recv.*;

/**
 * Base interface for dispatchers. Dispatchers handle a buffer containing
 * raw data as received from the network. It has to be parsed
 * and to generate a valid {@link RecvPacket} subclass. {@link AbstractDispatcher}
 * is one of them.
 * 
 * @see AbstractDispatcher
 * @see DispatcherTask
 * 
 * @author lc0277
 *
 */
public interface Dispatcher {
	
	/**
	 * Handle received data
	 * @param simConnect the simConnect instance that received the data
	 * @param data data directly read from network. Position is set to the beginning
	 * and <code>limit</code> to the end of data.
	 */
	public void dispatch(SimConnect simConnect, ByteBuffer data);
}
