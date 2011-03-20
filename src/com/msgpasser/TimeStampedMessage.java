/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.msgpasser;

import com.clock.ClockService;
import com.clock.LogicalClock;
import com.clock.VectorClock;
import com.conf.Preferences;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 * @author Divya
 */
public class TimeStampedMessage extends Message implements Serializable {

    ClockService clock;
    String eventID;

    public TimeStampedMessage(String dest, String kind, String id, Serializable data, ClockService c, boolean newTimeStamp) {
        super(dest, kind, id, data);
        if (c instanceof LogicalClock && newTimeStamp) {
            ((LogicalClock) c).incrementTime();
        } else if (c instanceof VectorClock && newTimeStamp) {
            ((VectorClock) c).incrementTime(Preferences.host_index);
        }
        if (c != null) {
            clock = c.getClockService();
        } else {
            clock = null;
        }
        if (c instanceof LogicalClock) {
            eventID = "E" + (Preferences.host_index + 1) + c.getTime();
        } else if (c instanceof VectorClock) {
            eventID = "E" + (Preferences.host_index + 1) + ((VectorClock) c).getTime(Preferences.host_index);
        }
    }

    public ClockService getClockService() {
        return clock;
    }

    public void setClock(ClockService c) {
        clock = c;
    }

    public String getEventID() {
        return eventID;
    }

    public void setEventID(String eid) {
        eventID = eid;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
//    public static void main(String args[]){
//        TimeStampedMessage tm = new TimeStampedMessage("alice", "kind1", "id1", "Hi bob", ClockFactory.getClock(ClockFactory.ClockType.VECTOR));
//    }
}
