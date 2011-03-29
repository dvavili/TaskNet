/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.android.tasknet.logger;

import ds.android.tasknet.config.Node;
import ds.android.tasknet.config.Preferences;
import ds.android.tasknet.exceptions.InvalidMessageException;
import ds.android.tasknet.msgpasser.Message;
import ds.android.tasknet.msgpasser.MessagePasser;
import ds.android.tasknet.msgpasser.MulticastMessage;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

/**
 *
 * @author Divya
 */
public class Coordinator {

    JFrame frame;
    JScrollPane scrollPane;
    JPanel mainPanel, panel, scrollPanel;
    static Vector<String> vClients;
    JProgressBar clientProgressBars[];
    int numberOfClients = 0;
    JTextArea taLogArea;
    TitledBorder batteryTitle;
    MessagePasser mp;

    public Coordinator(String host_name, String conf_file) {
        Preferences.setHostDetails(conf_file, host_name);
        mp = new MessagePasser(conf_file, host_name);
        numberOfClients = Preferences.nodes.size();

        frame = new JFrame("TaskNet - Logger");
        vClients = new Vector<String>();
        mainPanel = new JPanel();
        mainPanel.add(new JLabel("Communication Logs:"));

        taLogArea = new JTextArea(10, 50);
        taLogArea.setEditable(false);
        scrollPane = new JScrollPane(taLogArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(scrollPane);

        batteryTitle = BorderFactory.createTitledBorder("Battery Life of Nodes");
        panel = new JPanel(new GridBagLayout());
        panel.setBorder(batteryTitle);

        initComponents();

        mainPanel.add(panel);
        frame.setContentPane(mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setSize(600, 600);

        listenForIncomingMessages();
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
                                        Node nodeProfile = Preferences.nodes.get(Preferences.logger_name);
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
                                        Node profileOfNode = (Node)msg.getData();
                                        taLogArea.append(profileOfNode + "\n");
                                        break;
                                    case PROFILE_UPDATE:
                                        System.out.println("Receiving update info");
                                        Node nodeToBeUpdated = (Node)msg.getData();
                                        synchronized (Preferences.nodes) {
                                            Preferences.nodes.get(nodeToBeUpdated.getName()).update(nodeToBeUpdated);
                                        }
                                        repaintPanel();
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

    /**
     * Method to start listening for incoming connections
     */
    public void startListening() {
        mp.start();
    }

    public static void addClient(String clientName) {
        synchronized (vClients) {
            vClients.add(clientName);
            vClients.notifyAll();
        }
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

//    public static void main(String[] args) {
//        new TaskNetLogger();
////        (new InitialConfiguration()).addClients();
//    }
    private void initComponents() {
//        numberOfClients = vClients.size();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        clientProgressBars = new JProgressBar[numberOfClients];
        for (int i = 0; i < numberOfClients; i++) {
            clientProgressBars[i] = new JProgressBar(JProgressBar.VERTICAL, 0, 100);
            String nodeName = Preferences.node_names.get(i);
            clientProgressBars[i].setValue(Preferences.nodes.get(nodeName).getBatteryLevel());
        }

        int numberOfRows = 10;
        for (int i = 0; i < numberOfClients; i++) {
            panel.add(clientProgressBars[i], gbc);
            gbc.gridy++;
            panel.add(new JLabel(Preferences.node_names.get(i)), gbc);
            gbc.gridy--;
            gbc.gridx++;
            if ((i + 1) % numberOfRows == 0) {
                gbc.gridy += 2;
                gbc.gridx = 0;
            }
        }

//        taLogArea = new JTextArea(10,50);
//        gbc.gridx++;
//        gbc.gridy++;
//        panel.add(taLogArea,gbc);

//        listenForClients();
    }

    private void listenForClients() {
        (new Thread() {

            @Override
            public void run() {
                while (true) {
                    synchronized (vClients) {
                        if (numberOfClients == vClients.size()) {
                            try {
                                vClients.wait();
                            } catch (InterruptedException ex) {
                                Logger.getLogger(Coordinator.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        } else {
                            System.out.println("In repaint");
                            repaintPanel();
                        }
                    }
                }
            }
        }).start();
    }

    private void repaintPanel() {
        panel.removeAll();
        initComponents();
        panel.revalidate();
    }
//    private void addClients() {
//        (new Thread() {
//
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        Thread.sleep(1000);
//                        synchronized (vClients) {
//                            vClients.add("alice");
//                            System.out.println("Adding alice");
//                            vClients.notifyAll();
//                        }
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(InitialConfiguration.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//            }
//        }).start();
//    }
}
