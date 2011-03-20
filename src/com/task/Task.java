/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.task;

import java.io.Serializable;

/**
 *
 * @author Divya_PKV
 */
public class Task implements Serializable{
    public Integer taskLoad;

    public Task(Integer load){
        taskLoad = load;
    }

    public Integer getTaskLoad(){
        return taskLoad;
    }

    public void setTaskLoad(Integer load){
        taskLoad = load;
    }
}
