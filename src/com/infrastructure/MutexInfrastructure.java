package com.infrastructure;

import com.clock.ClockFactory;
import com.conf.Preferences;
import com.exceptions.InvalidMessageException;
import com.msgpasser.Message;
import com.msgpasser.MessagePasser;
import com.msgpasser.MulticastMessage;
import com.msgpasser.TimeStampedMessage;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MulticastSocket;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
public class MutexInfrastructure implements ActionListener, ItemListener {

    int numOfNodes = 0, host_index = 0;
    public static boolean flag = false;
    JFileChooser fileChooser;
    JScrollPane scrollPane;
    JPanel mainPanel, scrollPanel, radioNodesPanel, kindIdPanel, cbPanel, btnPanel;
    JLabel host_label, kind_label, id_label;
    JTextArea taMessages;
    JTextField tfSend, tfKind, tfId;
    JFrame mainFrame;
    JButton btnSend, btnSimulateEvent, btnMulticastSend, btnMulticastSimulateCrash, btnMutex;
    ButtonGroup bgNodes;
    JRadioButton rbNodes[];
    JCheckBox cbLog, cbDrop, cbDelay, cbDuplicate;
    MessagePasser mp;
    String host;
    Properties prop;
    ClockFactory.ClockType clock;

    public MutexInfrastructure(String host_name, String conf_file, String clockType) {
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

        tfSend = new JTextField(50);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
//        mainPanel.add(tfSend, gbc);

        bgNodes = new ButtonGroup();
        StringTokenizer nodes = new StringTokenizer(prop.getProperty("NAMES"), ",");
        rbNodes = new JRadioButton[nodes.countTokens() - 1];
        radioNodesPanel = new JPanel(new FlowLayout());
        numOfNodes = rbNodes.length + 1;
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
            rbNodes[i] = new JRadioButton(nodeStr.toUpperCase());
            bgNodes.add(rbNodes[i]);
            radioNodesPanel.add(rbNodes[i]);
        }
        rbNodes[0].setSelected(true);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
//        mainPanel.add(radioNodesPanel, gbc);

        kind_label = new JLabel("Enter Message Kind");
        tfKind = new JTextField(10);
        id_label = new JLabel("Enter Message ID");
        tfId = new JTextField(10);
        kindIdPanel = new JPanel();
        kindIdPanel.add(kind_label);
        kindIdPanel.add(tfKind);
        kindIdPanel.add(id_label);
        kindIdPanel.add(tfId);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(kindIdPanel, gbc);


        cbDrop = new JCheckBox("Drop Messages");
        cbDrop.addItemListener(this);
        cbDelay = new JCheckBox("Delay Messages");
        cbDelay.addItemListener(this);
        cbDuplicate = new JCheckBox("Duplicate Messages");
        cbDuplicate.addItemListener(this);
        cbLog = new JCheckBox("Log Event");
        cbLog.addItemListener(this);
        cbPanel = new JPanel();
        cbPanel.add(new JLabel("Log Events:"));
        cbPanel.add(cbDrop);
        cbPanel.add(cbDelay);
        cbPanel.add(cbDuplicate);
        cbPanel.add(cbLog);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
//        mainPanel.add(cbPanel, gbc);

        btnSimulateEvent = new JButton("Simulate Event");
        btnSimulateEvent.addActionListener(this);
        btnSend = new JButton("Send Message");
        btnSend.addActionListener(this);
        btnMulticastSend = new JButton("Multicast Message");
        btnMulticastSend.addActionListener(this);
        btnMutex = new JButton("Enter critical section");
        btnMutex.addActionListener(this);
        btnMulticastSimulateCrash = new JButton("Multicast Message by Simulatiing crash");
        btnMulticastSimulateCrash.addActionListener(this);
        btnPanel = new JPanel(new FlowLayout());
//        btnPanel.add(btnSimulateEvent);
//        btnPanel.add(btnSend);
//        btnPanel.add(btnMulticastSend);
//        btnPanel.add(btnMulticastSimulateCrash);
        btnPanel.add(btnMutex);
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

        mainFrame = new JFrame("Mutual Exclusion Lab");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setResizable(false);
        mainFrame.setSize(600, 300);
        mainFrame.add(mainPanel);
        mainFrame.setContentPane(mainPanel);
        mainFrame.setVisible(true);

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
                            if (msg instanceof MulticastMessage
                                    && ((MulticastMessage) msg).getMessageType() == MulticastMessage.MessageType.MUTEX_ACK) {
                                taMessages.append("Holding mutex....\n");
                            } else
                                taMessages.append(msg.getData().toString() + "\n");
                        }
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
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
        if (ae.getSource() == btnSend) {
            try {
                String node = getSelection(bgNodes);
                String kind = tfKind.getText();
                String msgid = tfId.getText();
                taMessages.append(" Message sent to " + node + ": " + tfSend.getText() + "\n");
                TimeStampedMessage tm = new TimeStampedMessage(node, kind, msgid, tfSend.getText(), mp.getClock(), true);
                mp.send(tm);
            } catch (InvalidMessageException ex) {
                System.out.println("Sender: " + ex.getError());
            }
        } else if (ae.getSource() == btnSimulateEvent) {
            if (cbLog.isSelected()) {
                try {
                    TimeStampedMessage tm = new TimeStampedMessage(Preferences.logger_name, "log", "log", "", mp.getClock(), true);
                    tm.setLogMessage("Random event in " + host);
                    mp.getClock().print();
                    mp.send(new Message(Preferences.logger_name, "log", "log", tm));
                } catch (InvalidMessageException ex) {
                    Logger.getLogger(MutexInfrastructure.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                mp.getClock().incrementTime(Preferences.host_index);
            }
        } else if (ae.getSource() == btnMulticastSend || ae.getSource() == btnMulticastSimulateCrash) {
            try {
                String node = getSelection(bgNodes);
                String kind = tfKind.getText();
                String msgid = tfId.getText();
                taMessages.append(" Multicasted Message: " + tfSend.getText() + "\n");
                MulticastMessage mMsg = new MulticastMessage(node, kind, msgid,
                        tfSend.getText(), mp.getClock(), true, MulticastMessage.MessageType.NORMAL, host);
                mp.send(mMsg);
                if (ae.getSource() == btnMulticastSimulateCrash) {
                    Preferences.crashNode = getSelection(bgNodes);
                } else {
                    Preferences.crashNode = "";
                }
            } catch (InvalidMessageException ex) {
                System.out.println("Sender: " + ex.getError());
            }
        } else if (ae.getSource() == btnMutex) {
            try {
                String node = Preferences.MULTICAST_MESSAGE;
                String kind = tfKind.getText();
                String msgid = tfId.getText();
                MulticastMessage mutexMsg = null;
                if (ae.getActionCommand().equalsIgnoreCase(Preferences.ENTER_CRITICAL_SECTION)) {
                    ((JButton) ae.getSource()).setText(Preferences.LEAVE_CRITICAL_SECTION);
                    taMessages.append("Mutex requested\n");
                    mp.setProcessState("Wanted");
                    mutexMsg = new MulticastMessage(node, kind, msgid, tfSend.getText(), mp.getClock(), true, MulticastMessage.MessageType.GET_MUTEX, host);
                } else {
                    ((JButton) ae.getSource()).setText(Preferences.ENTER_CRITICAL_SECTION);
                    taMessages.append("Mutex released\n");
                    mp.setProcessState("Released");
                    mutexMsg = new MulticastMessage(node, kind, msgid, tfSend.getText(), mp.getClock(), true, MulticastMessage.MessageType.RELEASE_MUTEX, host);
                }
                mp.send(mutexMsg);
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
        if (e.getSource() == cbDrop) {
            Preferences.logDrop = cbDrop.isSelected();
        } else if (e.getSource() == cbDelay) {
            Preferences.logDelay = cbDelay.isSelected();
        } else if (e.getSource() == cbDuplicate) {
            Preferences.logDuplicate = cbDuplicate.isSelected();
        } else if (e.getSource() == cbLog) {
            Preferences.logEvent = cbLog.isSelected();
        }
    }
}
