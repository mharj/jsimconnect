package flightsim.simconnect;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "flightsim.simconnect.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME, Locale.ENGLISH);
	private static final ResourceBundle RESOURCE_BUNDLE_DEF = ResourceBundle
	.getBundle(BUNDLE_NAME, Locale.ENGLISH);

	private Messages() {
	}

	public static String get(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	public static String getDefault(String key) {
		try {
			return RESOURCE_BUNDLE_DEF.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

}
