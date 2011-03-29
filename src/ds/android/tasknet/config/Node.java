/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.android.tasknet.config;

import java.io.Serializable;
import java.net.InetAddress;

/**
 *
 * @author Divya_PKV
 */
public class Node implements Serializable {

    private static final long serialVersionUID = 1L;
    String nodeName;
    Integer nodeIndex;
    InetAddress nodeAddress;
    Integer memoryCapacity;
    Integer processorLoad;
    Integer batteryLevel;
    String taskId;

    Node(String name, int index, InetAddress address) {
        nodeName = name;
        nodeIndex = index;
        nodeAddress = address;
        memoryCapacity = (index+1) * 1000;
        processorLoad = (index+1) * 100;
        batteryLevel = (index+1) * 10;
    }

    public void setTaskId(String id){
        taskId = id;
    }

    public String getTaskId(){
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

    public int getMemoryCapacity() {
        return memoryCapacity;
    }

    public int getProcessorLoad() {
        return processorLoad;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int value) {
        batteryLevel -= value;
    }

    public void update(Node nodeToBeUpdated) {
        memoryCapacity = nodeToBeUpdated.getMemoryCapacity();
        processorLoad = nodeToBeUpdated.getProcessorLoad();
        batteryLevel = nodeToBeUpdated.getBatteryLevel();
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
