/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.conf;

import java.io.Serializable;
import java.net.InetAddress;

/**
 *
 * @author Divya_PKV
 */
public class Node implements Serializable {

    String nodeName;
    Integer nodeIndex;
    InetAddress nodeAddress;
    Integer memoryCapacity;
    Integer processorLoad;
    Integer batteryLevel;
    

    Node(String name, int index, InetAddress address) {
        nodeName = name;
        nodeIndex = index;
        nodeAddress = address;
        memoryCapacity = index * 1000;
        processorLoad = index * 100;
        batteryLevel = index * 10;
    }

    public int getIndex() {
        return nodeIndex;
    }

    public String getName(){
        return nodeName;
    }

    public InetAddress getAdrress(){
        return nodeAddress;
    }

    public int getMemoryCapacity(){
        return memoryCapacity;
    }

    public int getProcessorLoad(){
        return processorLoad;
    }

    public int getBatteryLevel(){
        return batteryLevel;
    }

    @Override
    public String toString(){
        String str = "";
        str+="Name: " + nodeName;
        str+="\nIndex: " + nodeIndex;
        str+="\nAdrress: " + nodeAddress;
        str+="\nMemory Capacity: " + memoryCapacity;
        str+="\nProcessor Load: " + processorLoad;
        str+="\nBattery Level: " + batteryLevel;
        str+="\n";
        return str;
    }
}
