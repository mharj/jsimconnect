package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

import flightsim.simconnect.SimConnect;

/**
 * The <code>RecvEventFrame</code> structure is used with the 
 * {@link SimConnect#subscribeToSystemEvent(int, String)} call 
 * to return the frame rate and simulation speed to the client.
 * 
 * <p> Sets the requested system event to <b>Frame</b> or <b>PauseFrame<b> 
 * with the {@link SimConnect#subscribeToSystemEvent(int, String)} 
 * function to receive this data. </p>
 * 
 * @author lc0277
 *
 */
public class RecvEventFrame extends RecvEvent {
	private final float frameRate;
	private final float simSpeed;
	
	RecvEventFrame(ByteBuffer bf) {
		super(bf, RecvID.ID_EVENT_FRAME);
		frameRate = bf.getFloat();
		simSpeed = bf.getFloat();
	}

	/**
	 * Returns the visual frame rate in frames per second.
	 * @return frame rate
	 */
	public float getFrameRate() {
		return frameRate;
	}

	/**
	 * Returns the simulation rate. For example if the simulation 
	 * is running at four times normal speed - 4X - then 4.0 will 
	 * be returned.
	 * @return simulation speed
	 */
	public float getSimSpeed() {
		return simSpeed;
	}
	
	
}
