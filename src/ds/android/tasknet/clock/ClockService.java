/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ds.android.tasknet.clock;

import java.io.Serializable;

/**
 *
 * @author Divya
 */
public abstract class ClockService implements Serializable {
    private static final long serialVersionUID = 1L;
    public abstract void incrementTime(int... index);
    public abstract Object getTime();
    public abstract void print();
    public abstract ClockService getClockService();
}
