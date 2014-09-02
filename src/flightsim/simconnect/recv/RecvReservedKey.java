package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

import flightsim.simconnect.SimConnect;

/**
 * The <code>RecvReservedKey</code> structure is used with the 
 * {@link SimConnect#requestReservedKey(int, String)} function to return 
 * the reserved key combination.
 * 
 * @author lc0277
 *
 */
public class RecvReservedKey extends RecvPacket {

	private final String choiceReserved;
	private final String reservedKey;
	
	RecvReservedKey(ByteBuffer bf) {
		super(bf, RecvID.ID_RESERVED_KEY);
		choiceReserved = super.makeString(bf, 50);
		reservedKey = super.makeString(bf, 30);
	}
	
	/**
	 * Returns a string containing the key that has been reserved. 
	 * This will be identical to the string entered as one of the 
	 * choices for the {@link SimConnect#requestReservedKey(int, String)}
	 * @return reserved key
	 */
	public String getChoiceReserved() {
		return choiceReserved;
	}
	
	/**
	 * string containing the reserved key combination. This will be 
	 * an uppercase string containing all the modifiers that apply. 
	 * For example, if the client program requests "q", and the choice 
	 * is accepted, then this parameter will contain "TAB+Q". 
	 * If the client program requests "Q", then this parameter will 
	 * contain "SHIFT+TAB+Q". This string could then appear, for 
	 * example, in a dialog from the client application, informing 
	 * a user of the appropriate help key.
	 * @return reserved key
	 */
	public String getReservedKey() {
		return reservedKey;
	}
	
}
