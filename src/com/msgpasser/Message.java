package com.msgpasser;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @authors
 * Divya Vavili - dvavili@andrew.cmu.edu
 * Yash Pathak - ypathak@andrew.cmu.edu
 *
 */

/**
 * Message:
 * Wraps the 'object' to be sent with destination, kind and id details
 */
public class Message implements Serializable {

    public enum NormalMsgType { NORMAL, PROFILE_XCHG, TASK_ADV };

    protected Serializable data;
    protected String destination;
    protected String kind;
    protected String id;
    protected String logMessage;
    protected Boolean log;
    protected NormalMsgType normalMsgType;

    public Message(String dest, String kind, String id, Serializable data) {
        this.data = data;
        this.kind = kind;
        this.destination = dest;
        this.id = id;
        log = false;
        logMessage = "";
        normalMsgType = NormalMsgType.NORMAL;
    }

    public void setNormalMsgType(NormalMsgType type){
        normalMsgType = type;
    }

    public NormalMsgType getNormalMsgType(){
        return normalMsgType;
    }

    public String getDest() {
        return destination;
    }

    public String getKind() {
        return kind;
    }

    public String getId() {
        return id;
    }

    public Serializable getData() {
        return data;
    }

    public void setLogMessage(String status){
        logMessage = status;
    }

    public String getLogMessage(){
        return logMessage;
    }

    public boolean isToBeLogged(){
        return log;
    }

    public void setToBeLogged(boolean flag){
        log = flag;
    }

    public void setDest(String dest){
        destination = dest;
    }
   
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}
