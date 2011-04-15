/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.android.tasknet.infrastructure;

import ds.android.tasknet.application.SampleApplication;
import ds.android.tasknet.config.Preferences;
import ds.android.tasknet.logger.TaskNetLogger;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Enumeration;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 *
 * @author Divya
 */
public class InitialConfiguration implements ActionListener {

    JFrame frame;
    JPanel panel;
    JTextField fileName, local_node;
    JButton browseButton, startButton;
    ButtonGroup bgClockType;
    JRadioButton rbClock[];
    JPanel radioClockPanel;

    public InitialConfiguration() {
        frame = new JFrame("Multicast Lab - Initial Configuration");
        panel = new JPanel(new GridBagLayout());
        fileName = new JTextField(20);
        local_node = new JTextField(20);
        browseButton = new JButton("Browse");
        browseButton.addActionListener(this);
        startButton = new JButton("Start");
        startButton.addActionListener(this);

        bgClockType = new ButtonGroup();
        rbClock = new JRadioButton[Preferences.numClockTypes];
        radioClockPanel = new JPanel(new FlowLayout());
        for (int i = 0; i < Preferences.numClockTypes; i++) {
            rbClock[i] = new JRadioButton(Preferences.clockTypes[i]);
            bgClockType.add(rbClock[i]);
            radioClockPanel.add(rbClock[i]);
        }
        rbClock[Preferences.numClockTypes - 1].setSelected(true);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Choose the configuration file"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(fileName, gbc);
        gbc.gridx = 2;
        gbc.gridy = 0;
        panel.add(browseButton, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Local Host Name:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(local_node, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
//        panel.add(new JLabel("Clock Type:"), gbc);
//        gbc.gridx = 1;
//        gbc.gridy = 2;
//        panel.add(radioClockPanel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(startButton, gbc);

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setSize(600, 200);

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

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == browseButton) {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                fileName.setText(file.getAbsolutePath());
            }
        } else {
            if (fileName.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Choose Configuration file", "Choose file", JOptionPane.WARNING_MESSAGE);
            } else if (local_node.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Local node name", "Enter the local node name", JOptionPane.WARNING_MESSAGE);
            } else {
                frame.dispose();
                if (getLocalNodeName().equalsIgnoreCase("logger")) {
                    TaskNetLogger coordinator = new TaskNetLogger(getLocalNodeName(), getFileName());
                    coordinator.startListening();
                } else {
//                    TaskDistributor mis = new TaskDistributor(getLocalNodeName(), getFileName(), getSelection(bgClockType));
//                    mis.startListening();
                    SampleApplication sa = new SampleApplication(getLocalNodeName(), getFileName(), getSelection(bgClockType));
                }
            }
        }
    }

    public static void main(String[] args) {
        new InitialConfiguration();
    }

    private String getFileName() {
        return fileName.getText();
    }

    private String getLocalNodeName() {
        return local_node.getText().toLowerCase();
    }
}
