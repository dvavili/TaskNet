/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ds.android.tasknet.clock;

/**
 *
 * @author Divya
 */
public class ClockFactory {
    public enum ClockType { LOGICAL, VECTOR };
    public static ClockService initializeClock(ClockType clock, int numberOfNodes){
        switch(clock){
            case LOGICAL:
                return new LogicalClock();
            case VECTOR:
                return new VectorClock(numberOfNodes);
        }
        return null;
    }
}
