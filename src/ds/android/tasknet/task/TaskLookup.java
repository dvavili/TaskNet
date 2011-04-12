/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.android.tasknet.task;

import ds.android.tasknet.config.Preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Divya_PKV
 */
public class TaskLookup {

    Task task;
    Map<Integer, TaskChunk> taskGroup;
    Integer maxSequenceNumber;
    Enum<Preferences.TASK_STATUS> status;
    int retry;

    public TaskLookup(Task task) {
        this.task = task;
        this.taskGroup = new HashMap<Integer, TaskChunk>();
        this.maxSequenceNumber = task.getSeqNumber();
        this.status = Preferences.TASK_STATUS.ADVERTISED;
        this.retry = 0;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task t) {
        task = t;
    }

    public Map<Integer, TaskChunk> getTaskGroup() {
        return taskGroup;
    }

    public void setTaskGroup(Map<Integer, TaskChunk> taskgp) {
        taskGroup = taskgp;
    }

    public void addToTaskGroup(Integer seqNumber, TaskChunk taskChunk) {
    	this.taskGroup.put(seqNumber, taskChunk);
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

	public Enum<Preferences.TASK_STATUS> getStatus() {
		return status;
	}

	public void setStatus(Enum<Preferences.TASK_STATUS> status) {
		this.status = status;
		this.retry = 0;
	}

	public int getRetry() {
		return retry;
	}

	public void setRetry(int retry) {
		this.retry = retry;
	}
	
	public void incrRetry() {
		this.retry++;
	}
    
    
}
