package ds.android.tasknet.msgpasser;

import ds.android.tasknet.clock.ClockService;
import ds.android.tasknet.config.Preferences;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 * @author Divya
 */
public class MulticastMessage extends TimeStampedMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    public enum MessageType {
        NORMAL, UPDATE_STATE, RUT, ALIVE, ACK, GET_MUTEX, RELEASE_MUTEX, MUTEX_ACK, TASK_ADV
    };
    boolean[] msgReceived;
    MessageType msgType;

    public MulticastMessage(String dest, String kind, String id, Serializable data, ClockService c,
            boolean newTimeStamp, MessageType mType, String src) {
        super(dest, kind, id, data, c, newTimeStamp);
        msgReceived = new boolean[Preferences.nodes.size()];
        for (int i = 0; i < msgReceived.length; i++) {
            if (i == Preferences.host_index) {
                msgReceived[i] = true;
            } else {
                msgReceived[i] = false;
            }
        }
        msgType = mType;
        source = src;
    }

    public MulticastMessage(MulticastMessage m, MessageType type) {
        super("", Preferences.MULTICAST_MESSAGE, Preferences.MULTICAST_MESSAGE, m.getData(), m.getClockService(), false);
        msgReceived = m.getMsgReceivedArray();
        msgType = type;
        source = m.getSource();
    }

    boolean canBeDelivered() {
        for (int i = 0; i < msgReceived.length; i++) {
            if (msgReceived[i] == false) {
                return false;
            }
        }
        return true;
    }

    public MessageType getMessageType() {
        return msgType;
    }

    boolean[] getMsgReceivedArray() {
        return msgReceived;
    }

    void setMsgReceived(int index) {
        msgReceived[index] = true;
    }

    void setMsgReceivedArray(boolean[] boolArray) {
        msgReceived = boolArray;
    }

    void setData(Object data) {
        this.data = data.toString();
    }

    void printMsgReceivedArray() {
        System.out.print("[");
        for (boolean b : msgReceived) {
            System.out.print(b + ",");
        }
        System.out.println("]");
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}
