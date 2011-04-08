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
    Properties prop;
    ClockFactory.ClockType clock;
    public static int taskNum = 0;
    HashMap<String, ArrayList<Node>> taskGroup;
    HashMap<String, Task> taskDetails;

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
        host = host_name;
        taskGroup = new HashMap<String, ArrayList<Node>>();
        taskDetails = new HashMap<String, Task>();
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
            if (nodeStr.equalsIgnoreCase(host) || nodeStr.equalsIgnoreCase(Preferences.logger_name)) {
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
                                        taMessages.append("Received task advertisement\n");
                                        Task receivedTask = (Task) msg.getData();
                                        synchronized (Preferences.nodes) {
                                            Node host_node = Preferences.nodes.get(host);
                                            host_node.setTaskid(receivedTask.getTaskId());
                                            float remaining_load = Preferences.TOTAL_LOAD_AT_NODE
                                                    - (receivedTask.taskLoad + host_node.getProcessorLoad());
                                            if (remaining_load > Preferences.host_reserved_load) {
                                                Message profileMsg = new Message(((MulticastMessage) msg).getSource(),
                                                        "", "", host_node);
                                                profileMsg.setNormalMsgType(Message.NormalMsgType.PROFILE_XCHG);
                                                try {
                                                    mp.send(profileMsg);
                                                } catch (InvalidMessageException ex) {
                                                    ex.printStackTrace();
                                                }
                                                taMessages.append("Sent message profile for task: "
                                                        + receivedTask.taskId + " " + remaining_load
                                                        + "\n");
                                            } else {
                                                taMessages.append("Node Overloaded: " + receivedTask.taskId + " " + remaining_load + "\n");
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
                                        Node profileOfNode = (Node) msg.getData();
                                        taMessages.append(profileOfNode + "\n");
                                        // taMessages.append(profileOfNode + "\n");
                                        String taskId = profileOfNode.getTaskid();
                                        synchronized (taskGroup) {
                                            ArrayList<Node> taskNodes = taskGroup.get(taskId);
                                            if (taskNodes == null) {
                                                taskNodes = new ArrayList<Node>();
                                            }
                                            taskNodes.add(profileOfNode);
                                            taskGroup.put(taskId, taskNodes);
                                        }
                                        distributeTask(profileOfNode);
                                        break;
                                    case DISTRIBUTED_TASK:
                                        DistributedTask distTask = (DistributedTask) msg.getData();
                                        TaskResult result = new TaskResult(distTask.getTaskLoad(),
                                                distTask.taskId, host, handleDistributedTask(distTask));
                                        taMessages.append(result.getTaskResult() + "\n");
                                        Message resultMsg = new Message(distTask.getSource(), "", "", result);
                                        resultMsg.setNormalMsgType(Message.NormalMsgType.TASK_RESULT);
                                        try {
                                            mp.send(resultMsg);
                                        } catch (InvalidMessageException ex) {
                                            Logger.getLogger(MulticastInfrastructure.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                        break;
                                    case TASK_RESULT:
                                        TaskResult taskResult = (TaskResult)msg.getData();
                                        taMessages.append("Result from: " + taskResult.getSource() + " ==> "
                                                + taskResult.getTaskResult());
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
                    if (gotTask.getParameters() != null && ((mthdDef.get(gotTask.getMethodName())).length != gotTask.getParameters().length)) {
                        System.out.println("Parameters don\'t match");
                    } else {
                        Class params[] = mthdDef.get(gotTask.getMethodName());
                        Object parameters[] = (Object[]) gotTask.getParameters();
                        try {
                            Method invokedMethod = cl.getMethod(gotTask.getMethodName(), params);
                            return (Serializable) invokedMethod.invoke(new SampleApplication(), parameters);
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

            private void distributeTask(Node profileOfNode) {
                String taskId = profileOfNode.getTaskid();
                Task taskToDistribute = taskDetails.get(taskId);
                Serializable[] parameters = new Serializable[2];
                parameters[0] = 10;
                parameters[1] = 20;
                DistributedTask dsTask = new DistributedTask(taskToDistribute.getTaskLoad(), taskId,
                        taskToDistribute.getSource(),
                        "ds.android.tasknet.application.SampleApplication", "method1", parameters);
                Message distMsg = new Message(profileOfNode.getName(), "", "", dsTask);
                distMsg.setNormalMsgType(Message.NormalMsgType.DISTRIBUTED_TASK);
                try {
                    mp.send(distMsg);
                } catch (InvalidMessageException ex) {
                    ex.printStackTrace();
                }
            }
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
            try {
                String node = "";
                String kind = "";
                String msgid = "";
                if (tfSend.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Enter task load", "Task load", JOptionPane.WARNING_MESSAGE);
                } else {
                    try {
                        taskNum++;
                        String taskId = Preferences.node_names.get(host_index) + taskNum;
                        Task newTask = new Task(new Integer(tfSend.getText()), taskId, host);
                        taMessages.append("Task advertised\n");
                        synchronized (taskGroup) {
                            synchronized (taskDetails) {
                                taskGroup.put(taskId, new ArrayList<Node>());
                                taskDetails.put(taskId, newTask);
                            }
                        }
                        MulticastMessage mMsg = new MulticastMessage(node, kind, msgid,
                                newTask, mp.getClock(), true, MulticastMessage.MessageType.TASK_ADV, host);
                        mp.send(mMsg);
                        Preferences.crashNode = "";
                    } catch (NumberFormatException nex) {
                        JOptionPane.showMessageDialog(null, "Enter valid task load", "Enter valid Task load", JOptionPane.WARNING_MESSAGE);
                    }
                }
            } catch (InvalidMessageException ex) {
                System.out.println("Sender: " + ex.getError());
            }
        }
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
