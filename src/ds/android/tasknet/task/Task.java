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
public class Task implements Serializable{
    private static final long serialVersionUID = 1L;
    public Integer taskLoad;
    public String taskId;

    public Task(Integer load, String id){
        taskId = id;
        taskLoad = load;
    }

    public Integer getTaskLoad(){
        return taskLoad;
    }

    public void setTaskLoad(Integer load){
        taskLoad = load;
    }
}
