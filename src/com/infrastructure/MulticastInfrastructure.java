package com.infrastructure;

import com.clock.ClockFactory;
import com.conf.Node;
import com.conf.Preferences;
import com.exceptions.InvalidMessageException;
import com.msgpasser.Message;
import com.msgpasser.MessagePasser;
import com.msgpasser.MulticastMessage;
import com.task.Task;
import com.thoughtworks.xstream.XStream;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
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
         * This thread keeps polling for any incoming messages and
         * displays them to user
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
                                        XStream nodeXStream = new XStream();
                                        nodeXStream.alias("node", Node.class);
                                        String nodeProfile = nodeXStream.toXML(Preferences.nodes.get(host));
                                        Message profileMsg = new Message(((MulticastMessage) msg).getSource(), "", "", nodeProfile);
                                        profileMsg.setNormalMsgType(Message.NormalMsgType.PROFILE_XCHG);
                                        try {
                                            mp.send(profileMsg);
                                        } catch (InvalidMessageException ex) {
                                            Logger.getLogger(MulticastInfrastructure.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                        break;
                                }
                            } else {
                                switch (msg.getNormalMsgType()) {
                                    case NORMAL:
                                        taMessages.append(msg.getData() + "\n");
                                        break;
                                    case PROFILE_XCHG:
                                        XStream readProfile = new XStream();
                                        readProfile.alias("node", Node.class);
                                        Node profileOfNode = (Node) readProfile.fromXML(msg.getData().toString());
                                        taMessages.append(profileOfNode + "\n");
                                        break;
                                }
                            }
                        }
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
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
                        Thread.sleep(1000);
                        try {
                            synchronized (Preferences.nodes) {
                                Node updateNode = Preferences.nodes.get(host);
                                updateNode.setBatteryLevel(5);
                                XStream profileDetails = new XStream();
                                profileDetails.alias("node", Node.class);
                                Message profileUpdate = new Message(Preferences.COORDINATOR, "", "", profileDetails.toXML(updateNode));
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
                        Task newTask = new Task(new Integer(tfSend.getText()));
                        XStream taskDetails = new XStream();
                        taskDetails.alias("task", Task.class);
                        taMessages.append("Task advertised\n");
                        MulticastMessage mMsg = new MulticastMessage(node, kind, msgid,
                                taskDetails.toXML(newTask), mp.getClock(), true, MulticastMessage.MessageType.TASK_ADV, host);
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
