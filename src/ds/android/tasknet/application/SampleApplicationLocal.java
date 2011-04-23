/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ds.android.tasknet.application;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import ds.android.tasknet.config.Preferences;

/**
 *
 * @author Divya_PKV
 */
public class SampleApplicationLocal {

    public int method1(int a, int b) {
    	if(Preferences.DEBUG_MODE)
    		System.out.println("Method1: a = " + a + " b = " + b);
        return a + b;
    }

    public void method2() {
        System.out.println("Method2");
    }

    public void method3() {
        System.out.println("Method3");
    }

    public void method4() {
        System.out.println("Method4");
    }
}
