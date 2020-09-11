package flightsim.simconnect.wrappers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation class to "tag" fields for use with simconnect data requests/sends
 * 
 * @author lc0277
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FlightSimData {

	/**
	 * The FS variable name. See the SDK documentation for a list of 
	 * all available variables
	 * @return FS Variable name
	 */
	String variable();
	
	/**
	 * The variable units. mandatory for most numerical variables
	 * @return variable unit (or a blank string if unitless)
	 */
	String units() default "";
	
	/**
	 * The string length to use for string values. Valid values are 8, 32, 64,
	 * 128, 256 and 260. 
	 * @return string length
	 */
	int stringWidth() default 256;
}
