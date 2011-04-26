package ds.android.tasknet.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ds.android.tasknet.config.Preferences;
import ds.android.tasknet.distributor.TaskDistributor;

public class Simlulate {

	public static void main(String[] args) {
		Simlulate s = new Simlulate();
		s.doSimulation();
	}
	
	private void doSimulation() {
		List<TaskDistributor> nodeList = new ArrayList<TaskDistributor>();
		int nodeCount = 10;
		for(int i=0;i<nodeCount;i++) {
			String host = "a" + i;
			String conf_file = "C:\\Users\\Divya_PKV\\Documents\\NetBeansProjects\\TaskNet_Dynamic_Grp\\src\\ds\\android\\tasknet\\config\\ppfile.ini";
			TaskDistributor distributor = new TaskDistributor(host, conf_file, "127.0.0.1");
			nodeList.add(distributor);
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
		
		Random randomGenerator = new Random();
		int totalLoad = 0;
		for(int loop=0;loop<50;loop++) {
//			int from = randomGenerator.nextInt(nodeCount);
			int from = 0;
			int load = randomGenerator.nextInt(40);
			load += 20;

			totalLoad += load;
			System.out.println("Distributing  " + load + " from alice" + from);
			nodeList.get(from).distribute("method1", load);
			int wait = randomGenerator.nextInt(200);
			wait += 50;
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}	
		for(int i=0;i<nodeCount;i++) {
			System.out.println(nodeList.get(i).getNodes().get("a"+i).getBatteryLevel());
		}
		System.out.println("");
		System.out.println("Toal load distributed " + totalLoad);
		System.out.println("Simulation Done.");
	}
}
