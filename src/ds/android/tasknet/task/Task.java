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
    public Integer taskBatteryLoad;
    public Integer promisedTaskBatteryLoad;
    public String taskId;
    public String taskSrc; 
    public boolean inExecution;
    public long promisedTimeStamp;
    Integer sequenceNumber;

    public Task(float processorload, long memoryload, Integer batteryload, String id, String src) {
        taskId = id;
        taskProcessorLoad = processorload;
        taskMemoryLoad = memoryload;
        taskBatteryLoad = batteryload;
        taskSrc = src;
        sequenceNumber = -1;
        promisedTaskBatteryLoad = 0;
        inExecution = false;
    }

    public String getSource() {
        return taskSrc;
    }

    public Integer getTaskBatteryLoad() {
        return taskBatteryLoad;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskBatteryLoad(Integer battreyLoad) {
        taskBatteryLoad = battreyLoad;
    }

    public Integer getSeqNumber() {
        return sequenceNumber;
    }

    public void setSeqNumber(Integer sNum) {
        sequenceNumber = sNum;
    }

	public Integer getPromisedBatteryTaskLoad() {
		return promisedTaskBatteryLoad;
	}

	public void setPromisedTaskBatteryLoad(Integer promisedTaskBatteryLoad) {
		this.promisedTaskBatteryLoad = promisedTaskBatteryLoad;
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
