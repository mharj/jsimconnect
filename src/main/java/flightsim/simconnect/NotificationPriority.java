package flightsim.simconnect;

/**
 * Notification priorities.
 * <p> Each notifiction group has an assigned priority, and the SimConnect server will send events out strictly 
 * in the order of priority. No two groups will be set at the same priority. If a request is recieved for
 * a group to be set at a priority that has already been taken, the group will be assigned the next lowest
 *  priority that is available. This includes groups from all the clients that have opened communications with 
 *  the server. </p>
 * 
 * <p> If a group has an assigned priority above {@link #HIGHEST_MASKABLE} then it cannot mask events (hide 
 * them from other clients). If the group has a priority equal to or below {@link #HIGHEST_MASKABLE}, then 
 * events can be masked (the maskable flag must be set by the 
 * {@link SimConnect#addClientEventToNotificationGroup(int, int, boolean)} function to do this). Note that 
 * it is possible to mask Flight Simulator events, and therefore intercept them before they reach the simulation 
 * engine, and perhaps send new events to the simulation engine after appropriate processing has been done. 
 * Flight Simulator's simulation engine is treated as as SimConnect client in this regard, with a priority 
 * of {@link #DEFAULT}. </p>
 * 
 * <p> Input group events work in a similar manner. The priority groups are not combined though, a group and
 *  an input group can both have the same priority number. The SimConnect server manages two lists: notification 
 *  groups and input groups. </p>
 *  
 * <p> A typical use of masking is to prevent Flight Simulator itself from receiving an event, in order 
 * for the SimConnect client to completely replace the fucntionality in this case. Another use of masking 
 * is with co-operative clients, where there are multiple versions (perhaps a delux and standard version, 
 * or later and earlier versions), where the delux or later version might need to mask events from the 
 * other client, if they are both up and running. Flight Simulator does not mask any events.
 * </p>

 * 
 * 
 * @author lc0277
 * 
 *
 */
public enum NotificationPriority {

	/*
	 * we use hex values so java won't complain with out of range
	 * values 
	 */
	
	/** The highest priority. */
	HIGHEST					(0x00000001),
	/**  	The hightest priority that allows events to be masked. */
	HIGHEST_MASKABLE		(0x00989680),
	/** The standard priority. */
	STANDARD				(0x713fb300),
	/** The default priority. */
	DEFAULT					(0x77359400),
	/** Priorities lower than this will be ignored. */
	LOWEST					(0xee6b2800)
	;
	
	private final int value; 
	
	private NotificationPriority(int value) {
		this.value = value;
	}

	/**
	 * Return the numeric value of the event
	 * @return value of the event
	 */
	public int value() {
		return value;
	}
}
