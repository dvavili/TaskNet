/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.android.tasknet.task;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author Divya_PKV
 */
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;
    public float taskProcessorLoad;
    public long taskMemoryLoad;
    public Integer taskBattreyLoad;
    public Integer promisedTaskBattreyLoad;
    public String taskId;
    public String taskSrc; 
    public boolean inExecution;
    public long promisedTimeStamp;
    Integer sequenceNumber;

    public Task(float processorload, long memoryload, Integer battreyload, String id, String src) {
        taskId = id;
        taskProcessorLoad = processorload;
        taskMemoryLoad = memoryload;
        taskBattreyLoad = battreyload;
        taskSrc = src;
        sequenceNumber = -1;
        promisedTaskBattreyLoad = 0;
        inExecution = false;
    }

    public String getSource() {
        return taskSrc;
    }

    public Integer getTaskBattreyLoad() {
        return taskBattreyLoad;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskBattreyLoad(Integer battreyLoad) {
        taskBattreyLoad = battreyLoad;
    }

    public Integer getSeqNumber() {
        return sequenceNumber;
    }

    public void setSeqNumber(Integer sNum) {
        sequenceNumber = sNum;
    }

	public Integer getPromisedBattreyTaskLoad() {
		return promisedTaskBattreyLoad;
	}

	public void setPromisedTaskBattreyLoad(Integer promisedTaskBattreyLoad) {
		this.promisedTaskBattreyLoad = promisedTaskBattreyLoad;
	}

	public float getTaskProcessorLoad() {
		return taskProcessorLoad;
	}

	public void setTaskProcessorLoad(float taskProcessorLoad) {
		this.taskProcessorLoad = taskProcessorLoad;
	}

	public long getTaskMemoryLoad() {
		return taskMemoryLoad;
	}

	public void setTaskMemoryLoad(long taskMemoryLoad) {
		this.taskMemoryLoad = taskMemoryLoad;
	}

	public boolean isInExecution() {
		return inExecution;
	}

	public void setInExecution(boolean inExecution) {
		this.inExecution = inExecution;
	}

	public long getPromisedTimeStamp() {
		return promisedTimeStamp;
	}

	public void setPromisedTimeStamp(long promisedTimeStamp) {
		this.promisedTimeStamp = promisedTimeStamp;
	}
        	
	
}
