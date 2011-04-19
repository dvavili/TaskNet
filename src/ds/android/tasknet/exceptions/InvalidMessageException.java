/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ds.android.tasknet.exceptions;

/**
 * @authors
 * Divya Vavili - dvavili@andrew.cmu.edu
 * Yash Pathak - ypathak@andrew.cmu.edu
 *
 */

/**
 * Exception class to handle conditions when an invalid message is sent/received
 */
public class InvalidMessageException extends Exception{

    private static final long serialVersionUID = 1L;
    String message;
    public InvalidMessageException() {
        super();
        message = "Unknown Message Type";
    }

    public InvalidMessageException(String msg){
        super(msg);
        message = msg;
    }

    /**
     * Returns description of error
     */
    public String getError(){
        return message;
    }

}
