package ds.android.tasknet.infrastructure;

import ds.android.tasknet.application.SampleApplication;
import ds.android.tasknet.clock.ClockFactory;
import ds.android.tasknet.config.Node;
import ds.android.tasknet.config.Preferences;
import ds.android.tasknet.exceptions.InvalidMessageException;
import ds.android.tasknet.msgpasser.Message;
import ds.android.tasknet.msgpasser.MessagePasser;
import ds.android.tasknet.msgpasser.MulticastMessage;
import ds.android.tasknet.task.DistributedTask;
import ds.android.tasknet.task.Task;
import ds.android.tasknet.task.TaskAdvReply;
import ds.android.tasknet.task.TaskChunk;
import ds.android.tasknet.task.TaskLookup;
import ds.android.tasknet.task.TaskResult;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * @authors
 * Divya Vavili - dvavili@andrew.cmu.edu
 * Yash Pathak - ypathak@andrew.cmu.edu
 *
 */
/**
 * Test Harness to test MessagePasser
 */
public class MulticastInfrastructure implements ActionListener, ItemListener {

    int numOfNodes = 0, host_index = 0;
    JScrollPane scrollPane;
    JPanel mainPanel, scrollPanel, btnPanel, taskPanel;
    JLabel host_label;
    JTextArea taMessages;
    JTextField tfSend;
    JFrame mainFrame;
    JButton btnAdvertiseTask;
    MessagePasser mp;
    String host;
    String conf_file;
    String clockType;
    Properties prop;
    ClockFactory.ClockType clock;
    public static int taskNum = 0;
    HashMap<String, TaskLookup> taskLookups;
    Map<String, Map<Integer, TaskResult>> taskResults = new HashMap<String, Map<Integer, TaskResult>>();
    Integer taskAdvReplyId = 0;

    public MulticastInfrastructure(String host_name, String conf_file, String clockType) {
        prop = new Properties();
        Preferences.setHostDetails(conf_file, host_name);
        try {
            prop.load(new FileInputStream(conf_file));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (clockType.equalsIgnoreCase("default")) {
            clockType = prop.getProperty("clock.type");
        }
        if (clockType.equalsIgnoreCase("logical")) {
            clock = ClockFactory.ClockType.LOGICAL;
            mp = new MessagePasser(conf_file, host_name, ClockFactory.ClockType.LOGICAL, 1);
        } else if (clockType.equalsIgnoreCase("vector")) {
            clock = ClockFactory.ClockType.VECTOR;
            mp = new MessagePasser(conf_file, host_name, ClockFactory.ClockType.VECTOR, Preferences.nodes.size());
        }
        this.host = host_name;
        this.conf_file = conf_file;
        this.clockType = clockType;
        taskLookups = new HashMap<String, TaskLookup>();
        buildUI(conf_file);
    }

    void buildUI(String conf_file) {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        host_label = new JLabel(host.toUpperCase());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(host_label, gbc);

        taskPanel = new JPanel(new FlowLayout());
        taskPanel.add(new JLabel("Enter the task load: "), gbc);

        tfSend = new JTextField(40);
        taskPanel.add(tfSend);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(taskPanel, gbc);

        StringTokenizer nodes = new StringTokenizer(prop.getProperty("NAMES"), ",");
        numOfNodes = nodes.countTokens();
        for (int i = 0; i < numOfNodes; i++) {
            String nodeStr = nodes.nextToken();
            if (nodeStr.equalsIgnoreCase(host) || nodeStr.equalsIgnoreCase(Preferences.LOGGER_NAME)) {
                numOfNodes--;
                host_index = i;
                if (i < numOfNodes) {
                    nodeStr = nodes.nextToken();
                } else {
                    break;
                }
            }
        }

        btnAdvertiseTask = new JButton("Advertise task");
        btnAdvertiseTask.addActionListener(this);
        btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(btnAdvertiseTask);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(btnPanel, gbc);

        taMessages = new JTextArea(10, 50);
        taMessages.setEditable(false);
        scrollPane = new JScrollPane(taMessages,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(scrollPane, gbc);

        mainFrame = new JFrame("Multicast Lab");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setResizable(false);
        mainFrame.setSize(600, 300);
        mainFrame.add(mainPanel);
        mainFrame.setContentPane(mainPanel);
        mainFrame.setVisible(true);

        listenForIncomingMessages();
        keepSendingProfileUpdates();
    }

    private void listenForIncomingMessages() {
        /*
         * This thread keeps polling for any incoming messages and displays them
         * to user
         */

        (new Thread() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(10);
                        Message msg = mp.receive();
                        if (msg != null) {
                            if (msg instanceof MulticastMessage) {
                                switch (((MulticastMessage) msg).getMessageType()) {
                                    case TASK_ADV:
                                        Task receivedTask = (Task) msg.getData();
                                        synchronized (Preferences.nodes) {
                                            taMessages.append("Received task advertisement from: \n" + msg.getId());
                                            Node host_node = Preferences.nodes.get(host);
                                            //now we are simulating load on node through promosedLoad
                                            //which could be different than actual load
                                            float remaining_load = Preferences.TOTAL_LOAD_AT_NODE
                                                    - (receivedTask.taskLoad 
                                                    		/*+ host_node.getProcessorLoad()*/
                                                    		+ host_node.getPromisedLoad()
                                                    		);
                                            int loadCanServe = 0;
                                            if (remaining_load > Preferences.host_reserved_load) {
                                                loadCanServe = receivedTask.taskLoad;
                                            } else {
                                                // Need to change this to avoid fragmentation
                                                loadCanServe = Preferences.TOTAL_LOAD_AT_NODE
                                                        - (Preferences.host_reserved_load 
                                                        		+ host_node.getPromisedLoad());
                                            }

                                            if (loadCanServe > Preferences.MINIMUM_LOAD_REQUEST) {
                                                Integer tempTaskAdvReplyId = ++taskAdvReplyId;
                                                String tempTaskAdvReplyIdStr = host_node.getName() 
                                                	+ tempTaskAdvReplyId;
                                                receivedTask.setPromisedTaskLoad(loadCanServe);
                                                host_node.addToAcceptedTask(tempTaskAdvReplyIdStr, receivedTask);
                                                host_node.incrPromisedLoad(loadCanServe);
                                                TaskAdvReply taskAdvReply = new TaskAdvReply(tempTaskAdvReplyIdStr, 
                                                        		receivedTask.getTaskId(), host_node, loadCanServe);
                                                Message profileMsg = new Message(((MulticastMessage) msg).getSource(),
                                                        "", "", taskAdvReply);
                                                profileMsg.setNormalMsgType(Message.NormalMsgType.PROFILE_XCHG);
                                                try {
                                                    mp.send(profileMsg);
                                                } catch (InvalidMessageException ex) {
                                                    host_node.removeFromAcceptedTask(tempTaskAdvReplyIdStr);
                                                    host_node.decrPromisedLoad(loadCanServe);
                                                    ex.printStackTrace();
                                                }
                                                taMessages.append("Sent message profile for task: "
                                                        + receivedTask.taskId + " " + remaining_load
                                                        + "\n");
                                            } else {
                                                taMessages.append("Node Overloaded: " + receivedTask.taskId + " " + loadCanServe); 
                                            }
                                        }
                                        break;
                                }
                            } else {
                                switch (msg.getNormalMsgType()) {
                                    case NORMAL:
                                        taMessages.append(msg.getData() + "\n");
                                        break;
                                    case PROFILE_XCHG:
                                        TaskAdvReply taskAdvReply = (TaskAdvReply) msg.getData();
//                                        taMessages.append(taskAdvReply + "\n");
                                        TaskLookup taskLookup = taskLookups.get(taskAdvReply.getTaskId());
                                        taskLookup.setRetry(0);
                                        // Mistake here taskLookup maintains Sequence Number
                                        // TaskAdvReplyId is in String
                                        // Instead use getSequenceNumber method. Sequence Number is unique to each task.
//                                    	if(!taskLookup.getTaskGroup().containsKey(taskAdvReply.getTaskAdvReplyId())) {
                                        distributeTask(taskAdvReply);
//                                    	}
                                        break;
                                    case DISTRIBUTED_TASK:
                                        TaskChunk taskChunk = (TaskChunk) msg.getData();
//                                        taMessages.append(taskChunk.toString());
                                        DistributedTask distTask = taskChunk.getDsTask();
                                        TaskResult result;
                                        Map<Integer, TaskResult> tempResults = taskResults.get(distTask.getTaskId());
//                                        if (tempResults != null
//                                                && tempResults.get(distTask.getSeqNumber()) != null) {
//                                            result = taskResults.get(distTask.getTaskId()).get(taskChunk.getSequenceNumber());
//                                        } else {
                                            result = new TaskResult(distTask.getTaskLoad(),
                                                    distTask.taskId, host,
                                                    handleDistributedTask(distTask), taskChunk.getSequenceNumber());

                                            if (tempResults == null) {
                                                tempResults = new HashMap<Integer, TaskResult>();
                                            }

                                            tempResults.put(taskChunk.getSequenceNumber(), result);
                                            taskResults.put(distTask.getTaskId(), tempResults);
//                                        }
                                        Node host_node = Preferences.nodes.get(host);
                                        //decrease promise
                                        if(host_node.getAcceptedTaskByTaskId(taskChunk.getTaskAdvReplyId()) != null) {
                                        	host_node.decrPromisedLoad(
                                        			host_node.getAcceptedTaskByTaskId(
                                        					taskChunk.getTaskAdvReplyId()).getPromisedTaskLoad());
                                        }
                                        host_node.removeFromAcceptedTask(taskChunk.getTaskAdvReplyId());                                        
                                        taMessages.append(result.getTaskResult() + "\n");
                                        Message resultMsg = new Message(distTask.getSource(), "", "", result);
                                        resultMsg.setNormalMsgType(Message.NormalMsgType.TASK_RESULT);
                                        try {
                                            mp.send(resultMsg);
                                        } catch (InvalidMessageException ex) {
                                            Logger.getLogger(MulticastInfrastructure.class.getName())
                                            	.log(Level.SEVERE, null, ex);
                                        }
                                        break;
                                    case TASK_RESULT:
                                        TaskResult taskResult = (TaskResult) msg.getData();
                                        Integer seqNumber = taskResult.getSeqNumber();
                                        taskLookup = taskLookups.get(taskResult.getTaskId());
                                        synchronized (taskLookup) {
                                            taskLookup.getTaskGroup().get(taskLookup.getResultTracker().get(taskResult.getSeqNumber())).setStatus(Preferences.TASK_CHUNK_STATUS.RECEIVED);
                                            taskLookup.removeFromResultTracker(seqNumber);
                                            addAndMergeResults(taskResult);
                                        }
                                        taMessages.append("Sequence Number: " + seqNumber
                                                + " Result tracker: " + taskLookup.printResultTracker()
                                                + " Result from: " + taskResult.getSource()
                                                + " ==> "
                                                + taskResult.getTaskResult() + "\n");

                                        //Merger all results
                                        //if received all result, remove taskLookup
                                        break;
                                }
                            }
                        }
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            private Serializable handleDistributedTask(DistributedTask gotTask) {
                try {
                    Class cl = Class.forName(gotTask.getClassName());
                    HashMap<String, Class[]> mthdDef = new HashMap<String, Class[]>();
                    Method mthds[] = cl.getDeclaredMethods();
                    for (Method m : mthds) {
                        mthdDef.put(m.getName(), m.getParameterTypes());
                    }
                    if (gotTask.getParameters() != null && ((mthdDef.get(gotTask.getMethodName())).length 
                    		!= gotTask.getParameters().length)) {
                        System.out.println("Parameters don\'t match");
                    } else {
                        Class params[] = mthdDef.get(gotTask.getMethodName());
                        Object parameters[] = (Object[]) gotTask.getParameters();
                        try {
                            Method invokedMethod = cl.getMethod(gotTask.getMethodName(), params);
                            return (Serializable) invokedMethod.invoke(
                            		new SampleApplication(host, conf_file, clockType), parameters);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                return null;
            }

            private void distributeTask(TaskAdvReply taskAdvReply) {
                String taskId = taskAdvReply.getTaskId();
                Node node = taskAdvReply.getNode();
                TaskLookup taskLookup = taskLookups.get(taskId);
                Task taskToDistribute = (taskLookups.get(taskId)).getTask();

                // Check this code
                if (taskToDistribute.getTaskLoad() <= 0) {
                    return;
                }
                //------------------------
                if (taskLookup.getStatus() == Preferences.TASK_STATUS.DISTRIBUTED) {
                    return;
                }

                int loadDistributed = (int) Math.ceil((taskToDistribute.getTaskLoad() 
                		> taskAdvReply.getLoadCanServe())
                        ? (taskAdvReply.getLoadCanServe()) : taskToDistribute.getTaskLoad());

                //synchronized (taskLookups) {
                taskToDistribute.setTaskLoad(taskToDistribute.getTaskLoad() - loadDistributed);
                //}
                if (taskToDistribute.getTaskLoad() <= 0) {
                    taskLookup.setStatus(Preferences.TASK_STATUS.DISTRIBUTED);
                }

                Serializable[] parameters = new Serializable[2];
                parameters[0] = 10;
                parameters[1] = 20;
                DistributedTask dsTask = new DistributedTask(loadDistributed, taskId,
                        taskToDistribute.getSource(),
                        "ds.android.tasknet.application.SampleApplication", "method1", parameters);
                TaskChunk taskChunk = new TaskChunk(taskId, node, taskLookup.nextSequenceNumber(),
                        dsTask, taskAdvReply.getTaskAdvReplyId());
                taskLookup.addToTaskGroup(taskChunk.getTaskAdvReplyId(), taskChunk);
                Message distMsg = new Message(node.getName(), "", "", taskChunk);
                distMsg.setNormalMsgType(Message.NormalMsgType.DISTRIBUTED_TASK);

                sendAndRetryTaskChunk(taskChunk, distMsg, dsTask, loadDistributed, taskLookup);

            }

            private void addAndMergeResults(TaskResult taskResult) {
                TaskLookup taskLookup = taskLookups.get(taskResult.getTaskId());
                taskLookup.getTaskResults().put(taskResult.getSeqNumber(), taskResult);
                if(taskLookup.getTask().getTaskLoad() <= 0 && taskLookup.getTaskGroup().size() 
                		== taskLookup.getTaskResults().size())
                    taskLookup.setStatus(Preferences.TASK_STATUS.RECEIVED_RESULTS);
                //Merge results
                if(taskLookup.getStatus() == Preferences.TASK_STATUS.RECEIVED_RESULTS){
                String result = "";
                for(int i=0;i<taskLookup.getTaskResults().size();i++){
                    result += (taskLookup.getTaskResults().get(i)).toString() + " ";
                }
                taMessages.append("Result:\n" + result);
                }
            }
        }).start();
    }

    private void sendAndRetryTaskChunk(final TaskChunk taskChunk, final Message distMsg,
            final DistributedTask taskToDistribute, final int loadDistributed, final TaskLookup taskLookup) {

        (new Thread() {

            public void run() {
                //		synchronized (taskLookup) {
//        			synchronized (taskChunk) {
                //      				synchronized (taskToDistribute) {

                while (taskChunk.getStatus() != Preferences.TASK_CHUNK_STATUS.RECEIVED
                        && taskChunk.getRetry() < Preferences.NUMBER_OF_RETRIES_BEFORE_QUITTING) {

                    try {
                        taskLookups.get(((DistributedTask)taskToDistribute).getTaskId())
                        	.addToResultTracker(taskChunk.getSequenceNumber(), taskChunk.getTaskAdvReplyId());
                        mp.send(distMsg);
                        taskChunk.incrRetry();
                        Thread.sleep(Preferences.WAIT_TIME_BEFORE_RETRYING);
                    } catch (InvalidMessageException ex) {
                        ex.printStackTrace();
                    } catch (InterruptedException e) {
                        // do nothing
                        e.printStackTrace();
                    }
                }
                if (taskChunk.getStatus() != Preferences.TASK_CHUNK_STATUS.RECEIVED) {
                    taskToDistribute.setTaskLoad(taskToDistribute.getTaskLoad() + loadDistributed);
                    if (taskToDistribute.getTaskLoad() > 0) {
                        taskLookup.setStatus(Preferences.TASK_STATUS.ADVERTISED);
                    }
                }
            }
            //    			}
            //    		}
            //}
        }).start();
    }

    private void keepSendingProfileUpdates() {
        (new Thread() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(Preferences.PROFILE_UPDATE_TIME_PERIOD);
                        try {
                            synchronized (Preferences.nodes) {
                                Node updateNode = Preferences.nodes.get(host);
                                updateNode.setBatteryLevel(1);
                                Message profileUpdate = new Message(Preferences.COORDINATOR, "", "", updateNode);
                                profileUpdate.setNormalMsgType(Message.NormalMsgType.PROFILE_UPDATE);
                                mp.send(profileUpdate);
                            }
                        } catch (InvalidMessageException ex) {
                            Logger.getLogger(MulticastInfrastructure.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MulticastInfrastructure.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }).start();
    }

    /**
     * Input:
     * ButtonGroup: Radio button group
     *
     * Output:
     * String: Selected radio button text in the input ButtonGroup
     */
    public String getSelection(ButtonGroup group) {
        for (Enumeration e = group.getElements(); e.hasMoreElements();) {
            JRadioButton b = (JRadioButton) e.nextElement();
            if (b.getModel() == group.getSelection()) {
                return b.getText().toLowerCase();
            }
        }
        return null;
    }

    /**
     * This actionPerformed method of "Send Message" button creates a new message
     * and sends for further processing by MessagePasser
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == btnAdvertiseTask) {

            String node = "";
            String kind = "";
            String msgid = "";
            if (tfSend.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Enter task load", "Task load", JOptionPane.WARNING_MESSAGE);
            } else {

                taskNum++;
                String taskId = Preferences.node_names.get(host_index) + taskNum;
                Task newTask = new Task(new Integer(tfSend.getText()), taskId, host);
                taMessages.append("Task advertised\n");
                TaskLookup taskLookup = new TaskLookup(newTask);
                synchronized (taskLookups) {
                    taskLookups.put(taskId, taskLookup);
                }
                MulticastMessage mMsg = new MulticastMessage(node, kind, msgid,
                        newTask, mp.getClock(), true, MulticastMessage.MessageType.TASK_ADV, host);

                sendAndRetryTaskAdv(taskLookup, mMsg);
                Preferences.crashNode = "";
            }
        }
    }

    private void sendAndRetryTaskAdv(final TaskLookup taskLookup, final Message mMsg) {

        (new Thread() {

            public void run() {
                //synchronized (taskLookup) {
                //if it still hasn't exhausted its retry and still hasn't got enough reply
                Message advMsg = mMsg;
                while (taskLookup.getStatus() == Preferences.TASK_STATUS.ADVERTISED
                        && taskLookup.getRetry() < Preferences.NUMBER_OF_RETRIES_BEFORE_QUITTING) {

                    System.out.println("tasklookup status : " + taskLookup.getStatus());
                    //send task request advertisement, ask for bid
                    try {
                        mp.send(advMsg);
                        advMsg = new MulticastMessage(mMsg.getDest(), mMsg.getKind(), mMsg.getId(),
                        mMsg.getData(), mp.getClock(), true, MulticastMessage.MessageType.TASK_ADV, host);
                    } catch (InvalidMessageException e) {
                        e.printStackTrace();
                    }
                    taskLookup.incrRetry();
                    try {
                        Thread.sleep(Preferences.WAIT_TIME_BEFORE_RETRYING);
                    } catch (InterruptedException e) {
                    }
                }
             //}
            }
        }).start();
    }

    /**
     * Method to start listening for incoming connections
     */
    void startListening() {
        mp.start();
    }

    public void itemStateChanged(ItemEvent e) {
//        if (e.getSource() == cbDrop) {
//            Preferences.logDrop = cbDrop.isSelected();
//        } else if (e.getSource() == cbDelay) {
//            Preferences.logDelay = cbDelay.isSelected();
//        } else if (e.getSource() == cbDuplicate) {
//            Preferences.logDuplicate = cbDuplicate.isSelected();
//        } else if (e.getSource() == cbLog) {
//            Preferences.logEvent = cbLog.isSelected();
//        }
    }
}
