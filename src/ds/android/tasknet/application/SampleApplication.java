package ds.android.tasknet.application;

import ds.android.tasknet.config.Preferences;
import ds.android.tasknet.infrastructure.TaskDistributor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class SampleApplication implements ActionListener {

    JScrollPane scrollPane;
    JPanel mainPanel, scrollPanel, btnPanel, taskPanel;
    JLabel host_label;
    JTextArea taMessages;
    JTextField tfMethodName, tfTaskLoad;
    JFrame mainFrame;
    JButton btnDistributeTask, btnExecuteLocalTask;
    String host, configuration_file, clockType;
    Properties prop;
    TaskDistributor distributor;
    int numOfNodes = 0, host_index = 0;

    public SampleApplication(String host_name, String conf_file, String clockType) {
        prop = new Properties();
        Preferences.setHostDetails(conf_file, host_name);
        try {
            prop.load(new FileInputStream(conf_file));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        host = host_name;
        configuration_file = conf_file;
        this.clockType = clockType;

        distributor = new TaskDistributor(host, configuration_file, clockType);
        distributor.startListening();

        buildUI(conf_file);
    }

    void buildUI(String conf_file) {
        mainPanel = new JPanel();
        mainPanel.setLayout(new FlowLayout());

        host_label = new JLabel(host.toUpperCase());
        mainPanel.add(host_label);

        taskPanel = new JPanel(new FlowLayout());
        taskPanel.add(new JLabel("Method Name: "));
        tfMethodName = new JTextField(10);
        taskPanel.add(tfMethodName);
        taskPanel.add(new JLabel("Task Load: "));
        tfTaskLoad = new JTextField(10);
        taskPanel.add(tfTaskLoad);
        mainPanel.add(taskPanel);

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

        btnDistributeTask = new JButton("Distribute task");
        btnDistributeTask.addActionListener(this);
        btnExecuteLocalTask = new JButton("Execute Task Locally");
        btnExecuteLocalTask.addActionListener(this);
        btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(btnDistributeTask);
        btnPanel.add(btnExecuteLocalTask);
        mainPanel.add(btnPanel);

        mainFrame = new JFrame("TaskNet: " + host);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setResizable(false);
        mainFrame.setSize(600, 150);
        mainFrame.add(mainPanel);
        mainFrame.setContentPane(mainPanel);
        mainFrame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (tfMethodName.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter Method name", "Method Name", JOptionPane.WARNING_MESSAGE);
        } else if (tfTaskLoad.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter task load", "Task load", JOptionPane.WARNING_MESSAGE);
        }
        if (ae.getSource() == btnDistributeTask) {
            distributor.distributeTask(new Integer(tfTaskLoad.getText()));
        } else if (ae.getSource() == btnExecuteLocalTask) {
            System.out.println((new Integer((new SampleApplicationLocal()).method1(10, 20))).toString());
        }
    }
}
