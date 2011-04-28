package ds.android.tasknet.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
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
    public static final String[] clockTypes = {"LOGICAL", "VECTOR", "DEFAULT"};
    public static int host_index;
    public static String LOGGER_NAME;
    public static Integer host_initial_load;
    public static Integer host_reserved_load;
    public static String COORDINATOR;
    public static final String MULTICAST_MESSAGE = "multicast";
    public static final String ENTER_CRITICAL_SECTION = "Enter Critical Section";
    public static final String LEAVE_CRITICAL_SECTION = "Leave Critical Section";
    public static final int SIZE_OF_BUFFER = 10000;
    public static final int PROFILE_UPDATE_TIME_PERIOD = 500;
    public static final int TOTAL_PROCESSOR_LOAD_AT_NODE = 100;
    public static final int RESERVED_PROCESSOR_AT_NODE = 10;
    public static final int TOTAL_MEMORY_LOAD_AT_NODE = 512;
    public static final int RESERVED_MEMORY_AT_NODE = 64;
    public static final int TOTAL_BATTERY_AT_NODE = 10000;
    public static final int RESERVED_BATTERY_AT_NODE = 100;
    public static final int BATTERY_SPENT_IN_COMMUNICATION_SEND = 2;
    public static final int BATTERY_SPENT_IN_COMMUNICATION_RECEIVE = 1;
    public static final int BATTERY_SPENT_IN_TASK_CHUNK_EXECUTION = 10;
    public static final int BATTERY_SPENT_IN_TASK_DISTRIBUTION = 20;
    public static final int NODE_BATTERY_INFO_QUEUE_SIZE = 1;
    public static final int MINIMUM_FRAGMENTATION_LOAD = 100;
    public static final int MAX_TASK_CHUNK_LOAD_SIZE = 200;
    public static final int NUMBER_OF_RETRIES_BEFORE_QUITTING = 3;
    public static final int TIMEOUT_LOAD_PROMISE = 500;
    public static final int WAIT_TIME_BEFORE_REMOVING_DEAD_PROMISES = 500;
    public static final int WAIT_TIME_BEFORE_RETRYING = 1000;
    public static final int WAIT_TIME_BEFORE_RETRYING_BOOTSTRAP = 2000;
    public static boolean logDrop, logDelay, logDuplicate, logEvent;
    public static String crashNode;

    public static enum TASK_STATUS {
        ADVERTISED, DISTRIBUTED, RECEIVED_RESULTS
    };

    public static enum TASK_CHUNK_STATUS {
        DISTRIBUTED, RECEIVED
    };
    public static final int NUMBER_PACKETS_NETWORK_STABLIZE = 20;
    public static final float ALPHA_MAX = 4.0f;
    public static final float ALPHA_MIN = 1.2f;
    public static final float ALPHA_SAFE = 2.0f;
    public static final float ALPHA_POSITIVE_RANGE = 0.2f;
    public static final float BETA = 1.0f;
    public static final float GAMA = 0.8f;
    public static final int TASK_DEFAULT_CPU_LOAD = 15;
    public static final int TASK_DEFAULT_MEMORY_LOAD = 32;
    public static final boolean DEBUG_MODE = false;

    public static void setHostDetails(String configuration_filename,
            String local_host) {
        // nodesList = new ArrayList<Node>();
//		nodes = new HashMap<String, Node>();
//		node_names = new HashMap<Integer, String>();
//		node_addresses = new HashMap<String, InetAddress>();
        crashNode = "";
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(configuration_filename));
            LOGGER_NAME = prop.getProperty("LOGGER");
            COORDINATOR = prop.getProperty("COORDINATOR");
            host_initial_load = host_reserved_load = 10;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
