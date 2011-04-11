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
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;
    public Integer taskLoad;
    public String taskId;
    public String taskSrc;
    Integer sequenceNumber;

    public Task(Integer load, String id, String src) {
        taskId = id;
        taskLoad = load;
        taskSrc = src;
        sequenceNumber = -1;
    }

    public String getSource() {
        return taskSrc;
    }

    public Integer getTaskLoad() {
        return taskLoad;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskLoad(Integer load) {
        taskLoad = load;
    }

    public Integer getSeqNumber() {
        return sequenceNumber;
    }

    public void setSeqNumber(Integer sNum) {
        sequenceNumber = sNum;
    }
}
