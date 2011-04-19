/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.android.tasknet.msgpasser;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author Divya
 */
public class MessageQueue extends ArrayList<MessageQueueEntry> implements Serializable {

    private static final long serialVersionUID = 1L;
    @Override
    public String toString(){
        String printString = "";
        for(int i=0;i<this.size();i++)
            printString = printString + ((MessageQueueEntry)this.get(i)).getTimeStamp().toString();
        return printString;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}
