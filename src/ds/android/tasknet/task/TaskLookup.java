/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.android.tasknet.task;

import ds.android.tasknet.config.Preferences;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Divya_PKV
 */
public class TaskLookup {

    Task task;
    String className,methodName;
    Serializable[] params;
    Map<String, TaskChunk> taskGroup;
    Map<Integer, String> resultTracker;
    Map<Integer, TaskResult> taskResults;
    Integer maxSequenceNumber;
    Enum<Preferences.TASK_STATUS> status;
    int retry;

    public TaskLookup(Task task, String className, String methodName, Serializable[] params) {
        this.task = task;
        this.className = className;
        this.methodName = methodName;
        this.params = params;
        this.taskGroup = new HashMap<String, TaskChunk>();
        this.resultTracker = new HashMap<Integer, String>();
        this.taskResults = new TreeMap<Integer, TaskResult>();
        this.maxSequenceNumber = task.getSeqNumber();
        this.status = Preferences.TASK_STATUS.ADVERTISED;
        this.retry = 0;
    }

    public Task getTask() {
        return task;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public Serializable[] getParams() {
        return params;
    }

    public Map<Integer, String> getResultTracker() {
        return resultTracker;
    }

    public Map<Integer, TaskResult> getTaskResults() {
        return taskResults;
    }

    public void setTaskResults(Map<Integer, TaskResult> taskResults) {
        this.taskResults = taskResults;
    }

    public String printResultTracker() {
        return resultTracker.keySet().toString();
    }

    public void setResultTracker(Map<Integer, String> resultTracker) {
        this.resultTracker = resultTracker;
    }

    public void addToResultTracker(Integer seqNumber, String taskAdvReplyId) {
        this.resultTracker.put(seqNumber, taskAdvReplyId);
    }

    public void removeFromResultTracker(Integer seqNumber) {
        this.resultTracker.remove(seqNumber);
    }

    public void setTask(Task t) {
        task = t;
    }

    public Map<String, TaskChunk> getTaskGroup() {
        return taskGroup;
    }

    public void setTaskGroup(Map<String, TaskChunk> taskgp) {
        taskGroup = taskgp;
    }

    public void addToTaskGroup(String seqNumber, TaskChunk taskChunk) {
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
