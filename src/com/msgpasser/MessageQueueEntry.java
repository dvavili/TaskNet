/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.msgpasser;

import com.clock.VectorClock;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Vector;

/**
 *
 * @author Divya
 */
public class MessageQueueEntry implements Serializable{
    boolean[] msgReceived;
    Vector<Integer> timeStamp;
    MulticastMessage message;

    MessageQueueEntry(MulticastMessage msg, Vector vc, boolean[] msgRcvd){
        message = msg;
        timeStamp = vc;
        msgReceived = msgRcvd;
    }

    Vector getTimeStamp() {
        return timeStamp;
    }

    boolean[] getMsgReceivedArray() {
        return msgReceived;
    }

    void setMsgReceivedArray(boolean[] boolArray){
        msgReceived = boolArray;
    }

    MulticastMessage getMessage(){
        return message;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}
