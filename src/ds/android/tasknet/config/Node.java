package ds.android.tasknet.config;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import ds.android.tasknet.task.Task;

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
    InetAddress nodeAddress;
    Integer nodePort;
    long memoryCapacity;
    float processorLoad;
    Integer batteryLevel;
    Map<String, Task> acceptedTasks = new HashMap<String, Task>();
    int promisedLoad;

    public Node(String name, InetAddress address, Integer port) {
        nodeName = name;
        nodeIndex = -1;
        nodeAddress = address;
        nodePort = port;
        memoryCapacity = 0;
        processorLoad = 0;
        batteryLevel = 0;
        promisedLoad = 0;
    }

    public Integer getIndex() {
        return nodeIndex;
    }

    public String getName() {
        return nodeName;
    }

    public InetAddress getAdrress() {
        return nodeAddress;
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

    public long getMemoryCapacity() {
        return memoryCapacity;
    }

    public float getProcessorLoad() {
        return processorLoad;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int value) {
//        batteryLevel -= value;
    }

//    public Boolean hasBeenDistributed() {
//        return distributed;
//    }
//
//    public void setDistributed(Boolean flag) {
//        distributed = flag;
//    }

    public void update(long currentRAM, float CPUsage, int currentBatteryLevel) {
        memoryCapacity = currentRAM;
        processorLoad = CPUsage;
        batteryLevel = currentBatteryLevel;
    }

    public void addToAcceptedTask(String taskAdvReplyId, Task task) {
    	if(this.acceptedTasks.get(taskAdvReplyId) == null) {
    		this.acceptedTasks.put(taskAdvReplyId, task);
    	}
    }
    
    public void removeFromAcceptedTask(String taskId) {
    	this.acceptedTasks.remove(taskId);
    }
    
    public Task getAcceptedTaskByTaskId(String taskId) {
    	return this.acceptedTasks.get(taskId);
    }
    
    public int getPromisedLoad() {
		return promisedLoad;
	}

	public void setPromisedLoad(int promisedLoad) {
		this.promisedLoad = promisedLoad;
	}
	
	public void incrPromisedLoad(int promisedLoad) {
		this.promisedLoad += promisedLoad;
	}

	public void decrPromisedLoad(int promisedLoad) {
		this.promisedLoad -= promisedLoad;
	}
	@Override
    public String toString() {
        String str = "";
        str += "Name: " + nodeName;
        str += "\nIndex: " + nodeIndex;
        str += "\nAdrress: " + nodeAddress;
        str += "\nMemory Capacity: " + memoryCapacity;
        str += "\nProcessor Load: " + processorLoad;
        str += "\nBattery Level: " + batteryLevel;
        str += "\n";
        return str;
    }
}
