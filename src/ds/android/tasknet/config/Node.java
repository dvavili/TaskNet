package ds.android.tasknet.config;

import java.io.Serializable;
import java.net.InetAddress;

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
    long memoryCapacity;
    float processorLoad;
    Integer batteryLevel;
    String taskId;
    Boolean distributed;

    Node(String name, int index, InetAddress address) {
        nodeName = name;
        nodeIndex = index;
        nodeAddress = address;
        memoryCapacity = 0;
        processorLoad = 0;
        batteryLevel = 0;
        distributed = false;
    }

    public void setTaskid(String id) {
        taskId = id;
    }

    public String getTaskid() {
        return taskId;
    }

    public int getIndex() {
        return nodeIndex;
    }

    public String getName() {
        return nodeName;
    }

    public InetAddress getAdrress() {
        return nodeAddress;
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

    public Boolean hasBeenDistributed(){
        return distributed;
    }

    public void setDistributed(Boolean flag){
        distributed = flag;
    }

    public void update(long currentRAM, float CPUsage, int currentBatteryLevel) {
        memoryCapacity = currentRAM;
        processorLoad = CPUsage;
        batteryLevel = currentBatteryLevel;
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
