package flightsim.simconnect.config;

/**
 * Exception thrown when a configuration is not found
 * @author lc0277
 *
 */
public class ConfigurationNotFoundException extends Exception {
	private final int number;
	
	ConfigurationNotFoundException(int number) {
		this.number = number;
	}

	private static final long serialVersionUID = -7402875474831801952L;

	/**
	 * Returns the missing configuration number
	 * @return configuration number
	 */
	public int getNumber() {
		return number;
	}
}
