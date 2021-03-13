package flightsim.simconnect.config;

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;

import javax.swing.filechooser.FileSystemView;

/**
 * Read and parse configuration files. First read in "My Documents" folder, then in the current directory
 * @author lc0277
 * @since 0.2
 *
 */
public class ConfigurationManager {

	private static Vector<Configuration> configs = new Vector<Configuration>();
	private static boolean inited = false;
	
	private static void readConfiguration() {
		if (inited) return;
		// try in current dir
		File f = new File("SimConnect.cfg"); //$NON-NLS-1$
		if (f.exists())	try {
				parse(f);
				inited = true;
				return;
			} catch (IOException e) {}
			
		// home dir (works in linux)
		f = new File(System.getProperty("user.home"), "SimConnect.cfg"); //$NON-NLS-1$ //$NON-NLS-2$
		if (f.exists()) try {
				parse(f);
				inited = true;
				return;
			} catch (IOException ioe) {}
		
		// home dir (my documents) on XP
		f = new File(FileSystemView.getFileSystemView().getDefaultDirectory(), "SimConnect.cfg"); //$NON-NLS-1$
		if (f.exists()) try {
				parse(f);
				inited = true;
				return;
			} catch (IOException ioe) {}
		
	}
	
	private static final Pattern HDR_PATTERN = Pattern.compile("^\\s*\\[SimConnect(.(\\d+))?\\].*", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
	private static final Pattern LINE_PATTERN = Pattern.compile("^\\s*([^#]*)=([^#]*).*", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
	
	private static void parse(File f) throws IOException {
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		String line;
		Configuration current = null;
		while ((line = br.readLine()) != null) {
			java.util.regex.Matcher m = HDR_PATTERN.matcher(line);
			if (m.matches()) {
				current = new Configuration();
				try {
					int cfgNo = 0;
					if (m.group(2) != null) {
						cfgNo = Integer.parseInt(m.group(2));
					}
					if (configs.size()-1 < cfgNo) configs.setSize(cfgNo+1);
					configs.set(cfgNo, current);
				} catch (NumberFormatException nfe) {}
			}
			m = LINE_PATTERN.matcher(line);
			if (m.matches() && (current != null)) {
				String key = m.group(1).trim().toLowerCase();
				String val = m.group(2).trim();
				current.put(key, val);
			}
		}
		fr.close();
	}
        
        public static void addConfiguration(Configuration config) {
            configs.add(config);
        }
	
	/**
	 * Returns the configuration given its number
	 * @param number number of configuration block in file
	 * @return configuration
	 * @throws ConfigurationNotFoundException if number is not found 
	 */
	public static Configuration getConfiguration(int number) throws ConfigurationNotFoundException { 
		if (!inited) readConfiguration();
		if (configs.size() == 0 || number > configs.size()) throw new ConfigurationNotFoundException(number);
		return configs.get(number);
	}
}
