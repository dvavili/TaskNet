/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.android.tasknet.task;

import ds.android.tasknet.config.Node;
import java.util.ArrayList;

/**
 *
 * @author Divya_PKV
 */
public class TaskLookup {

    Task task;
    ArrayList<Node> taskGroup;
    Integer maxSequenceNumber;

    public TaskLookup(Task task) {
        this.task = task;
        this.taskGroup = new ArrayList<Node>();
        this.maxSequenceNumber = task.getSeqNumber();
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task t) {
        task = t;
    }

    public ArrayList<Node> getTaskGroup() {
        return taskGroup;
    }

    public void set(ArrayList<Node> taskgp) {
        taskGroup = taskgp;
    }

    public Integer getSequenceNumber() {
        return maxSequenceNumber;
    }

    public void setSequenceNumber(Integer seqNumber) {
        maxSequenceNumber = seqNumber;
    }

    public Integer nextSequenceNumber() {
        return ++maxSequenceNumber;
    }
}
