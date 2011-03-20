/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.logger;

import com.clock.ClockFactory;
import com.clock.LogicalClock;
import com.clock.VectorClock;
import com.conf.Preferences;
import com.msgpasser.Message;
import com.msgpasser.TimeStampedMessage;
import java.util.ArrayList;
import java.util.Vector;

/**
 *
 * @author Divya
 */
enum msgRelation {

    BEFORE("<-"), CONCURRENT("||"), AFTER("->");
    private String representation;

    msgRelation(String rep) {
        representation = rep;
    }

    public String getRepresentation() {
        return representation;
    }
}

public class Logs {

    private ArrayList<LogMessage> logMsgs;

    public Logs() {
        logMsgs = new ArrayList<LogMessage>();
    }

    public void add(Message msg) {
        logMsgs.add(new LogMessage(msg));
    }

    public ArrayList<LogMessage> getLogs() {
        return logMsgs;
    }

    public void orderLogs() {
        for (int i = 0; i < logMsgs.size(); i++) {
            for (int j = 0; j < i; j++) {
                int compareResult = 0;
                if (((TimeStampedMessage) logMsgs.get(i).getMessage()).getClockService() instanceof VectorClock) {
                    compareResult = compare(logMsgs.get(i), logMsgs.get(j), ClockFactory.ClockType.VECTOR);
                } else if (((TimeStampedMessage) logMsgs.get(i).getMessage()).getClockService() instanceof LogicalClock) {
                    compareResult = compare(logMsgs.get(i), logMsgs.get(j), ClockFactory.ClockType.LOGICAL);
                }
                if (compareResult < 0) {
                    LogMessage tempMsg = (LogMessage) logMsgs.get(i);
                    logMsgs.set(i, logMsgs.get(j));
                    logMsgs.set(j, tempMsg);
                }
            }
        }
    }

    int compare(LogMessage firstMsg, LogMessage secondMsg, ClockFactory.ClockType clockType) {
        int returnVal = 0;
        if(clockType == ClockFactory.ClockType.LOGICAL)
            returnVal = compareLogical(firstMsg,secondMsg);
        else if(clockType == ClockFactory.ClockType.VECTOR)
            returnVal = compareVector(firstMsg,secondMsg);
        return returnVal;
    }

    int compareLogical(LogMessage firstMsg, LogMessage secondMsg){
        int result = 0;
        firstMsg.setRelation(msgRelation.CONCURRENT.getRepresentation());
        int firstMsgClock = new Integer(((TimeStampedMessage) firstMsg.getMessage()).getClockService().getTime().toString());
        int secondMsgClock = new Integer(((TimeStampedMessage) secondMsg.getMessage()).getClockService().getTime().toString());
        if(firstMsgClock > secondMsgClock){
            result = 1;
            firstMsg.setRelation(msgRelation.AFTER.getRepresentation());
        }
        else if(firstMsgClock < secondMsgClock){
            result = -1;
            firstMsg.setRelation(msgRelation.BEFORE.getRepresentation());
        }
        return result;
    }
    
    int compareVector(LogMessage firstMsg, LogMessage secondMsg){
        int result = 0;
        Vector<Integer> vFirst = (Vector<Integer>) ((TimeStampedMessage) firstMsg.getMessage()).getClockService().getTime();
        Vector<Integer> vSecond = (Vector<Integer>) ((TimeStampedMessage) secondMsg.getMessage()).getClockService().getTime();
        for (int i = 0; i < Preferences.nodes.size(); i++) {
            if (vFirst.get(i) < vSecond.get(i)) {
                if (result > 0) {
                    result = 0;
                    firstMsg.setRelation(msgRelation.CONCURRENT.getRepresentation());
                    break;
                } else {
                    firstMsg.setRelation(msgRelation.BEFORE.getRepresentation());
                    result--;
                }
            } else if (vFirst.get(i) > vSecond.get(i)) {
                if (result < 0) {
                    result = 0;
                    firstMsg.setRelation(msgRelation.CONCURRENT.getRepresentation());
                    break;
                } else {
                    firstMsg.setRelation(msgRelation.AFTER.getRepresentation());
                    result++;
                }
            }
        }
        return result;
    }
}
