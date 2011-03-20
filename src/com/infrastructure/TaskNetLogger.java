/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.infrastructure;

import com.conf.Preferences;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;

/**
 *
 * @author Divya
 */
public class TaskNetLogger {

    JFrame frame;
    JPanel panel;
    static Vector<String> vClients;
    JProgressBar clientProgressBars[];
    int numberOfClients = 0;

    public TaskNetLogger() {
        frame = new JFrame("TaskNet - Logger");
        vClients = new Vector<String>();
        panel = new JPanel(new GridBagLayout());
        
        initComponents();

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setSize(600, 700);
    }

    public static void addClient(String clientName){
        synchronized(vClients){
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

    public static void main(String[] args) {
        new TaskNetLogger();
//        (new InitialConfiguration()).addClients();
    }

    private void initComponents() {
        
        numberOfClients = vClients.size();
        clientProgressBars = new JProgressBar[numberOfClients];
        for (int i = 0; i < numberOfClients; i++) {
            clientProgressBars[i] = new JProgressBar(JProgressBar.VERTICAL, 0, 100);
            clientProgressBars[i].setValue(50);
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.gridy = 0;

        int numberOfRows = 10;
        for (int i = 0; i < numberOfClients; i++) {
            panel.add(clientProgressBars[i], gbc);
            gbc.gridy++;
            panel.add(new JLabel(vClients.elementAt(i)), gbc);
            gbc.gridy--;
            gbc.gridx++;
            if ((i + 1) % numberOfRows == 0) {
                gbc.gridy += 2;
                gbc.gridx = 0;
            }
        }

        listenForClients();
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
                                Logger.getLogger(TaskNetLogger.class.getName()).log(Level.SEVERE, null, ex);
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
