/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.android.tasknet.task;

import java.io.Serializable;

/**
 *
 * @author Divya_PKV
 */
public class TaskResult extends Task implements Serializable {

    private static final long serialVersionUID = 1L;
    Serializable taskResult;
    Integer seqNumber;

    public TaskResult(float processorload, long memoryload, Integer battreyload, 
    		String id, String src, Serializable result, Integer seqNumber) {
    	super(processorload, memoryload, battreyload, id, src);
        taskResult = result;
        this.seqNumber = seqNumber;
    }

    public Serializable getTaskResult() {
        return taskResult;
    }

    public Integer getSeqNumber() {
        return seqNumber;
    }

    public void setSeqNumber(Integer seqNumber) {
        this.seqNumber = seqNumber;
    }

    public String toString(){
        return taskResult.toString();
    }
}
