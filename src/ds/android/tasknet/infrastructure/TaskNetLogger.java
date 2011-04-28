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
import java.util.Calendar;

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
    String host_name;
    Map<String, Node> nodes = new HashMap<String, Node>();
	Map<Integer, String> node_names = new HashMap<Integer, String>();
	Map<String, InetAddress> node_addresses = new HashMap<String, InetAddress>();

    public TaskNetLogger(String host_name, String conf_file) {

    	this.host_name = host_name;
        Preferences.setHostDetails(conf_file, host_name);
        try {
            mp = new MessagePasser(conf_file, host_name, InetAddress.getByName("127.0.0.1").getHostAddress(),
            		this.nodes, this.node_names, this.node_addresses);
        } catch (UnknownHostException ex) {
            Logger.getLogger(TaskNetLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
        numberOfClients = this.nodes.size();
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
        panel = new JPanel();
        taLogArea = new JTextArea(10, 60);
        taLogArea.setEditable(false);
        scrollPane = new JScrollPane(taLogArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
//        frame.setSize(800, 500);
        frame.setSize(1000, 700);

        listenForIncomingMessages();
        monitorNodeUpdates();
    }

    public void actionPerformed(ActionEvent e) {
        String nodeName = tfNodeName.getText();
        if (nodeName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter Node name", "Node name",
                    JOptionPane.WARNING_MESSAGE);
        } else if (!this.node_names.containsValue(nodeName)) {
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
                                    newNode.setNodeIndex(nodes.size());
                                    createNewLogFrame(newNode.getName());
                                    try {
                                        System.out.println(newNode.getAdrress());
                                    } catch (UnknownHostException ex) {
                                        Logger.getLogger(TaskNetLogger.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    nodes.put(msg.getSource(), newNode);
                                    try {
                                        node_addresses.put(newNode.getName(), newNode.getAdrress());
                                    } catch (UnknownHostException ex) {
                                        Logger.getLogger(TaskNetLogger.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    node_names.put(newNode.getIndex(), newNode.getName());
                                    Message bootstrapNodeList = new Message(msg.getSource(),
                                            "", "", (Serializable) nodes, host_name);
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
//                                    System.out.println("Receiving update info");
                                    Node nodeToBeUpdated = (Node) msg.getData();
                                    try{
                                    	synchronized (nodes) {
                                        int mem = (int) nodeToBeUpdated.getMemoryLoad();
                                        int procload = (int) nodeToBeUpdated.getProcessorLoad();
                                        int batterylevel = nodeToBeUpdated.getBatteryLevel();
                                        (nodes.get(nodeToBeUpdated.getName())).update(mem, procload, batterylevel);
                                    	}
                                    }catch(Exception e)
                                    {
                                    	e.getStackTrace();
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
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
        Object[] nodeList;
        synchronized (this.nodes) {
            nodeList = this.nodes.values().toArray();
        }
        numberOfClients = nodeList.length;
        if (numberOfClients > 0) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(15, 15, 15, 15);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            batteryProgressBars = new JProgressBar[numberOfClients];
            for (int i = 0; i < numberOfClients; i++) {
                batteryProgressBars[i] = new JProgressBar(JProgressBar.VERTICAL, 0, Preferences.TOTAL_BATTERY_AT_NODE);
                batteryProgressBars[i].setSize(10, 30);
                batteryProgressBars[i].setValue(((Node) nodeList[i]).getBatteryLevel());
            }

            int numberOfRows = 10;
            for (int i = 0; i < numberOfClients; i++) {
                batteryPanel.add(batteryProgressBars[i], gbc);
                gbc.gridy++;
                batteryPanel.add(new JLabel(((Node) nodeList[i]).getName()), gbc);
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
                memoryProgressBars[i] = new JProgressBar(JProgressBar.VERTICAL, 
                		0, Preferences.TOTAL_MEMORY_LOAD_AT_NODE);
                memoryProgressBars[i].setValue((int) ((Node) nodeList[i]).getMemoryLoad());
            }

            for (int i = 0; i < numberOfClients; i++) {
                memoryPanel.add(memoryProgressBars[i], gbc);
                gbc.gridy++;
                memoryPanel.add(new JLabel(((Node) nodeList[i]).getName()), gbc);
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
                cpuLoadProgressBars[i] = new JProgressBar(JProgressBar.VERTICAL, 0, 
                		Preferences.TOTAL_PROCESSOR_LOAD_AT_NODE);
                cpuLoadProgressBars[i].setValue((int) ((Node) nodeList[i]).getProcessorLoad());
            }

            for (int i = 0; i < numberOfClients; i++) {
                cpuLoadPanel.add(cpuLoadProgressBars[i], gbc);
                gbc.gridy++;
                cpuLoadPanel.add(new JLabel(((Node) nodeList[i]).getName()), gbc);
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

    private void monitorNodeUpdates() {
        (new Thread() {

            public void run() {
                while (true) {
                    try {
                        Thread.sleep(2 * Preferences.PROFILE_UPDATE_TIME_PERIOD);
                        ArrayList<String> nodesToRemove = new ArrayList<String>();
                        for (Node n : nodes.values()) {
                            synchronized (n) {
                                if (n.getLastUpdated() != null
                                        && ((Calendar.getInstance().getTime().getTime() - n.getLastUpdated().getTime())
                                        > 4 * Preferences.PROFILE_UPDATE_TIME_PERIOD)) {
                                    nodesToRemove.add(n.getName());
                                }
                            }
                        }
                        int numOfNodesToRemove = nodesToRemove.size();
                        for (int i = 0; i < numOfNodesToRemove; i++) {
                            String nodeToRemove = nodesToRemove.get(i);
                            synchronized (nodes) {
                                node_names.remove(nodes.get(nodeToRemove).getIndex());
                                nodes.remove(nodeToRemove);
                                node_addresses.remove(nodeToRemove);
                            }
                            Message removeMessage = new Message("", "", "", nodeToRemove, host_name);
                            removeMessage.setNormalMsgType(Message.NormalMsgType.REMOVE_NODE);
                            for (Node n : nodes.values()) {
                                removeMessage.setDest(n.getName());
                                mp.send(removeMessage);
                                Thread.sleep(100);
                            }
                            nodesToRemove.remove(nodeToRemove);
                        }
                        if(numOfNodesToRemove > 0)
                            repaintPanel();
                    } catch (InvalidMessageException ex) {
                        Logger.getLogger(TaskNetLogger.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TaskNetLogger.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            
        }).start();
    }
}
