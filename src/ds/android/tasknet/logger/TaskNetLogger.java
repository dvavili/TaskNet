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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

/**
 *
 * @author Divya
 */
public class TaskNetLogger implements ActionListener {

    JButton btnShowLog;
    JFrame frame, nodeFrames[];
    JScrollPane scrollPane, nodeScrollPane[];
    JPanel mainPanel, batteryPanel, clientPanel, memoryPanel, cpuLoadPanel;
    JPanel scrollPanel, nodeNamePanel, nodePanel[], panel;
    JProgressBar batteryProgressBars[], memoryProgressBars[], cpuLoadProgressBars[];
    int numberOfClients = 0;
    JTextField tfNodeName;
    JTextArea taLogArea, taNodeLogArea[];
    TitledBorder batteryTitle, memoryTitle, cpuLoadTitle;
    Map<String, JTextArea> nodeLogTextAreaMap;
    Map<String, JFrame> nodeFrameMap;
    Map<String, ArrayList<String>> nodeLogs;
    MessagePasser mp;
    String host_name;

    public TaskNetLogger(String host_name, String conf_file) {

    	this.host_name = host_name;
        Preferences.setHostDetails(conf_file, host_name);
        mp = new MessagePasser(conf_file, host_name);
        numberOfClients = Preferences.nodes.size();

        frame = new JFrame("TaskNet - Logger");

        nodeNamePanel = new JPanel();
        nodeNamePanel.add(new JLabel("Node Name: "));
        tfNodeName = new JTextField(20);
        btnShowLog = new JButton("Show Logs");
        btnShowLog.addActionListener(this);
        nodeNamePanel.add(tfNodeName);
        nodeNamePanel.add(btnShowLog);

        mainPanel = new JPanel();
        panel = new JPanel(new FlowLayout());

        taLogArea = new JTextArea(10, 50);
        taLogArea.setEditable(false);
        scrollPane = new JScrollPane(taLogArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(nodeNamePanel);
        mainPanel.add(scrollPane);

        batteryTitle = BorderFactory.createTitledBorder("Battery Life of Nodes");
        batteryPanel = new JPanel(new GridBagLayout());
        batteryPanel.setBorder(batteryTitle);

        memoryTitle = BorderFactory.createTitledBorder("Memory capacity of Nodes");
        memoryPanel = new JPanel(new GridBagLayout());
        memoryPanel.setBorder(memoryTitle);

        cpuLoadTitle = BorderFactory.createTitledBorder("CPU Load of Nodes");
        cpuLoadPanel = new JPanel(new GridBagLayout());
        cpuLoadPanel.setBorder(cpuLoadTitle);

        initComponents();

        mainPanel.add(panel);
        frame.setContentPane(mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setSize(800, 500);

        createNodeLogFrames();

        listenForIncomingMessages();
    }

    public void actionPerformed(ActionEvent e) {
        String nodeName = tfNodeName.getText();
        if (nodeName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter Node name", "Node name", JOptionPane.WARNING_MESSAGE);
        } else if (!Preferences.node_names.containsValue(nodeName)) {
            JOptionPane.showMessageDialog(null, "Node does not exist in the system", "Node name", JOptionPane.WARNING_MESSAGE);
        } else {
            nodeFrameMap.get(nodeName).setVisible(true);
        }
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
                                        taLogArea.append("\nReceived task advertisement");
                                        Node nodeProfile = Preferences.nodes.get(Preferences.LOGGER_NAME);
                                        Message profileMsg = new Message(((MulticastMessage) msg).getSource(), 
                                        		"", "", nodeProfile, host_name);
                                        profileMsg.setNormalMsgType(Message.NormalMsgType.PROFILE_XCHG);
                                        try {
                                            mp.send(profileMsg);
                                        } catch (InvalidMessageException ex) {
                                            ex.printStackTrace();
                                        }
                                        break;
                                }
                            } else {
                                switch (msg.getNormalMsgType()) {
                                    case LOG_MESSAGE:
                                        nodeLogs.get(msg.getLogSource()).add(msg.getData().toString());
                                        taLogArea.append(msg.getLogSource() + ": " + msg.getData());
                                        nodeLogTextAreaMap.get(msg.getLogSource()).append(msg.getData().toString());
                                        break;
                                    case PROFILE_UPDATE:
                                        System.out.println("Receiving update info");
                                        Node nodeToBeUpdated = (Node) msg.getData();
                                        synchronized (Preferences.nodes) {
                                            int mem = (int) nodeToBeUpdated.getMemoryCapacity() - 1;
                                            int procload = (int) nodeToBeUpdated.getProcessorLoad() - 1;
                                            int batterylevel = nodeToBeUpdated.getBatteryLevel() - 1;
                                            (Preferences.nodes.get(nodeToBeUpdated.getName())).update(mem, procload, batterylevel);
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

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        batteryProgressBars = new JProgressBar[numberOfClients];
        for (int i = 0; i < numberOfClients; i++) {
            batteryProgressBars[i] = new JProgressBar(JProgressBar.VERTICAL, 0, 100);
            String nodeName = Preferences.node_names.get(i);
            batteryProgressBars[i].setValue(Preferences.nodes.get(nodeName).getBatteryLevel());
        }

        int numberOfRows = 10;
        for (int i = 0; i < numberOfClients; i++) {
            batteryPanel.add(batteryProgressBars[i], gbc);
            gbc.gridy++;
            batteryPanel.add(new JLabel(Preferences.node_names.get(i)), gbc);
            gbc.gridy--;
            gbc.gridx++;
            if ((i + 1) % numberOfRows == 0) {
                gbc.gridy += 2;
                gbc.gridx = 0;
            }
        }

        /*-----------------------------------------------------------------------*/
        /*                Memory load of Nodes                                   */
        /*-----------------------------------------------------------------------*/
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        memoryProgressBars = new JProgressBar[numberOfClients];
        for (int i = 0; i < numberOfClients; i++) {
            memoryProgressBars[i] = new JProgressBar(JProgressBar.VERTICAL, 0, 100);
            String nodeName = Preferences.node_names.get(i);
            memoryProgressBars[i].setValue((int) Preferences.nodes.get(nodeName).getMemoryCapacity());
        }

        for (int i = 0; i < numberOfClients; i++) {
            memoryPanel.add(memoryProgressBars[i], gbc);
            gbc.gridy++;
            memoryPanel.add(new JLabel(Preferences.node_names.get(i)), gbc);
            gbc.gridy--;
            gbc.gridx++;
            if ((i + 1) % numberOfRows == 0) {
                gbc.gridy += 2;
                gbc.gridx = 0;
            }
        }
        /*-----------------------------------------------------------------------*/
        /*                Processor load of Nodes                                   */
        /*-----------------------------------------------------------------------*/
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cpuLoadProgressBars = new JProgressBar[numberOfClients];
        for (int i = 0; i < numberOfClients; i++) {
            cpuLoadProgressBars[i] = new JProgressBar(JProgressBar.VERTICAL, 0, 100);
            String nodeName = Preferences.node_names.get(i);
            cpuLoadProgressBars[i].setValue((int) Preferences.nodes.get(nodeName).getProcessorLoad());
        }

        for (int i = 0; i < numberOfClients; i++) {
            cpuLoadPanel.add(cpuLoadProgressBars[i], gbc);
            gbc.gridy++;
            cpuLoadPanel.add(new JLabel(Preferences.node_names.get(i)), gbc);
            gbc.gridy--;
            gbc.gridx++;
            if ((i + 1) % numberOfRows == 0) {
                gbc.gridy += 2;
                gbc.gridx = 0;
            }
        }
        panel.add(batteryPanel);
        panel.add(memoryPanel);
        panel.add(cpuLoadPanel);
    }

    private void repaintPanel() {
        batteryPanel.removeAll();
        cpuLoadPanel.removeAll();
        memoryPanel.removeAll();
        panel.removeAll();
        initComponents();
        batteryPanel.revalidate();
        cpuLoadPanel.revalidate();
        memoryPanel.revalidate();
        panel.revalidate();
    }

    private void createNodeLogFrames() {

        taNodeLogArea = new JTextArea[numberOfClients];
        nodeLogTextAreaMap = new HashMap<String, JTextArea>();
        nodeScrollPane = new JScrollPane[numberOfClients];
        nodePanel = new JPanel[numberOfClients];
        nodeFrames = new JFrame[numberOfClients];
        nodeFrameMap = new HashMap<String, JFrame>();

        //Setting up logs list
        nodeLogs = new HashMap<String, ArrayList<String>>();
        for (int i = 0; i < numberOfClients; i++) {
            nodeLogs.put(Preferences.node_names.get(i), new ArrayList<String>());

            taNodeLogArea[i] = new JTextArea(10, 50);
            taNodeLogArea[i].setEditable(false);
            nodeLogTextAreaMap.put(Preferences.node_names.get(i), taNodeLogArea[i]);
            nodeScrollPane[i] = new JScrollPane(taNodeLogArea[i],
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            nodePanel[i] = new JPanel(new FlowLayout());
            nodePanel[i].add(new JLabel(Preferences.node_names.get(i).toUpperCase()));
            nodePanel[i].add(nodeScrollPane[i]);

            nodeFrames[i] = new JFrame("Logs: " + Preferences.node_names.get(i).toUpperCase());
            nodeFrameMap.put(Preferences.node_names.get(i), nodeFrames[i]);
            nodeFrames[i].setContentPane(nodePanel[i]);
            nodeFrames[i].setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            nodeFrames[i].setVisible(false);
            nodeFrames[i].setResizable(false);
            nodeFrames[i].setSize(575, 250);
        }
    }
}
