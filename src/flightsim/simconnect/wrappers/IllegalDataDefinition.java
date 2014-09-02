package flightsim.simconnect.wrappers;

/**
 * Exception that occurs in the data definition wrappers
 * 
 * @author lc0277
 *
 */
public class IllegalDataDefinition extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2487573609936505337L;

	public IllegalDataDefinition(String cause) {
		super(cause);
	}

}
