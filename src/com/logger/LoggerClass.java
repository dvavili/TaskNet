/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.logger;

import com.clock.ClockFactory;
import com.clock.LogicalClock;
import com.clock.VectorClock;
import com.conf.Node;
import com.conf.Preferences;
import com.exceptions.InvalidMessageException;
import com.msgpasser.Message;
import com.msgpasser.MessagePasser;
import com.msgpasser.MulticastMessage;
import com.msgpasser.TimeStampedMessage;
import com.thoughtworks.xstream.XStream;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 *
 * @author Divya
 */
public class LoggerClass implements ActionListener {

    JFrame mainFrame;
    JPanel mainPanel, comparePanel;
    JButton logButton, btnCompare;
    JTextField tfFirstEvent, tfSecondEvent;
    JTextArea taLogArea;
    JScrollPane scrollPane;
    MessagePasser mp;
    Logs logs;

    public LoggerClass(String host_name, String conf_file) {
        Preferences.setHostDetails(conf_file, host_name);
        mp = new MessagePasser(conf_file, host_name);
        logs = new Logs();
        buildUI();
        listenForIncomingMessages();
    }

    public void buildUI() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new FlowLayout());

        comparePanel = new JPanel();
        comparePanel.setLayout(new FlowLayout());
        tfFirstEvent = new JTextField(10);
        tfSecondEvent = new JTextField(10);
        btnCompare = new JButton("Compare Events");
        btnCompare.addActionListener(this);
        comparePanel.add(new JLabel("Event1 ID:"));
        comparePanel.add(tfFirstEvent);
        comparePanel.add(new JLabel("Event2 ID:"));
        comparePanel.add(tfSecondEvent);
        comparePanel.add(btnCompare);
        mainPanel.add(comparePanel);

        logButton = new JButton("Show Logs");
        logButton.addActionListener(this);
        mainPanel.add(logButton);

        taLogArea = new JTextArea(17, 50);
        taLogArea.setEditable(false);
        scrollPane = new JScrollPane(taLogArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(scrollPane);

        mainFrame = new JFrame("Logger");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setResizable(false);
        mainFrame.setSize(600, 400);
//        mainFrame.add(comparePanel);
        mainFrame.add(mainPanel);
        mainFrame.setContentPane(mainPanel);
        mainFrame.setVisible(true);

        mp.start();
        listenForIncomingMessages();
//        (new Thread() {
//
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        Thread.sleep(10);
//                        Message msg = mp.receive();
//                        if (msg != null) {
//                            logs.add((Message) msg.getData());
//                        }
//                    } catch (InterruptedException ex) {
//                        ex.printStackTrace();
//                    }
//                }
//            }
//        }).start();
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
                                        taLogArea.append("Received task advertisement\n");
                                        XStream nodeXStream = new XStream();
                                        nodeXStream.alias("node", Node.class);
                                        String nodeProfile = nodeXStream.toXML(Preferences.nodes.get(Preferences.logger_name));
                                        Message profileMsg = new Message(((MulticastMessage) msg).getSource(), "", "", nodeProfile);
                                        profileMsg.setNormalMsgType(Message.NormalMsgType.PROFILE_XCHG);
                                        try {
                                            mp.send(profileMsg);
                                        } catch (InvalidMessageException ex) {
                                            Logger.getLogger(LoggerClass.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                        break;
                                }
                            } else {
                                switch (msg.getNormalMsgType()) {
                                    case NORMAL:
                                        taLogArea.append(msg.getData() + "\n");
                                        break;
                                    case PROFILE_XCHG:
                                        XStream readProfile = new XStream();
                                        readProfile.alias("node", Node.class);
                                        Node profileOfNode = (Node) readProfile.fromXML(msg.getData().toString());
                                        taLogArea.append(profileOfNode + "\n");
                                        break;
                                    case PROFILE_UPDATE:
                                        XStream profile = new XStream();
                                        profile.alias("node", Node.class);
                                        Node nodeToBeUpdated = (Node)profile.fromXML(msg.getData().toString());
                                        synchronized(Preferences.nodes){
                                            Preferences.nodes.get(nodeToBeUpdated.getName()).update(nodeToBeUpdated);
                                        }
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

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == logButton) {
            receiveMessages();
        } else if (e.getSource() == btnCompare) {
            if (tfFirstEvent.getText().isEmpty() || tfSecondEvent.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Enter Event ID", "Enter Event ID", JOptionPane.WARNING_MESSAGE);
            } else {
                LogMessage firstEvent = null, secondEvent = null;
                for (LogMessage lm : logs.getLogs()) {
                    if (tfFirstEvent.getText().equalsIgnoreCase(((TimeStampedMessage) lm.getMessage()).getEventID())) {
                        firstEvent = lm;
                    }
                    if (tfSecondEvent.getText().equalsIgnoreCase(((TimeStampedMessage) lm.getMessage()).getEventID())) {
                        secondEvent = lm;
                    }
                }
                if (firstEvent == null) {
                    JOptionPane.showMessageDialog(null, tfFirstEvent.getText() + " does not exist",
                            "Invalid Event ID", JOptionPane.WARNING_MESSAGE);
                } else if (secondEvent == null) {
                    JOptionPane.showMessageDialog(null, tfSecondEvent.getText() + " does not exist",
                            "Invalid Event ID", JOptionPane.WARNING_MESSAGE);
                } else {
                    int compareResult = 0;
                    if (((TimeStampedMessage) firstEvent.getMessage()).getClockService() instanceof VectorClock) {
                        compareResult = logs.compare(firstEvent, secondEvent, ClockFactory.ClockType.VECTOR);
                    } else if (((TimeStampedMessage) firstEvent.getMessage()).getClockService() instanceof LogicalClock) {
                        compareResult = logs.compare(firstEvent, secondEvent, ClockFactory.ClockType.LOGICAL);
                    }
                    String result = null;
                    if (compareResult < 0) {
                        result = tfFirstEvent.getText() + " "
                                + ((TimeStampedMessage) firstEvent.getMessage()).getClockService().getTime()
                                + " happened before " + tfSecondEvent.getText() + " "
                                + ((TimeStampedMessage) secondEvent.getMessage()).getClockService().getTime();
                    } else if (compareResult > 0) {
                        result = tfSecondEvent.getText() + " "
                                + ((TimeStampedMessage) secondEvent.getMessage()).getClockService().getTime()
                                + " happened before " + tfFirstEvent.getText() + " "
                                + ((TimeStampedMessage) firstEvent.getMessage()).getClockService().getTime();
                    } else {
                        result = tfFirstEvent.getText() + " "
                                + ((TimeStampedMessage) firstEvent.getMessage()).getClockService().getTime()
                                + " is concurrent with " + tfSecondEvent.getText() + " "
                                + ((TimeStampedMessage) secondEvent.getMessage()).getClockService().getTime();
                    }
                    JOptionPane.showMessageDialog(null, result,
                            "Compare Result", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    public void receiveMessages() {
        taLogArea.append("In receive logs\n");
        logs.orderLogs();
        logs.orderLogs();
        ArrayList<LogMessage> orderedLogs = logs.getLogs();
        for (int i = 0; i < orderedLogs.size(); i++) {
            LogMessage currentLog = (LogMessage) orderedLogs.get(i);
            taLogArea.append(((TimeStampedMessage) currentLog.getMessage()).getEventID() + "\t"
                    + orderedLogs.get(i).getRelation() + "\t"
                    + ((TimeStampedMessage) currentLog.getMessage()).getClockService().getTime() + "\t"
                    + ((TimeStampedMessage) currentLog.getMessage()).getLogMessage() + "\n");
        }
    }
}
