package ds.android.tasknet.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ds.android.tasknet.config.Preferences;
import ds.android.tasknet.distributor.TaskDistributor;
import java.io.Serializable;

public class Simlulate {

    public static void main(String[] args) {
        Simlulate s = new Simlulate();
        s.doSimulation();
    }

    private void doSimulation() {
        List<TaskDistributor> nodeList = new ArrayList<TaskDistributor>();
        int nodeCount = 3;
        for (int i = 0; i < nodeCount; i++) {
            String host = "a" + i;
            String conf_file = "C:\\personal\\shekhar\\synched\\My Dropbox\\cmu_course_work\\18842_distributed_systems\\git_repo\\TaskNet_Divya\\TaskNet\\src\\ds\\android\\tasknet\\config\\ppfile.ini";
            TaskDistributor distributor = new TaskDistributor(host, conf_file, "127.0.0.1");
            nodeList.add(distributor);
        }
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Random randomGenerator = new Random();
        int totalLoad = 0;
        for (int loop = 0; loop < 3; loop++) {
//			int from = randomGenerator.nextInt(nodeCount);
            int from = 0;
            int load;
            load = 1000;
//			load = randomGenerator.nextInt(40);
//			load += 20;

            totalLoad += load;
            System.out.println("Distributing  " + load + " from alice" + from);
            Serializable[] parameters = new Serializable[2];
            parameters[0] = 10;
            parameters[1] = 20;
            nodeList.get(from).distribute("ds.android.tasknet.application.SampleApplicationLocal", "method1",parameters, load);
            int wait = randomGenerator.nextInt(200);
            wait += 50;
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<Integer> leftBattreyList = new ArrayList<Integer>(nodeCount);
        int sum = 0;
        List<Integer> notReceiverNodes = new ArrayList<Integer>();
        notReceiverNodes.add(0);

        for (int i = 0; i < nodeCount; i++) {
            if (notReceiverNodes.contains(i)) {
                continue;
            }
            int battrey = nodeList.get(i).getNodes().get("a" + i).getBatteryLevel();
            leftBattreyList.add(battrey);
            sum += battrey;
            System.out.println(battrey);
        }
        double avgBattrey = sum / leftBattreyList.size();
        double variance = 0;
        for (int battrey : leftBattreyList) {
            variance += Math.pow((battrey - avgBattrey), 2);
        }
        double std_dev = Math.sqrt(variance / leftBattreyList.size());

        System.out.println("");
        System.out.println("Toal load distributed " + totalLoad);
        System.out.println("Avg Load " + avgBattrey + " Std Dev " + std_dev);
        System.out.println("Simulation Done.");
    }
}
