package ds.android.tasknet.infrastructure;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import ds.android.tasknet.config.Node;
import ds.android.tasknet.config.Preferences;
import ds.android.tasknet.exceptions.InvalidMessageException;
import ds.android.tasknet.msgpasser.Message;
import ds.android.tasknet.msgpasser.MessagePasser;
import java.net.InetAddress;

/**
 *
 * @author Divya
 */
public class TaskNetLogger implements ActionListener {

    JButton btnShowLog;
    JFrame frame;
    JScrollPane scrollPane;
    JPanel mainPanel, batteryPanel, clientPanel, memoryPanel, cpuLoadPanel;
    JPanel scrollPanel, nodeNamePanel, panel;
    JProgressBar batteryProgressBars[], memoryProgressBars[], cpuLoadProgressBars[];
    int numberOfClients = 0;
    JTextField tfNodeName;
    JTextArea taLogArea;
    TitledBorder batteryTitle, memoryTitle, cpuLoadTitle;
    Map<String, JTextArea> nodeLogTextAreaMap;
    Map<String, JFrame> nodeFrameMap;
    Map<String, ArrayList<String>> nodeLogs;
    MessagePasser mp;

    public TaskNetLogger(String host_name, String conf_file) {

        Preferences.setHostDetails(conf_file, host_name);
        try {
            mp = new MessagePasser(conf_file, host_name, InetAddress.getByName("127.0.0.1").getHostAddress());
        } catch (UnknownHostException ex) {
            Logger.getLogger(TaskNetLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
        numberOfClients = Preferences.nodes.size();
        nodeLogTextAreaMap = new HashMap<String, JTextArea>();
        nodeFrameMap = new HashMap<String, JFrame>();
        nodeLogs = new HashMap<String, ArrayList<String>>();

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

        listenForIncomingMessages();
    }

    public void actionPerformed(ActionEvent e) {
        String nodeName = tfNodeName.getText();
        if (nodeName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter Node name", "Node name",
                    JOptionPane.WARNING_MESSAGE);
        } else if (!Preferences.node_names.containsValue(nodeName)) {
            JOptionPane.showMessageDialog(null,
                    "Node does not exist in the system", "Node name",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            nodeFrameMap.get(nodeName).setVisible(true);
        }
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
                            switch (msg.getNormalMsgType()) {
                                case BOOTSTRAP:
                                    Node newNode = (Node) msg.getData();
                                    newNode.setNodeIndex(Preferences.nodes.size());
                                    createNewLogFrame(newNode.getName());
                                    try {
                                        System.out.println(newNode.getAdrress());
                                    } catch (UnknownHostException ex) {
                                        Logger.getLogger(TaskNetLogger.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    Preferences.nodes.put(msg.getSource(), newNode);
                                    try {
                                        Preferences.node_addresses.put(newNode.getName(), newNode.getAdrress());
                                    } catch (UnknownHostException ex) {
                                        Logger.getLogger(TaskNetLogger.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    Preferences.node_names.put(newNode.getIndex(), newNode.getName());
                                    Message bootstrapNodeList = new Message(msg.getSource(), "", "", (Serializable) Preferences.nodes);
                                    bootstrapNodeList.setNormalMsgType(Message.NormalMsgType.BOOTSTRAP_NODE_LIST);
                                    try {
                                        mp.send(bootstrapNodeList);
                                    } catch (InvalidMessageException ex) {
                                        Logger.getLogger(TaskNetLogger.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    break;
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
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void createNewLogFrame(String nodeName) {
        JTextArea taNodeLog;
        JScrollPane nodeScrollPane;
        JPanel nodePanel = new JPanel(new FlowLayout());
        JFrame nodeFrame;

        taNodeLog = new JTextArea(10, 50);
        taNodeLog.setEditable(false);

        nodeScrollPane = new JScrollPane(taNodeLog,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        nodePanel.add(new JLabel(nodeName.toUpperCase()));
        nodePanel.add(nodeScrollPane);

        nodeFrame = new JFrame("Logs: " + nodeName.toUpperCase());
        nodeFrame.setContentPane(nodePanel);
        nodeFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        nodeFrame.setVisible(false);
        nodeFrame.setResizable(false);
        nodeFrame.setSize(575, 250);

        nodeLogs.put(nodeName, new ArrayList<String>());
        nodeLogTextAreaMap.put(nodeName, taNodeLog);
        nodeFrameMap.put(nodeName, nodeFrame);
    }

    private void initComponents() {
        numberOfClients = Preferences.nodes.size();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        batteryProgressBars = new JProgressBar[numberOfClients];
        for (int i = 0; i < numberOfClients; i++) {
            batteryProgressBars[i] = new JProgressBar(JProgressBar.VERTICAL, 0,
                    100);
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
        /* Memory load of Nodes */
        /*-----------------------------------------------------------------------*/
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        memoryProgressBars = new JProgressBar[numberOfClients];
        for (int i = 0; i < numberOfClients; i++) {
            memoryProgressBars[i] = new JProgressBar(JProgressBar.VERTICAL, 0,
                    100);
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
        /* Processor load of Nodes */
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
}
