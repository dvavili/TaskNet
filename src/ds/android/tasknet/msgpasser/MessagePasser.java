package ds.android.tasknet.msgpasser;

import ds.android.tasknet.clock.ClockFactory;
import ds.android.tasknet.clock.ClockFactory.ClockType;
import ds.android.tasknet.clock.ClockService;
import ds.android.tasknet.clock.LogicalClock;
import ds.android.tasknet.clock.VectorClock;
import ds.android.tasknet.config.Node;
import ds.android.tasknet.config.Preferences;
import ds.android.tasknet.exceptions.InvalidMessageException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @authors
 * Divya Vavili - dvavili@andrew.cmu.edu
 * Yash Pathak - ypathak@andrew.cmu.edu
 *
 */
public class MessagePasser extends Thread {

    enum Process_State {

        RELEASED, WANTED, HELD
    };
    Properties prop;
    DatagramSocket udpServerSocket;
    DatagramPacket udpPacketReceived;
    ArrayList<Message> inQueue, delayedInQueue;
    ArrayList<Message> outQueue, delayedOutQueue;
    Vector<MessageQueueEntry> holdBackQueue;
    Vector<MulticastMessage> mutexQueue;
    boolean inDelayed = false, outDelayed = false;
    InetAddress host_ip;
    Integer host_port, numOfTimesOutDelayed = 0, numOfTimesInDelayed = 0;
    byte receiveData[];
    byte sendData[];
    String conf_file, host_name;
    ClockService clock;
    Vector<Integer> causalVector;
    Process_State process_state;
    boolean[] arrMutexAckReceived;
    boolean flag = false;
    boolean bootstrapComplete = false;
    ClockType clockType;

    /**
     * @param configuration_filename
     * @param local_name
     * @param clockType - Enum ClockType
     */
    public MessagePasser(String configuration_filename, String local_name, String IPaddress, ClockType clockType, Integer... numberOfNodes) {
        this(configuration_filename, local_name, IPaddress);
        this.clockType = clockType;
        clock = ClockFactory.initializeClock(clockType, numberOfNodes[0]);
    }

    /**
     * Input:
     * String - configuration file to read the properties of the nodes in the p2p system
     * String - local host name (should be mentioned in the NAMES section of the configuration file
     *
     * Creates a socket for listening for connections and initializes
     * input and output buffers
     */
    public MessagePasser(String configuration_filename, String local_name, String ipAddress) {
        prop = new Properties();
        receiveData = new byte[Preferences.SIZE_OF_BUFFER];
        conf_file = configuration_filename;
        host_name = local_name;
        clockType = ClockType.LOGICAL;

        process_state = Process_State.RELEASED;
        arrMutexAckReceived = new boolean[Preferences.nodes.size()];
        for (int i = 0; i < arrMutexAckReceived.length; i++) {
            arrMutexAckReceived[i] = false;
        }

        try {
            prop.load(new FileInputStream(configuration_filename));
            if (host_name.equalsIgnoreCase(Preferences.LOGGER_NAME)) {
                host_ip = InetAddress.getByName(prop.getProperty("node." + local_name + ".ip"));
                host_port = Integer.parseInt(prop.getProperty("node." + local_name + ".port"));
                udpServerSocket = new DatagramSocket(host_port);
            } else {

                udpServerSocket = new DatagramSocket(new InetSocketAddress("127.0.0.1", 0));
                host_ip = udpServerSocket.getLocalAddress();

                host_port = udpServerSocket.getLocalPort();
                System.out.println(host_name + " " + host_ip + " " + host_port);
            }
            /* Input and Output Buffers
             * Use of delayed input and output buffers improve efficiency
             */
            inQueue = new ArrayList<Message>();
            delayedOutQueue = new ArrayList<Message>();
            delayedInQueue = new ArrayList<Message>();
            outQueue = new ArrayList<Message>();
            mutexQueue = new Vector<MulticastMessage>();
            holdBackQueue = new Vector<MessageQueueEntry>();
            causalVector = new Vector<Integer>();

            for (int i = 0; i < Preferences.nodes.size(); i++) {
                causalVector.add(i, 0);
            }

            /* This thread periodically polls the output buffers
             * to see if there are any messages to be sent
             */
            new Thread() {

                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(10);
                            sendMessages();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }.start();

            new Thread() {

                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(10);
                            processHoldBackQueue();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }.start();

            start();

            if (!host_name.equalsIgnoreCase(Preferences.LOGGER_NAME)) {
                while (!bootstrapComplete) {
                    try {
                        bootstrapNodeProcess();
                        Thread.sleep(Preferences.WAIT_TIME_BEFORE_RETRYING_BOOTSTRAP);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MessagePasser.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + configuration_filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void bootstrapNodeProcess() {
        System.out.println("Sending bootstrapping message: " + Preferences.LOGGER_NAME);
        Node hostNodeDetails = new Node(host_name, host_ip, host_port);
        synchronized (Preferences.nodes) {
            Preferences.nodes.put(host_name, hostNodeDetails);
        }
        Message bootstrapMsg = new Message(Preferences.LOGGER_NAME, "", "", hostNodeDetails, host_name);
        bootstrapMsg.setLogSource(host_name);
        bootstrapMsg.setNormalMsgType(Message.NormalMsgType.BOOTSTRAP);
        sendMsgThroSocket(bootstrapMsg);
    }

    public ClockService getClock() {
        return clock;
    }

    /**
     * Get any messages from the output buffers - outQueue and delayedOutQueue
     * and send them to their destination.
     *
     * NOTE: synchronization of buffers is necessary as they are also accessed
     * by application layer in send() method
     */
    void sendMessages() {
        Message message = null;
        try {
            synchronized (outQueue) {
                if (!outQueue.isEmpty()) {
                    message = outQueue.remove(0);
                    if (!message.getDest().equalsIgnoreCase(Preferences.LOGGER_NAME)) {
                        numOfTimesOutDelayed = 0;
                        outDelayed = true;
                    }
                } else {
                    /* Check for any messages in delayed queue only
                     * if there are no other messages to be sent
                     *
                     * The delayed messages wait until another non delayed message
                     * is sent or if the messages have waited for a long time
                     *
                     * The delay time is mentioned in Preferences file
                     */
                    synchronized (delayedOutQueue) {
                        if (!delayedOutQueue.isEmpty()) {
                            if (outDelayed == false && numOfTimesOutDelayed >= Preferences.delayedEnoughThreshold) {
                                outDelayed = true;
                                numOfTimesOutDelayed = 0;
                            } else {
                                if (outDelayed == false) {
                                    numOfTimesOutDelayed++;
                                    return;
                                }
                            }
                            if (outDelayed == true) {
                                message = delayedOutQueue.remove(0);
//                                System.out.println("Delayed message sent");
                                outDelayed = false;
                            }
                        } else {
                            if (outDelayed == true) {
                                outDelayed = false;
                            }
                            return;
                        }
                    }
                }
            }
            sendMsgThroSocket(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendMsgThroSocket(Message message) {
        ObjectOutputStream oos = null;
        try {
            sendData = null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject((Object) message);
            oos.flush();
            sendData = bos.toByteArray();
            DatagramPacket udpsendPacket = null;
            DatagramSocket udpClientSocket = null;
            prop = new Properties();
            prop.load(new FileInputStream(conf_file));
            udpClientSocket = new DatagramSocket();
            udpClientSocket.setReceiveBufferSize(5000);
            udpClientSocket.setSendBufferSize(5000);
            if (message instanceof MulticastMessage) {
                if (((MulticastMessage) message).getMessageType() == MulticastMessage.MessageType.GET_MUTEX) {
                    synchronized (process_state) {
                        synchronized (arrMutexAckReceived) {
//                            process_state = Process_State.WANTED;
                            arrMutexAckReceived[(Preferences.nodes.get(host_name)).getIndex()] = true;
                        }
                    }
                } else if (((MulticastMessage) message).getMessageType() == MulticastMessage.MessageType.RELEASE_MUTEX) {
                    synchronized (process_state) {
                        synchronized (arrMutexAckReceived) {
                            synchronized (mutexQueue) {
                                process_state = Process_State.RELEASED;
                                for (int i = 0; i < arrMutexAckReceived.length; i++) {
                                    arrMutexAckReceived[i] = false;
                                }
                                while (!mutexQueue.isEmpty()) {
                                    MulticastMessage mutexAckMsg = new MulticastMessage(mutexQueue.remove(0), MulticastMessage.MessageType.MUTEX_ACK);
                                    mutexAckMsg.setDest(mutexAckMsg.getSource());
                                    mutexAckMsg.setSource(host_name);
                                    sendMsgThroSocket(mutexAckMsg);
                                }
                                return;
                            }
                        }
                    }
                }
                if (((MulticastMessage) message).getMessageType() == MulticastMessage.MessageType.UPDATE_STATE
                        || ((MulticastMessage) message).getMessageType() == MulticastMessage.MessageType.RUT
                        || ((MulticastMessage) message).getMessageType() == MulticastMessage.MessageType.MUTEX_ACK) {
                    if (((MulticastMessage) message).getMessageType() == MulticastMessage.MessageType.MUTEX_ACK) {
                        System.out.println("Sending MUTEX_ACK to: " + message.getDest());
                    }
                    if (message.getDest().equalsIgnoreCase(Preferences.LOGGER_NAME)) {
                        udpsendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(prop.getProperty("node." + message.getDest() + ".ip")), Integer.parseInt(prop.getProperty("node." + message.getDest() + ".port")));
                    } else {
                        udpsendPacket = new DatagramPacket(sendData, sendData.length, Preferences.nodes.get(message.getDest()).getAdrress(), Preferences.nodes.get(message.getDest()).getNodePort());
                    }
                    //if source/dest is logger dont simulate energy consumption
                    if (!message.getDest().equals(Preferences.LOGGER_NAME)
                            && !message.getSource().equals(Preferences.LOGGER_NAME)) {
                        Preferences.nodes.get(host_name).decrBatteryLevel(Preferences.LOAD_SPENT_IN_COMMUNICATION_SEND);
                    }
                    udpClientSocket.send(udpsendPacket);
                } else {
                    Object[] node_names = Preferences.node_addresses.keySet().toArray();
                    for (int i = 0; i < node_names.length; i++) {
                        if (!host_name.equalsIgnoreCase((String) node_names[i])
                                && !Preferences.crashNode.equalsIgnoreCase((String) node_names[i])) {
                            if (((MulticastMessage) message).getMessageType() == MulticastMessage.MessageType.GET_MUTEX) {
                                System.out.println("Sending GET_MUTEX to: " + message.getDest());
                            }
                            if (message.getDest().equalsIgnoreCase(Preferences.LOGGER_NAME)) {
                                udpsendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(prop.getProperty("node." + message.getDest() + ".ip")), Integer.parseInt(prop.getProperty("node." + message.getDest() + ".port")));
                            } else {
                                udpsendPacket = new DatagramPacket(sendData, sendData.length, Preferences.nodes.get(node_names[i]).getAdrress(), Preferences.nodes.get(node_names[i]).getNodePort());
                            }
                            //if source/dest is logger dont simulate energy consumption
                            if (!message.getDest().equals(Preferences.LOGGER_NAME)
                                    && !message.getSource().equals(Preferences.LOGGER_NAME)) {
                                Preferences.nodes.get(host_name).decrBatteryLevel(Preferences.LOAD_SPENT_IN_COMMUNICATION_SEND);
                            }
                            udpClientSocket.send(udpsendPacket);
                        }
                    }
                }
            } else {
                if (message.getDest().equalsIgnoreCase(Preferences.LOGGER_NAME)) {
                    udpsendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(prop.getProperty("node." + message.getDest() + ".ip")), Integer.parseInt(prop.getProperty("node." + message.getDest() + ".port")));
                } else {
                    udpsendPacket = new DatagramPacket(sendData, sendData.length, Preferences.nodes.get(message.getDest()).getAdrress(), Preferences.nodes.get(message.getDest()).getNodePort());
                }
                //remove this if letter
                //if source/dest is logger dont simulate energy consumption
                if (!message.getDest().equals(Preferences.LOGGER_NAME)
                        && !message.getSource().equals(Preferences.LOGGER_NAME)
                        && Preferences.nodes.get(host_name) != null) {
                    Preferences.nodes.get(host_name).decrBatteryLevel(Preferences.LOAD_SPENT_IN_COMMUNICATION_SEND);
                }
                udpClientSocket.send(udpsendPacket);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                oos.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void setProcessState(String state) {
        if (state.equalsIgnoreCase("Wanted")) {
            process_state = Process_State.WANTED;
        } else if (state.equalsIgnoreCase("Released")) {
            process_state = Process_State.RELEASED;
        }
    }

    /**
     * Process messages from application layer according to their type
     * ID1/Kind1 - drop - not enqueued in any buffers
     * ID2/Kind2 - delay - enqueued in delayedOutBuffer
     * ID3/Kind3 - duplicate - enqueued twice in outBuffer
     * Any other combination of ID and Kind are just enqueued into outBuffer
     * Also ID of the message takes precedence over Kind
     */
    public void send(Message msg) throws InvalidMessageException {
        String msgType = prop.getProperty("is." + msg.getId());
        updateTime(msg, "send");
        if (msgType == null) {
            msgType = prop.getProperty("ir." + msg.getId());
            if (msgType != null) {
                msgType = "other";
            } else {
                msgType = prop.getProperty("ks." + msg.getKind());
                if (msgType == null) {
                    msgType = "other";
                }
            }
        }
        if (msgType.equalsIgnoreCase("drop")) {
//            System.out.println("Sent Message dropped");
            if (Preferences.logDrop) {
                synchronized (outQueue) {
                    msg.setLogMessage("Message from " + host_name + " to " + msg.getDest() + " dropped");
                    outQueue.add(new Message(Preferences.LOGGER_NAME, "log", "log", msg, host_name));
                }
            }
            return;
        } else if (msgType.equalsIgnoreCase("delay")) {
//            System.out.println("Sent Message added to delay Q");
            synchronized (delayedOutQueue) {
                delayedOutQueue.add(msg);
                if (Preferences.logDelay) {
                    synchronized (outQueue) {
                        msg.setLogMessage("Message from " + host_name + " to " + msg.getDest() + " delayed");
                        outQueue.add(new Message(Preferences.LOGGER_NAME, "log", "log", msg, host_name));
                    }
                }
            }
        } else if (msgType.equalsIgnoreCase("duplicate")) {
//            System.out.println("Sent Message duplicated");
            synchronized (outQueue) {
                outQueue.add(msg);
                if (!(msg instanceof MulticastMessage)) {
                    outQueue.add(msg);
                }
                if (Preferences.logDuplicate) {
                    msg.setLogMessage("Message from " + host_name + " to " + msg.getDest() + " duplicated");
                    outQueue.add(new Message(Preferences.LOGGER_NAME, "log", "log", msg, host_name));
                }
            }
        } else {
            synchronized (outQueue) {
                outQueue.add(msg);
                if (Preferences.logEvent && !msg.getDest().equalsIgnoreCase(Preferences.LOGGER_NAME)) {
                    msg.setToBeLogged(true);
                    msg.setLogMessage("Message sent from " + host_name + " to " + msg.getDest());
                    outQueue.add(new Message(Preferences.LOGGER_NAME, "log", "log", msg, host_name));
                }
            }
        }
    }

    /**
     * Checks for any data received from the buffers and processes them accordingly
     */
    @Override
    public void run() {
        System.out.println("Listening: " + host_name + " " + host_ip + " " + host_port);
        while (true) {
            receiveData = new byte[Preferences.SIZE_OF_BUFFER];
            ObjectInputStream ois = null;
            try {
                udpPacketReceived = new DatagramPacket(receiveData, receiveData.length, host_ip, host_port);
                udpServerSocket.setReceiveBufferSize(5000);
                udpServerSocket.setSendBufferSize(5000);
                udpServerSocket.receive(udpPacketReceived);
                ByteArrayInputStream bis = new ByteArrayInputStream(udpPacketReceived.getData());
                ois = new ObjectInputStream(bis);
                final Message msg = (Message) (ois.readObject());

                //if source/dest is logger dont simulate energy consumption
                if (!msg.getDest().equals(Preferences.LOGGER_NAME)
                        && !msg.getSource().equals(Preferences.LOGGER_NAME)
                        && Preferences.nodes.get(host_name) != null) {
                    Preferences.nodes.get(host_name).decrBatteryLevel(Preferences.LOAD_SPENT_IN_COMMUNICATION_RECEIVE);
                }

                if (msg instanceof MulticastMessage) {
                    (new Thread() {

                        public void run() {
                            deliverMessage((MulticastMessage) msg);
                        }
                    }).start();
                } else {
                    (new Thread() {

                        public void run() {
                            try {
                                processReceivedMessage(msg);
                            } catch (InvalidMessageException ex) {
                                Logger.getLogger(MessagePasser.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }).start();
                }
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    ois.close();
                } catch (IOException ex) {
                    Logger.getLogger(MessagePasser.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Process messages from network layer according to their type
     * ID4/Kind4 - drop - not enqueued in any buffers
     * ID5/Kind5 - delay - enqueued in delayedInBuffer
     * ID6/Kind6 - duplicate - enqueued twice in inBuffer
     * Any other messages with combination of ID and Kind are enqueued in inQueue
     * Also ID of the message takes precedence over Kind
     */
    void processReceivedMessage(Message msg) throws InvalidMessageException {
        if (!msg.getDest().equalsIgnoreCase("logger")) {
            updateTime(msg, "receive");
            switch (msg.getNormalMsgType()) {
                case BOOTSTRAP_NODE_LIST:
                    for (Node n : ((HashMap<String, Node>) msg.getData()).values()) {
                        if (!Preferences.node_addresses.containsKey(n.getName())) {
                            try {
                                Preferences.nodes.put(n.getName(), n);
                                Preferences.node_addresses.put(n.getName(), n.getAdrress());
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }
                            Preferences.node_names.put(n.getIndex(), n.getName());
                            causalVector.setSize(Math.max(causalVector.size(), n.getIndex() + 1));
                            causalVector.set(n.getIndex(), 0);
                        }
                    }
                    Preferences.host_index = Preferences.nodes.get(host_name).getIndex();
                    clock = ClockFactory.initializeClock(clockType, Preferences.nodes.size());
                    announcePresence();
                    bootstrapComplete = true;
                    return;
                case ALIVE:
                    Node newNode = (Node) msg.getData();
                    Preferences.nodes.put(newNode.getName(), newNode);
                    return;
                case REMOVE_NODE:
                    synchronized(Preferences.nodes){
                        Preferences.node_names.remove(Preferences.nodes.get((String)msg.getData()).getIndex());
                        Preferences.nodes.remove((String)msg.getData());
                        Preferences.node_addresses.remove((String)msg.getData());
                    }
                    return;
            }
        }
        String msgType = prop.getProperty("is." + msg.getId());
        if (msgType != null) {
            msgType = "other";
        } else {
            msgType = prop.getProperty("ir." + msg.getId());
            if (msgType == null) {
                msgType = prop.getProperty("ks." + msg.getKind());
                if (msgType == null) {
                    msgType = prop.getProperty("kr." + msg.getKind());
                    if (msgType == null) {
                        msgType = "other";
                    }
                } else {
                    msgType = "other";
                }
            }
        }
        if (msgType.equalsIgnoreCase("drop")) {
//            System.out.println("Received msg dropped");
            if (Preferences.logDrop || msg.isToBeLogged()) {
                synchronized (outQueue) {
                    msg.setLogMessage("Received message dropped in " + host_name);
                    outQueue.add(new Message(Preferences.LOGGER_NAME, "log", "log", msg, host_name));
                }
            }
            return;
        } else if (msgType.equalsIgnoreCase("delay")) {
//            System.out.println("Received msg put in delay Q");
            synchronized (delayedInQueue) {
                delayedInQueue.add(msg);
                if (Preferences.logDelay || msg.isToBeLogged()) {
                    synchronized (outQueue) {
                        msg.setLogMessage("Received message delayed in " + host_name);
                        outQueue.add(new Message(Preferences.LOGGER_NAME, "log", "log", msg, host_name));
                    }
                }
            }
        } else if (msgType.equalsIgnoreCase("duplicate")) {
//            System.out.println("Received msg duplicated");
            synchronized (inQueue) {
                inQueue.add(msg);
                if (!(msg instanceof MulticastMessage)) {
                    inQueue.add(msg);
                }
                if (Preferences.logDuplicate || msg.isToBeLogged()) {
                    msg.setLogMessage("Received message duplicated in " + host_name);
                    outQueue.add(new Message(Preferences.LOGGER_NAME, "log", "log", msg, host_name));
                }
            }
        } else {
//            System.out.println("Received msg added to queue");
            synchronized (inQueue) {
                inQueue.add(msg);
                if (Preferences.logEvent || msg.isToBeLogged()) {
                    msg.setLogMessage("Message received in " + host_name);
                    outQueue.add(new Message(Preferences.LOGGER_NAME, "log", "log", msg, host_name));
                }
            }
        }
    }

    /**
     * Get any messages from the input buffers - inQueue and delayedInQueue
     *
     * NOTE: synchronization of buffers is necessary as they are also accessed
     * by processedReceivedMessage(Message) method
     */
    public Message receive() {
        Message message = null;
        try {
            synchronized (inQueue) {
                if (!inQueue.isEmpty()) {
                    inDelayed = true;
                    numOfTimesInDelayed = 0;
                    message = inQueue.remove(0);
                } else {
                    /* Check for any messages in delayed queue only
                     * if there are no other messages to be received from inQueue
                     *
                     * The delayed messages wait until another non delayed message
                     * is received or if the messages have waited for a long time
                     *
                     * The delay time is mentioned in Preferences file
                     */
                    synchronized (delayedInQueue) {
                        if (!delayedInQueue.isEmpty()) {
                            if (inDelayed == false && numOfTimesInDelayed >= Preferences.receiverDelayedEnoughThreshold) {
                                inDelayed = true;
                                numOfTimesInDelayed = 0;
                            } else {
                                if (inDelayed == false) {
                                    numOfTimesInDelayed++;
                                    return null;
                                }
                            }
                            if (inDelayed == true) {
                                message = delayedInQueue.remove(0);
//                                System.out.println("Received delayed message");
                                inDelayed = false;
                            }
                        } else {
                            if (inDelayed == true) {
                                inDelayed = false;
                            }
                            return null;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return message;
    }

    public void updateTime(Message msg, String process) {
        if (process.equalsIgnoreCase("receive") && msg instanceof TimeStampedMessage) {
            if (clock instanceof VectorClock) {
                ((VectorClock) clock).updateTime(((TimeStampedMessage) msg).getClockService());
                ((TimeStampedMessage) msg).setEventID("E" + (Preferences.host_index + 1)
                        + ((((VectorClock) clock).getTime(Preferences.host_index)) + 1));
            } else {

                Integer nextId = new Integer(clock.getTime().toString()) + 1;
                ((TimeStampedMessage) msg).setEventID("E" + (Preferences.host_index + 1)
                        + nextId);
                ((LogicalClock) clock).updateTime(((TimeStampedMessage) msg).getClockService());
            }
            clock.incrementTime((Preferences.nodes.get(host_name)).getIndex());
            ((TimeStampedMessage) msg).setClock(clock);

        }
        if (msg instanceof TimeStampedMessage) {
            clock.print();
        }
    }

    private void deliverMessage(MulticastMessage msg) {
        msg.setMsgReceived((Preferences.nodes.get(host_name)).getIndex());
        /* If the message is a normal msg from application layer
         * 1. Add the hold back queue
         * 2. Send an acknowledgement to all the processes in the group
         *      Acknowledgement kind and id are "multicast"
         */
        switch (msg.getMessageType()) {
            case TASK_ADV:
                boolean[] received = new boolean[Preferences.nodes.size()];
                for (int i = 0; i < received.length; i++) {
                    received[i] = true;
                }
                msg.setMsgReceivedArray(received);
                synchronized (holdBackQueue) {
                    holdBackQueue.add(new MessageQueueEntry(msg,
                            (Vector) msg.getClockService().getTime(),
                            msg.getMsgReceivedArray()));
                }
                break;
            case NORMAL:
                synchronized (holdBackQueue) {
                    holdBackQueue.add(new MessageQueueEntry(msg,
                            (Vector) msg.getClockService().getTime(),
                            msg.getMsgReceivedArray()));
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MessagePasser.class.getName()).log(Level.SEVERE, null, ex);
                }
                MulticastMessage ackMsg = new MulticastMessage(msg, MulticastMessage.MessageType.ACK);
                sendMsgThroSocket(ackMsg);
                break;

            case ACK:
                updateMsgReceivedArray(msg);
                break;

            case ALIVE:
                System.out.println("Getting presence message");
                Node newNode = (Node) msg.getData();
                Preferences.nodes.put(newNode.getName(), newNode);
                try {
                    Preferences.node_addresses.put(newNode.getName(), newNode.getAdrress());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                Preferences.node_names.put(newNode.getIndex(), newNode.getName());
                causalVector.setSize(Math.max(causalVector.size(), newNode.getIndex() + 1));
                causalVector.set(newNode.getIndex(), 0);
                ((VectorClock) clock).update(newNode.getIndex());
                MulticastMessage updateStateMsg;
                Vector<MulticastMessage> hbqArray = new Vector<MulticastMessage>();
                synchronized (holdBackQueue) {
                    for (int i = 0; i < holdBackQueue.size(); i++) {
                        hbqArray.add(holdBackQueue.get(i).getMessage());
                    }
                }
                for (int i = 0; i < hbqArray.size(); i++) {
                    String source = msg.getSource();
                    updateStateMsg = new MulticastMessage(hbqArray.get(i), MulticastMessage.MessageType.UPDATE_STATE);
                    updateStateMsg.setDest(source);
                    updateStateMsg.setSource(hbqArray.get(i).getSource());
                    sendMsgThroSocket(updateStateMsg);
                }
                break;

            case UPDATE_STATE:
                updateHoldBackQueue(msg);
                break;

            case RUT:
                msg.msgReceived[(Preferences.nodes.get(host_name)).getIndex()] = true;
                msg.msgType = MulticastMessage.MessageType.UPDATE_STATE;
                updateHoldBackQueue((MulticastMessage) msg);
                processHoldBackQueue();
                break;

            case GET_MUTEX:
//                System.out.println("Received GET_MUTEX from " + msg.getSource());
                synchronized (process_state) {
                    if (process_state == Process_State.RELEASED) {
                        MulticastMessage mutexAckMsg = new MulticastMessage(msg, MulticastMessage.MessageType.MUTEX_ACK);
                        mutexAckMsg.setDest(msg.getSource());
                        mutexAckMsg.setSource(host_name);
                        sendMsgThroSocket(mutexAckMsg);
                    } else if (process_state == Process_State.HELD) {
                        synchronized (mutexQueue) {
                            mutexQueue.add(msg);
                        }
                    } else if (process_state == Process_State.WANTED) {
                        if (msg.getId().equalsIgnoreCase("id2")) {
                            if (((Integer) (msg.clock.getTime()) > (Integer) (clock.getTime()))) {
//                                System.out.println("WANT: Message from " + msg.getSource() + " added to mutexQueue");
                                synchronized (mutexQueue) {
                                    mutexQueue.add(msg);
                                }
                            }
                            if (((Integer) (msg.clock.getTime())).equals((Integer) (clock.getTime()))) {
                                if (Preferences.nodes.get(host_name).getIndex() < Preferences.nodes.get(msg.getSource()).getIndex()) {
//                                    System.out.println("WANT: Message from " + msg.getSource() + " added to mutexQueue");
                                    synchronized (mutexQueue) {
                                        mutexQueue.add(msg);
                                    }
                                } else {
                                    MulticastMessage mutexAckMsg = new MulticastMessage(msg, MulticastMessage.MessageType.MUTEX_ACK);
                                    mutexAckMsg.setDest(msg.getSource());
                                    mutexAckMsg.setSource(host_name);
                                    sendMsgThroSocket(mutexAckMsg);
                                }
                            }
                            if (((Integer) (msg.clock.getTime()) < (Integer) (clock.getTime()))) {
                                MulticastMessage mutexAckMsg = new MulticastMessage(msg, MulticastMessage.MessageType.MUTEX_ACK);
                                mutexAckMsg.setDest(msg.getSource());
                                mutexAckMsg.setSource(host_name);
                                sendMsgThroSocket(mutexAckMsg);
                            }
                        } else {
                            if (!delayedOutQueue.isEmpty()) {
                                if (((Integer) (msg.clock.getTime()) > (Integer) (clock.getTime()))) {
                                    synchronized (mutexQueue) {
                                        mutexQueue.add(msg);
                                    }
                                }
                                if (((Integer) (msg.clock.getTime())).equals((Integer) (clock.getTime()))) {
                                    if (Preferences.nodes.get(host_name).getIndex() < Preferences.nodes.get(msg.getSource()).getIndex()) {
                                        synchronized (mutexQueue) {
                                            mutexQueue.add(msg);
                                        }
                                    } else {
                                        MulticastMessage mutexAckMsg = new MulticastMessage(msg, MulticastMessage.MessageType.MUTEX_ACK);
                                        mutexAckMsg.setDest(msg.getSource());
                                        mutexAckMsg.setSource(host_name);
                                        sendMsgThroSocket(mutexAckMsg);
                                    }
                                }
                                if (((Integer) (msg.clock.getTime()) < (Integer) (clock.getTime()))) {
                                    MulticastMessage mutexAckMsg = new MulticastMessage(msg, MulticastMessage.MessageType.MUTEX_ACK);
                                    mutexAckMsg.setDest(msg.getSource());
                                    mutexAckMsg.setSource(host_name);
                                    sendMsgThroSocket(mutexAckMsg);
                                }
                            }
                            if (((Integer) (msg.clock.getTime()) >= (Integer) (clock.getTime()))) {
                                synchronized (mutexQueue) {
                                    mutexQueue.add(msg);
                                }
                            } else {
                                MulticastMessage mutexAckMsg = new MulticastMessage(msg, MulticastMessage.MessageType.MUTEX_ACK);
                                mutexAckMsg.setDest(msg.getSource());
                                mutexAckMsg.setSource(host_name);
                                sendMsgThroSocket(mutexAckMsg);
                            }
                        }
                    }
                }
                break;

            case MUTEX_ACK:
//                System.out.println("Received MUTEX_ACK from " + msg.getSource());
                boolean mutextAcquired = true;
                if (process_state == Process_State.WANTED) {
                    synchronized (arrMutexAckReceived) {
                        arrMutexAckReceived[Preferences.nodes.get(msg.source).getIndex()] = true;
                        for (boolean b : arrMutexAckReceived) {
                            if (b == false) {
                                mutextAcquired = false;
                            }
                        }
                    }
                    if (mutextAcquired == true) {
                        synchronized (process_state) {
                            process_state = Process_State.HELD;
                            synchronized (inQueue) {
                                inQueue.add(msg);
                            }
                        }
                    }
                }
                break;
        }
    }

    private void processHoldBackQueue() {
        synchronized (holdBackQueue) {
            MessageQueueEntry mqe = findSmallestInQueue(holdBackQueue);
            if (mqe != null) {
                for (boolean b : mqe.getMsgReceivedArray()) {
                    if (b == false) {
                        return;
                    }
                }
                synchronized (inQueue) {
                    synchronized (causalVector) {
                        if (causalVector.get(Preferences.nodes.get(mqe.getMessage().getSource()).getIndex())
                                >= new Integer(((Vector) mqe.getMessage().getClockService().getTime()).get(Preferences.nodes.get(mqe.getMessage().getSource()).getIndex()).toString())) {
                            holdBackQueue.remove(mqe);
//                            System.out.println("Duplicate message removed:" + mqe.timeStamp);
                        } else {
                            if (causalVector.get(Preferences.nodes.get(mqe.getMessage().getSource()).getIndex()) + 1
                                    == new Integer(((Vector) mqe.getMessage().getClockService().getTime()).get(Preferences.nodes.get(mqe.getMessage().getSource()).getIndex()).toString())) {
                                inQueue.add(mqe.getMessage());
                                causalVector.setElementAt(new Integer(mqe.getTimeStamp().get(Preferences.nodes.get(mqe.getMessage().getSource()).getIndex()).toString()), Preferences.nodes.get(mqe.getMessage().getSource()).getIndex());
                                holdBackQueue.remove(mqe);
//                                System.out.println("Message removed from hold back queue: " + mqe.getTimeStamp() + " " + causalVector);
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateMsgReceivedArray(MulticastMessage msg) {
        Iterator arrayIterator;
        MessageQueueEntry mqe = null;
        boolean[] mqeArray = null, msgArray = null;
        if (msg.getSource().equalsIgnoreCase(host_name)) {
            return;
        } else {
            arrayIterator = holdBackQueue.iterator();
        }
        /*
         * Update the received boolean array
         */
        while (arrayIterator.hasNext()) {
            mqe = (MessageQueueEntry) arrayIterator.next();
            int msgSrcIndex = Preferences.nodes.get(msg.getSource()).getIndex();
            if (msg.getSource().equalsIgnoreCase(mqe.message.getSource())
                    && (((Vector) msg.getClockService().getTime()).elementAt(msgSrcIndex).toString().equalsIgnoreCase(
                    mqe.getTimeStamp().elementAt(msgSrcIndex).toString()))) {
                msgArray = msg.getMsgReceivedArray();
                mqeArray = mqe.getMsgReceivedArray();
                for (int i = 0; i
                        < mqeArray.length; i++) {
                    if (mqeArray[i] == true) {
                        msgArray[i] = true;
                    } else if (msgArray[i] == true) {
                        mqeArray[i] = true;
                    }
                }
                msg.setMsgReceivedArray(msgArray);
                mqe.setMsgReceivedArray(mqeArray);
            }
        }
        /*
         * Proceed if there is an entry in the hold back queue
         */
        if (mqe != null) {
            /*
             * Return back if all the nodes have not acknowledged the message
             */
            for (boolean b : mqe.getMsgReceivedArray()) {
                if (b == false) {
                    return;
                }
            }
            /*
             * If all the nodes have acknowledged the message and
             * if the node is the sender, remove the entry from the sentQueue
             * else remove the entry from the holdBackQueue
             */
            synchronized (holdBackQueue) {
                if (!isSmallestInQueue(holdBackQueue, mqe)) {
                    MessageQueueEntry smallest = findSmallestInQueue(holdBackQueue);
                    if (smallest != null) {
                        boolean[] msgReceivedArray = smallest.getMsgReceivedArray();
                        for (int i = 0; i < msgReceivedArray.length; i++) {
                            if (!msgReceivedArray[i]) {
                                System.out.println("Sending RUT message to: " + Preferences.node_names.get(i)
                                        + " for message: " + smallest.timeStamp);
                                MulticastMessage rutMsg = new MulticastMessage(smallest.getMessage(), MulticastMessage.MessageType.RUT);
                                rutMsg.setDest(Preferences.node_names.get(i));
                                sendMsgThroSocket(rutMsg);
                            }
                        }
                    }
                }
            }
        }
    }

    private MessageQueueEntry findSmallestInQueue(Vector<MessageQueueEntry> queue) {
        MessageQueueEntry smallest = null;
        if (queue != null) {
            synchronized (holdBackQueue) {
                Iterator<MessageQueueEntry> hbqIterator = queue.iterator();
                boolean first = true;
                while (hbqIterator.hasNext()) {
                    MessageQueueEntry mqe = hbqIterator.next();
                    if (first) {
                        smallest = mqe;
                        first = false;
                    }
                    if (compareTimestamps(smallest.getTimeStamp(), mqe.getTimeStamp()) > 0) {
                        smallest = mqe;
                    }
                }
            }
        }
        return smallest;
    }

    private boolean isSmallestInQueue(Vector<MessageQueueEntry> messageQueue, MessageQueueEntry mqe) {
        Iterator arrayIterator = messageQueue.iterator();
        boolean isSmallest = true;
        /*
         * Find the Message Queue Entry with the smallest timestamp
         */
        while (arrayIterator.hasNext()) {
            MessageQueueEntry nextMQE = (MessageQueueEntry) arrayIterator.next();
            if (compareTimestamps(mqe.getTimeStamp(), nextMQE.getTimeStamp()) > 0) {
                isSmallest = false;
                return isSmallest;
            }
        }
        return isSmallest;
    }

    /**
     * Compares two time stamps in the queue --> used to ensure causal ordering
     * @param vFirst
     * @param vSecond
     * @return
     */
    private int compareTimestamps(Vector<Integer> vFirst, Vector<Integer> vSecond) {
        int result = 0;
        for (int i = 0; i
                < Preferences.nodes.size(); i++) {
            if (vFirst.get(i) < vSecond.get(i)) {
                if (result > 0) {
                    result = 0;
                    break;
                } else {
                    result--;
                }
            } else if (vFirst.get(i) > vSecond.get(i)) {
                if (result < 0) {
                    result = 0;
                    break;
                } else {
                    result++;
                }
            }
        }
        return result;
    }

    private void announcePresence() {
        Collection<Node> nodes = Preferences.nodes.values();
        Message presenceMsg = new Message("", "", "", Preferences.nodes.get(host_name), host_name);
        presenceMsg.setSource(host_name);
        presenceMsg.setNormalMsgType(Message.NormalMsgType.ALIVE);
        for (Node n : nodes) {
            if (!n.getName().equalsIgnoreCase(host_name)) {
                presenceMsg.setDest(n.getName());
                sendMsgThroSocket(presenceMsg);
            }
        }
        System.out.println("Announcing presence");
    }

    private void updateHoldBackQueue(MulticastMessage msg) {
        Iterator<MessageQueueEntry> hbqIterator;
        synchronized (holdBackQueue) {
            boolean hasEntry = false;
            hbqIterator = holdBackQueue.iterator();
            while (hbqIterator.hasNext()) {
                MessageQueueEntry hbqMQE = hbqIterator.next();
                if (((Vector) msg.getClockService().getTime()).toString().equalsIgnoreCase(hbqMQE.getTimeStamp().toString())) {
                    hasEntry = true;
                    break;
                }
            }
            if (!hasEntry) {
                int msgSrcIndex = Preferences.nodes.get(msg.getSource()).getIndex();
                int msgSeqNum = new Integer(((Vector) msg.getClockService().getTime()).get(msgSrcIndex).toString());
                if ((msg.msgType != MulticastMessage.MessageType.UPDATE_STATE && causalVector.get(msgSrcIndex) >= msgSeqNum)
                        || (msg.msgType == MulticastMessage.MessageType.UPDATE_STATE && causalVector.get(msgSrcIndex) < msgSeqNum)) {
                    holdBackQueue.add(new MessageQueueEntry(msg,
                            (Vector) msg.getClockService().getTime(),
                            msg.getMsgReceivedArray()));
                    updateMsgReceivedArray(msg);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MessagePasser.class.getName()).log(Level.SEVERE, null, ex);
                }
                MulticastMessage ackMsg = new MulticastMessage(msg, MulticastMessage.MessageType.ACK);
                sendMsgThroSocket(ackMsg);
            }
        }
    }
}
