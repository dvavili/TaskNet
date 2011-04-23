package ds.android.tasknet.config;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import ds.android.tasknet.task.Task;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author Divya_PKV
 */
public class Node implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    String nodeName;
    Integer nodeIndex;
    String nodeAddress;
    Integer nodePort;
    long memoryLoad;
    float processorLoad;
    Integer batteryLevel;
    Map<String, Task> acceptedTasks = new HashMap<String, Task>();
    int promisedBattreyLoad;
    
    Date lastUpdated;

    public Node(String name, InetAddress address, Integer port) {
        nodeName = name;
        nodeIndex = -1;
        nodeAddress = address.getHostAddress();
        nodePort = port;
        memoryLoad = 0;
        processorLoad = 0;
        batteryLevel = Preferences.TOTAL_BATTREY_AT_NODE;
        promisedBattreyLoad = 0;
        lastUpdated = null;
    }

    public Integer getIndex() {
        return nodeIndex;
    }

    public String getName() {
        return nodeName;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public InetAddress getAdrress() throws UnknownHostException {
        return InetAddress.getByName(nodeAddress);
    }

    public void setNodeIndex(Integer nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    public Integer getNodePort() {
        return nodePort;
    }

    public void setNodePort(Integer nodePort) {
        this.nodePort = nodePort;
    }

    public long getMemoryLoad() {
        return memoryLoad;
    }

    public float getProcessorLoad() {
        return processorLoad;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int value) {
        batteryLevel = value;
    }

    public void decrBatteryLevel(int value) {
//    	System.out.println("decreasing battrey current: " + batteryLevel + " by " + value);
        batteryLevel -= value;        
    }
    
    public void incrProcessorLoad(float value) {
    	processorLoad += value;
    }
    
    public void decrProcessorLoad(float value) {
    	processorLoad -= value;
    }
    
    public void incrMemoryLoad(long value) {
    	memoryLoad += value;
    }
    
    public void decrMemoryLoad(long value) {
    	memoryLoad -= value;
    }    

    public void update(long currentRAM, float CPUsage, int currentBatteryLevel) {
        memoryLoad = currentRAM;
        processorLoad = CPUsage;
        batteryLevel = currentBatteryLevel;
        lastUpdated = Calendar.getInstance().getTime();
    }

    public void addToAcceptedTask(String taskAdvReplyId, Task task) {
        if (this.acceptedTasks.get(taskAdvReplyId) == null) {
            this.acceptedTasks.put(taskAdvReplyId, task);
        }
    }

    public void removeFromAcceptedTask(String taskId) {
        this.acceptedTasks.remove(taskId);
    }

    public Map<String,Task> getAcceptedTasks() {
        return this.acceptedTasks;
    }
    
    public Task getAcceptedTaskByTaskId(String taskId) {
        return this.acceptedTasks.get(taskId);
    }

    public int getPromisedBattreyLoad() {
        return promisedBattreyLoad;
    }

    public void setPromisedBattreyLoad(int promisedLoad) {
        this.promisedBattreyLoad = promisedLoad;
    }

    public void incrPromisedBattreyLoad(int promisedLoad) {
        this.promisedBattreyLoad += promisedLoad;
    }

    public void decrPromisedBattreyLoad(int promisedLoad) {
        this.promisedBattreyLoad -= promisedLoad;
    }

    public String toString() {
        String str = "";
        str += "Name: " + nodeName;
        str += "\nIndex: " + nodeIndex;
        str += "\nAdrress: " + nodeAddress;
        str += "\nPort: " + nodePort;
        str += "\nMemory Load: " + memoryLoad;
        str += "\nProcessor Load: " + processorLoad;
        str += "\nBattery Level: " + batteryLevel;
        str += "\n";
        return str;
    }
}
