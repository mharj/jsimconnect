package flightsim.simconnect.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * A configuration block. Extends <code>HashTable</code> to easily store values
 *
 * @author lc0277
 * @since 0.2
 *
 */
public class Configuration extends Hashtable<String, String> {

    private static final long serialVersionUID = 4120183786070819349L;

    /**
     * Get a parameter from the configuration
     *
     * @param key parameter name (case insensitive)
     * @param def default value if parameter is not found
     * @return parameter value
     */
    public String get(String key, String def) {
        String s = get(key.toLowerCase());
        if (s == null) {
            return def;
        } else {
            return s;
        }
    }

    public String get(String key) {
        return super.get(key.toLowerCase());
    }

    /**
     * Get an integer parameter from the configuration
     *
     * @param key parameter name (case insensitive)
     * @param def default value if parameter is not found
     * @return parameter value
     */
    public int getInt(String key, int def) {
        String s = get(key.toLowerCase());
        if (s == null) {
            return def;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            return def;
        }
    }

    /**
     * Get a boolean parameter from the configuration. Booleans are represented
     * by integers 0/1
     *
     * @param key parameter name (case insensitive)
     * @param def default value if not found or invalid format
     * @return parameter value
     */
    public boolean getBoolean(String key, boolean def) {
        int val = getInt(key, def ? 1 : 0);
        if (val == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Add a key=value pair to the configuration
     */
    @Override
    public synchronized String put(String key, String value) {
        return super.put(key.toLowerCase(), value);
    }

    /**
     * The litteral config key constant for the local address
     *
     * @since 0.6
     */
    public static final String ADDRESS = "Address";

    /**
     * The litteral config key constant for the protocol to use
     *
     * @since 0.6
     */
    public static final String PROTOCOL = "Protocol";

    /**
     * The litteral config value constant to specify ipv4 connection
     *
     * @since 0.6
     */
    public static final String PROTOCOL_IPv4 = "IPv4";

    /**
     * The litteral config value constant to specify ipv6 connection
     *
     * @since 0.6
     */
    public static final String PROTOCOL_IPv6 = "IPv6";

    public static final String PROTOCOL_PIPE = "Pipe";

    /**
     * The litteral config key constant for the tcp port number to use.
     *
     * @since 0.6
     */
    public static final String PORT = "Port";

    /**
     * The litteral config key constant for the port number to use
     *
     * @since 0.6
     */
    public static final String MAX_RECEIVE_SIZE = "MaxReceiveSize";

    /**
     * The litteral config key constant to specify nodelay settings
     *
     * @since 0.6
     */
    public static final String DISABLE_NAGLE = "DisableNagle";

    /**
     * Sets the network address to use for this connection
     *
     * @param address
     * @since 0.6
     */
    public void setAddress(String address) {
        put(ADDRESS, address);
    }

    /**
     * Sets the network port to use for this connection
     *
     * @param port
     * @since 0.6
     */
    public void setPort(int port) {
        put(PORT, Integer.toString(port));
    }

    /**
     * Sets the network port as string to use for this connection
     * Adds support for named pipes
     *
     * @param port
     * @since 0.6
     */
    public void setPort(String port) {
        put(PORT, port);
    }

    /**
     * Sets the ip protocol version to use for this connection
     *
     * @param protocol
     * @throws IllegalArgumentException if protocol is not 4 or 6 or 255
     * @since 0.8
     */
    public void setProtocol(int protocol) {
        if (protocol != 4 && protocol != 6 && protocol != 255) {
            throw new IllegalArgumentException("Bad protocol version (" + protocol + ")");
        }
        if (protocol == 255) {
            put(PROTOCOL, PROTOCOL_PIPE);
            return;
        }
        put(PROTOCOL, protocol == 4 ? PROTOCOL_IPv4 : PROTOCOL_IPv6);
    }

    /**
     * Attempts to retrieve the simconnect TCP port number by looking up
     * registry values. Negative values indicates a failure while guessing the
     * registry port (ie no registry available, process execute failure, no key,
     * etc). A zero value is thus a success, and it indicates that an ipv4 is
     * not open because either FSX is not running, or no local ipv4 connections
     * has been configured in simconnect.xml
     *
     * @return -1 if registry key is not found or cannot be read; >= 0 else.
     * @see #findSimConnectPortIPv6()
     * @since 0.6
     */
    public static int findSimConnectPortIPv4() {
        return readRegistryValue("SimConnect_Port_IPv4");
    }

    /**
     * Attempts to retrieve the simconnect TCP port number by looking up
     * registry values. Negative values indicates a failure while guessing the
     * registry port (ie no registry available, process execute failure, no key,
     * etc). A zero value is thus a success, and it indicates that an ipv6 is
     * not open because either FSX is not running, or no local ipv6 connections
     * has been configured in simconnect.xml.
     * <p>
     * <b>Warning: </b> currently ipv6 support is broken on Windows 2003, NT and
     * XP.
     *
     * @return -1 if registry key is not found or cannot be read; >= 0 else.
     * @see #findSimConnectPortIPv4()
     * @since 0.6
     */
    public static int findSimConnectPortIPv6() {
        return readRegistryValue("SimConnect_Port_IPv6");
    }

    /**
     * Attempts to retrieve the simconnect TCP port number by looking up
     * registry values. Negative values indicates a failure while guessing the
     * registry port (ie no registry available, process execute failure, no key,
     * etc). A zero value is thus a success, and it indicates that no local
     * connections has been configured in simconnect.xml or FSX is not running.
     * <br/>
     * This method just try to lookup the IPv4 port first, then fallback with
     * the ipV6 port if not found
     *
     * @return -1 if registry key is not found or cannot be read; >= 0 else.
     * @see #findSimConnectPortIPv4()
     * @see #findSimConnectPortIPv6()
     * @since 0.6
     */
    public static int findSimConnectPort() {
        int port4 = findSimConnectPortIPv4();
        if (port4 <= 0) {
            return findSimConnectPortIPv6();
        }
        return port4;
    }

    /**
     * Attemps read a registry value
     *
     * @param key
     * @since 0.6
     * @return -1 on failure else the read value
     */
    private static int readRegistryValue(String key) {
        try {
            Process p = Runtime.getRuntime().exec("reg query \"HKCU\\Software\\Microsoft\\Microsoft Games\\Flight Simulator\" /v " + key);
            InputStream is = p.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                StringTokenizer toker = new StringTokenizer(line);

                try {
                    String regKey = toker.nextToken().trim();
                    String regType = toker.nextToken().trim();
                    String regValue = toker.nextToken().trim();
                    if (regKey.equalsIgnoreCase(key)
                            && "REG_SZ".equalsIgnoreCase(regType)) {
                        return Integer.parseInt(regValue);
                    }

                } catch (NoSuchElementException nse) {
                    // bad line, proceed with next
                } catch (NumberFormatException nfe) {
                    // bad value?
                }

            }
            is.close();
        } catch (IOException e) {
        }

        return -1;
    }

}
