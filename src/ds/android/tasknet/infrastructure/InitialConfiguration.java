package ds.android.tasknet.infrastructure;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
    JComboBox nicCombo;

    public InitialConfiguration() {
        frame = new JFrame("Multicast Lab - Initial Configuration");
        panel = new JPanel(new GridBagLayout());
        fileName = new JTextField(20);
        local_node = new JTextField(20);
        browseButton = new JButton("Browse");
        browseButton.addActionListener(this);
        startButton = new JButton("Start");
        startButton.addActionListener(this);

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
        panel.add(new JLabel("Choose the IP you want to use:"), gbc);
        ArrayList<String> nicNames = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> nic = NetworkInterface.getNetworkInterfaces();
            while(nic.hasMoreElements()){
                NetworkInterface currentNic = nic.nextElement();
                Enumeration<InetAddress> inetAddr = currentNic.getInetAddresses();
                while(inetAddr.hasMoreElements()){
                    InetAddress ip = inetAddr.nextElement();
                    if(ip instanceof Inet4Address)
                        nicNames.add(ip.getHostAddress());
                }
            }
        } catch (SocketException ex) {
            Logger.getLogger(InitialConfiguration.class.getName()).log(Level.SEVERE, null, ex);
        }
        nicCombo = new JComboBox(nicNames.toArray());
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(nicCombo,gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(startButton, gbc);

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setSize(600, 200);

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
                    new TaskNetLogger(getLocalNodeName(), getFileName());
                } else {
                    new TaskNetNode(getLocalNodeName(), getFileName(),(String)nicCombo.getSelectedItem());
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
