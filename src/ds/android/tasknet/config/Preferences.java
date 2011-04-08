package ds.android.tasknet.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @authors
 * Divya Vavili - dvavili@andrew.cmu.edu
 * Yash Pathak - ypathak@andrew.cmu.edu
 *
 */

/**
 * Constants/Preferences used in the project
 */
public class Preferences {

	public static final String conf_file = System.getProperty("file.separator")
			+ "mnt" + System.getProperty("file.separator") + "sdcard"
			+ System.getProperty("file.separator") + "ppfile.ini";
	public static final int num_kind = 6;
	public static final int num_msgid = 6;
	public static final int delayedEnoughThreshold = 1000;
	public static final int receiverDelayedEnoughThreshold = 1000;
	public static final int numClockTypes = 3;
	public static final String[] clockTypes = { "LOGICAL", "VECTOR", "DEFAULT" };
	public static int host_index;
	public static String logger_name;
	public static Integer host_initial_load;
	public static Integer host_reserved_load;
	public static String COORDINATOR;
	public static final String MULTICAST_MESSAGE = "multicast";
	public static final String ENTER_CRITICAL_SECTION = "Enter Critical Section";
	public static final String LEAVE_CRITICAL_SECTION = "Leave Critical Section";
	public static final int SIZE_OF_BUFFER = 10000;
	public static final int PROFILE_UPDATE_TIME_PERIOD = 2000;
	public static final int TOTAL_LOAD_AT_NODE = 100;
	// public static ArrayList<Node> nodesList;
	public static HashMap<String, InetAddress> node_addresses;
	public static HashMap<String, Node> nodes;
	public static HashMap<Integer, String> node_names;
	public static boolean logDrop, logDelay, logDuplicate, logEvent;
	public static String crashNode;

	public static void setHostDetails(String configuration_filename,
			String local_host) {
		// nodesList = new ArrayList<Node>();
		nodes = new HashMap<String, Node>();
		node_names = new HashMap<Integer, String>();
		node_addresses = new HashMap<String, InetAddress>();
		crashNode = "";
		try {
			Properties prop = new Properties();
			prop.load(new FileInputStream(configuration_filename));
			logger_name = prop.getProperty("LOGGER");
			COORDINATOR = prop.getProperty("COORDINATOR");
			StringTokenizer node_string = new StringTokenizer(
					prop.getProperty("NAMES"), ",");
			int num_nodes = node_string.countTokens();
			for (int i = 0; i < num_nodes; i++) {
				String node_name = node_string.nextToken();
				if (node_name.equalsIgnoreCase(local_host)) {
					host_index = i;
					host_initial_load = host_reserved_load = new Integer(
							prop.getProperty("node." + local_host
									+ ".reserved_load"));
				}
				if (!node_name.equalsIgnoreCase(logger_name)) {
					Node node = new Node(node_name, i,
							InetAddress.getByName(prop.getProperty("node."
									+ node_name + ".ip")));
					// nodesList.add(node);
					nodes.put(node_name, node);
					node_names.put(i, node_name);
					node_addresses.put(
							node_name,
							InetAddress.getByName(prop.getProperty("node."
									+ node_name + ".ip")));
				}
			}
		} catch (FileNotFoundException ex) {
			Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (IOException ex) {
			Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}
}