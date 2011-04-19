/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.android.tasknet.clock;

import ds.android.tasknet.config.Preferences;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

/**
 *
 * @author Divya
 */
public class VectorClock extends ClockService {

    private static final long serialVersionUID = 1L;
    Vector<Integer> vectorClock;

    public VectorClock() {
        vectorClock = new Vector<Integer>();
    }

    public VectorClock(int numberOfNodes) {
        vectorClock = new Vector<Integer>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; i++) {
//            System.out.println(i);
            vectorClock.add(i, 0);
        }
    }

    private VectorClock(Vector<Integer> vClock) {
        vectorClock = new Vector(vClock);
    }

    @Override
    public void incrementTime(int... index) {
        for (int i : index) {
            vectorClock.set(i, vectorClock.get(i) + 1);
        }
    }

    public ClockService getClockService() {
        return new VectorClock(vectorClock);
    }

    @Override
    public Vector<Integer> getTime() {
        return vectorClock;
    }

    public void setTime(Vector<Integer> vtime) {
        vectorClock = vtime;
    }

    public Integer getTime(int index) {
        return vectorClock.get(index);
    }

    public void updateTime(ClockService c) {
        Vector<Integer> nodeClock = ((VectorClock) c).getTime();
        for (int i = 0; i < Preferences.nodes.size(); i++) {
            if (i != Preferences.host_index) {
                if (nodeClock.get(i) > vectorClock.get(i)) {
                    vectorClock.set(i, nodeClock.get(i));
                }
            }
        }
    }

    public void update(Integer index) {
        vectorClock.setSize(Math.max(vectorClock.size(), index + 1));
        if(vectorClock.get(index)==null)
            vectorClock.set(index, 0);
        else
            vectorClock.set(index, Math.max(vectorClock.get(index), 0));
    }

    public void print() {
        System.out.println(vectorClock);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}
