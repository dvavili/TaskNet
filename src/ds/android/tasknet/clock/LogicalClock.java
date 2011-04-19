/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ds.android.tasknet.clock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

/**
 *
 * @author Divya
 */
public class LogicalClock extends ClockService{
    private static final long serialVersionUID = 1L;
    Integer logicalClock;

    public LogicalClock() {
        logicalClock = 0;
    }

    private LogicalClock(Integer lClock) {
        logicalClock = lClock;
    }
   
    @Override
    public void incrementTime(int... index) {
        logicalClock++;
    }

    @Override
    public  Integer getTime() {
        return logicalClock;
    }

     public void updateTime(ClockService c){
        Integer nodeClock = (Integer)c.getTime();
        if(nodeClock > logicalClock)
            logicalClock = nodeClock;
    }

    public void print(){
        System.out.print(logicalClock+" ");
    }
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    @Override
    public ClockService getClockService() {
        return new LogicalClock(logicalClock);
    }
}
