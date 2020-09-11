package flightsim.simconnect;

/**
 * AddToClientDataDefinition <code>SizeOrType<code> parameter type values
 * @since 0.5
 * @author lc0277
 *
 */
public enum ClientDataType {
	INT8		(-1),
	INT16		(-2),
	INT32		(-3),
	INT64		(-4),
	FLOAT32		(-5),
	FLOAT64		(-6);
	
	private final int value; 
	
	private ClientDataType(int value) {
		this.value = value;
	}

	/**
	 * Return the numeric value of the data type
	 * @return value of the event
	 */
	public int value() {
		return value;
	}

}
