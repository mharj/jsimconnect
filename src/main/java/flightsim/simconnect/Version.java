package flightsim.simconnect;

import java.io.InputStream;
import java.util.Properties;

import javax.swing.JOptionPane;

/**
 * Contains static reference to simconnect version number
 * @author lc
 *
 */
public class Version {
	
	/** version string, eg "0.5" */
	public static final String VERSION_STRING	=	"0.7";
	
	/** version major number, eg "0" */
	public static final int VERSION_MAJOR	=	0;
	
	/** version major number, eg "5" */
	public static final int VERSION_MINOR	=	7;
	
	/** patchlevel number, eg "0" */
	public static final int PATCHLEVEL	=	0;
	
	/** build number 
	 * @return build number 
	 */
	public static int buildNumber() {
		InputStream is = Version.class.getResourceAsStream("/jsimconnect.build.number");
		Properties props = new Properties();
		try {
			props.load(is);
			String val = props.getProperty("build.number", "-1");
			return Integer.parseInt(val);
		} catch (Exception e) {
		}
		return -1;	// fallback
	}
	
	public static void main(String[] args) {
		int bn = buildNumber();
		String ver = VERSION_MAJOR + "." + VERSION_MINOR + " (" + PATCHLEVEL + ") build "  + bn + " proto " + SimConnectConstants.PROTO_VERSION;
		System.out.println("JSimConnect " + ver);
		JOptionPane.showMessageDialog(null, "JSimConnect " + ver, 
				"JSimConnect",
				JOptionPane.INFORMATION_MESSAGE);
	}
}
