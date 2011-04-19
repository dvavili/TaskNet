package ds.android.tasknet.distributor;

import ds.android.tasknet.application.SampleApplicationLocal;
import ds.android.tasknet.config.Node;
import ds.android.tasknet.config.Preferences;
import ds.android.tasknet.exceptions.InvalidMessageException;
import ds.android.tasknet.msgpasser.Message;
import ds.android.tasknet.msgpasser.MessagePasser;
import ds.android.tasknet.msgpasser.MulticastMessage;
import ds.android.tasknet.task.DistributedTask;
import ds.android.tasknet.task.Task;
import ds.android.tasknet.task.TaskAdvReply;
import ds.android.tasknet.task.TaskChunk;
import ds.android.tasknet.task.TaskLookup;
import ds.android.tasknet.task.TaskResult;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @authors
 * Divya Vavili - dvavili@andrew.cmu.edu
 *
 */
/**
 * Test Harness to test MessagePasser
 */
public class TaskDistributor {

    int numOfNodes = 0;
    MessagePasser mp;
    String host;
    Properties prop;
    public static int taskNum = 0;
    HashMap<String, TaskLookup> taskLookups;
    Map<String, Map<Integer, TaskResult>> taskResults = new HashMap<String, Map<Integer, TaskResult>>();
    Integer taskAdvReplyId = 0;
    
    //global Network statistics
    int advReplyCount;
    float avgNodeLoad;
    float varianceNodeLoad;

    public TaskDistributor(String host_name, String conf_file, String IPaddress) {
        prop = new Properties();
        Preferences.setHostDetails(conf_file, host_name);
        try {
            prop.load(new FileInputStream(conf_file));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        mp = new MessagePasser(conf_file, host_name, IPaddress);
        host = host_name;
        taskLookups = new HashMap<String, TaskLookup>();

        listenForIncomingMessages();
        keepSendingProfileUpdates();
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
                            if (!(msg instanceof MulticastMessage)) {
                                switch (msg.getNormalMsgType()) {
                                    case TASK_ADV:
                                        Task receivedTask = (Task) msg.getData();
                                        synchronized (Preferences.nodes) {
                                            logMessage("Received task advertisement from: " + msg.getSource());
                                            Node host_node = Preferences.nodes.get(host);
                                            float remaining_load = host_node.getBatteryLevel()
                                                    - (receivedTask.taskLoad 
                                                    		/*+ host_node.getProcessorLoad()*/
                                                    		+ host_node.getPromisedLoad()
                                                    		);
                                            int loadCanServe = 0;
                                            if (remaining_load > Preferences.host_reserved_load) {
                                                loadCanServe = receivedTask.taskLoad;
                                            } else {
                                                // Need to change this to avoid fragmentation
                                                loadCanServe = host_node.getBatteryLevel()
                                                        - (Preferences.host_reserved_load 
                                                        		+ host_node.getPromisedLoad());
                                            }
                                            if (loadCanServe > Preferences.MINIMUM_LOAD_REQUEST) {
                                                Integer tempTaskAdvReplyId = ++taskAdvReplyId;
                                                String tempTaskAdvReplyIdStr = host_node.getName() 
                                                	+ tempTaskAdvReplyId;
                                                receivedTask.setPromisedTaskLoad(loadCanServe);
                                                host_node.addToAcceptedTask(tempTaskAdvReplyIdStr, receivedTask);
                                                host_node.incrPromisedLoad(loadCanServe);
                                                TaskAdvReply taskAdvReply = new TaskAdvReply(
                                                        tempTaskAdvReplyIdStr,
                                                        receivedTask.getTaskId(),
                                                        host_node, loadCanServe);
                                                Message profileMsg = new Message(
                                                        msg.getSource(),
                                                        "", "", taskAdvReply, host);
                                                profileMsg.setNormalMsgType(Message.NormalMsgType.PROFILE_XCHG);
                                                try {
                                                    System.out.println("Sent profile message from: "
                                                            + host
                                                            + " "
                                                            + Preferences.nodes.get(host).getAdrress()
                                                            + " "
                                                            + Preferences.nodes.get(host).getNodePort());
                                                    mp.send(profileMsg);
                                                } catch (InvalidMessageException ex) {
                                                    host_node.removeFromAcceptedTask(tempTaskAdvReplyIdStr);
                                                    host_node.decrPromisedLoad(loadCanServe);
                                                    ex.printStackTrace();
                                                } catch (UnknownHostException e) {
                                                    // TODO Auto-generated catch
                                                    // block
                                                    e.printStackTrace();
                                                }
                                                logMessage("Sent message profile for task: "
                                                        + receivedTask.taskId
                                                        + " "
                                                        + remaining_load);
                                            } else {
                                                logMessage("Node Overloaded: "
                                                        + receivedTask.taskId + " "
                                                        + loadCanServe);
                                            }
                                        }
                                        break;
                                    case NORMAL:
                                        logMessage(msg.getData().toString());
                                        break;
                                    case PROFILE_XCHG:
                                        TaskAdvReply taskAdvReply = (TaskAdvReply) msg.getData();
                                        System.out.println("Getting profile_xchg message from: "
                                                + taskAdvReply.getNode().getName()
                                                + " "
                                                + taskAdvReply.getNode().getAdrress()
                                                + " "
                                                + taskAdvReply.getNode().getNodePort());
                                        TaskLookup taskLookup = taskLookups.get(taskAdvReply.getTaskId());
                                        taskLookup.setRetry(0);
                                        distributeTask(taskAdvReply);
                                        break;
                                    case DISTRIBUTED_TASK:
                                        TaskChunk taskChunk = (TaskChunk) msg.getData();
                                        // taMessages.append(taskChunk.toString());
                                        DistributedTask distTask = taskChunk.getDsTask();
                                        TaskResult result;
                                        Map<Integer, TaskResult> tempResults = taskResults.get(distTask.getTaskId());
                                        // if (tempResults != null
                                        // &&
                                        // tempResults.get(distTask.getSeqNumber())
                                        // != null) {
                                        // result =
                                        // taskResults.get(distTask.getTaskId()).get(taskChunk.getSequenceNumber());
                                        // } else {
                                        result = new TaskResult(
                                                distTask.getTaskLoad(),
                                                distTask.taskId, host,
                                                handleDistributedTask(distTask),
                                                taskChunk.getSequenceNumber());

                                        if (tempResults == null) {
                                            tempResults = new HashMap<Integer, TaskResult>();
                                        }

                                        tempResults.put(
                                                taskChunk.getSequenceNumber(),
                                                result);
                                        taskResults.put(distTask.getTaskId(),
                                                tempResults);
                                        // }
                                        Node host_node = Preferences.nodes.get(host);
                                      //decrease promise
                                        if(host_node.getAcceptedTaskByTaskId(taskChunk.getTaskAdvReplyId()) != null) {
                                        	host_node.decrPromisedLoad(
                                        			host_node.getAcceptedTaskByTaskId(
                                        					taskChunk.getTaskAdvReplyId()).getPromisedTaskLoad());
                                        }
                                        host_node.removeFromAcceptedTask(taskChunk.getTaskAdvReplyId());
                                        Preferences.nodes.get(host).decrBatteryLevel(distTask.getTaskLoad() 
                                        		+ Preferences.LOAD_SPENT_IN_TASK_CHUNK_EXECUTION);
                                        
                                        logMessage(result.getTaskResult().toString());
                                        Message resultMsg = new Message(distTask.getSource(), "", "", 
                                        		result, host_node.getName());
                                        resultMsg.setNormalMsgType(Message.NormalMsgType.TASK_RESULT);
                                        try {
                                            mp.send(resultMsg);
                                        } catch (InvalidMessageException ex) {
                                            Logger.getLogger(TaskDistributor.class.getName()).log(Level.SEVERE, 
                                            		null, ex);
                                        }
                                        break;
                                    case TASK_RESULT:
                                        TaskResult taskResult = (TaskResult) msg.getData();
                                        Integer seqNumber = taskResult.getSeqNumber();
                                        taskLookup = taskLookups.get(taskResult.getTaskId());
                                        synchronized (taskLookup) {
                                            taskLookup.getTaskGroup().get(taskLookup.getResultTracker()
                                            		.get(taskResult.getSeqNumber()))
                                            		.setStatus(Preferences.TASK_CHUNK_STATUS.RECEIVED);
                                            taskLookup.removeFromResultTracker(seqNumber);
                                            addAndMergeResults(taskResult);
                                        }
                                        logMessage("Sequence Number: " + seqNumber
                                                + " Result tracker: " + taskLookup.printResultTracker()
                                                + " Result from: " + taskResult.getSource()
                                                + " ==> "
                                                + taskResult.getTaskResult());

                                        //Merger all results
                                        //if received all result, remove taskLookup
                                        break;
                                }
                            }
                        }
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    } catch (UnknownHostException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            private Serializable handleDistributedTask(DistributedTask gotTask) {
                try {
                    Class cl = Class.forName(gotTask.getClassName());
                    HashMap<String, Class[]> mthdDef = new HashMap<String, Class[]>();
                    Method mthds[] = cl.getDeclaredMethods();
                    for (Method m : mthds) {
                        mthdDef.put(m.getName(), m.getParameterTypes());
                    }
                    if (gotTask.getParameters() != null && ((mthdDef.get(gotTask.getMethodName())).length 
                    		!= gotTask.getParameters().length)) {
                        logMessage("Parameters don\'t match");
                    } else {
                        Class params[] = mthdDef.get(gotTask.getMethodName());
                        Object parameters[] = (Object[]) gotTask.getParameters();
                        try {
                            Method invokedMethod = cl.getMethod(gotTask.getMethodName(), params);
                            return (Serializable) invokedMethod.invoke(new SampleApplicationLocal(), parameters);
                        } catch (IllegalAccessException ex) {
                            ex.printStackTrace();
                        } catch (InvocationTargetException ex) {
                            ex.printStackTrace();
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                return null;
            }

            private void distributeTask(TaskAdvReply taskAdvReply) {
                String taskId = taskAdvReply.getTaskId();
                Node node = taskAdvReply.getNode();
                TaskLookup taskLookup = taskLookups.get(taskId);
                Task taskToDistribute = (taskLookups.get(taskId)).getTask();

                // Check this code
                if (taskToDistribute.getTaskLoad() <= 0) {
                    return;
                }
                //------------------------
                if (taskLookup.getStatus() == Preferences.TASK_STATUS.DISTRIBUTED) {
                    return;
                }
                
                //should this node be assigned some task
                if(!isNodeSuitableForDistriution(node, taskLookup))
                	return;

                int loadDistributed = (int) Math.ceil((taskToDistribute.getTaskLoad() 
                		> taskAdvReply.getLoadCanServe())
                        ? (taskAdvReply.getLoadCanServe()) : taskToDistribute.getTaskLoad());

                taskToDistribute.setTaskLoad(taskToDistribute.getTaskLoad() - loadDistributed);
                if (taskToDistribute.getTaskLoad() <= 0) {
                    taskLookup.setStatus(Preferences.TASK_STATUS.DISTRIBUTED);
                }

                Serializable[] parameters = new Serializable[2];
                parameters[0] = 10;
                parameters[1] = 20;
                DistributedTask dsTask = new DistributedTask(loadDistributed, taskId,
                        taskToDistribute.getSource(),
                        "ds.android.tasknet.application.SampleApplicationLocal", "method1", parameters);
                TaskChunk taskChunk = new TaskChunk(taskId, node, taskLookup.nextSequenceNumber(),
                        dsTask, taskAdvReply.getTaskAdvReplyId());
                taskLookup.addToTaskGroup(taskChunk.getTaskAdvReplyId(), taskChunk);
                Message distMsg = new Message(node.getName(), "", "", taskChunk, host);
                distMsg.setNormalMsgType(Message.NormalMsgType.DISTRIBUTED_TASK);

                try {
                    System.out.println("Sending Distributed_task message from: "
                            + node.getName() + " " + node.getAdrress()
                            + " " + node.getNodePort());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                sendAndRetryTaskChunk(taskChunk, distMsg, dsTask, loadDistributed, taskLookup);
            }
            
            private boolean isNodeSuitableForDistriution(Node node, TaskLookup taskLookup) {
//            	return isNodeSuitableForNaiveDistriutionCheck(node, taskLookup);
            	return isNodeSuitableForEfficientDistriutionCheck(node, taskLookup);
            }

            private boolean isNodeSuitableForNaiveDistriutionCheck(Node node, TaskLookup taskLookup) {
            	//first come-first server
            	//all nodes are suitable, doesn't consider node-health
            	return true;            	
            }
            
            private boolean isNodeSuitableForEfficientDistriutionCheck(Node node, TaskLookup taskLookup) {
            	//keep global avg, std dev, count
            	// if its within range, select and update avg, variance by weight 1 else by 0.5
            	boolean isSuitable = true;
            	float alpha = 1.0f;
            	float beta = 1.0f;
            	float gama = 0.5f;
            	float weight = beta;
            	            	
            	if(advReplyCount > 1) {
            		double stdDevNodeLoad = Math.sqrt(varianceNodeLoad/advReplyCount); 	            	
	            	if(Math.abs(node.getBatteryLevel() - avgNodeLoad) >  alpha*stdDevNodeLoad) {
	            		//out of range don't choose this node
	            		isSuitable = false;
	            	}
            	}
            	            	
            	advReplyCount++;
            	float lastAvgNodeLoad = avgNodeLoad;
            	
            	if(!isSuitable)
            		weight = gama;
            	
            	avgNodeLoad = avgNodeLoad + weight*((node.getBatteryLevel() - avgNodeLoad)/advReplyCount);
            	if(advReplyCount == 1) 
            		varianceNodeLoad = 0;
            	else
            		varianceNodeLoad = varianceNodeLoad 
            			+ weight * ((node.getBatteryLevel() - lastAvgNodeLoad) 
            							* (node.getBatteryLevel() - avgNodeLoad));
            	
            	return isSuitable;            	
            }
                        
            
            private void addAndMergeResults(TaskResult taskResult) {
                TaskLookup taskLookup = taskLookups.get(taskResult.getTaskId());
                taskLookup.getTaskResults().put(taskResult.getSeqNumber(), taskResult);
                if (taskLookup.getTask().getTaskLoad() <= 0
                        && taskLookup.getTaskGroup().size() == taskLookup.getTaskResults().size()) {
                    taskLookup.setStatus(Preferences.TASK_STATUS.RECEIVED_RESULTS);
                }
                //Merge results
                if (taskLookup.getStatus() == Preferences.TASK_STATUS.RECEIVED_RESULTS) {
                    String result = "";
                    for (int i = 0; i < taskLookup.getTaskResults().size(); i++) {
                        result += (taskLookup.getTaskResults().get(i)).toString() + " ";
                    }
                    logMessage("Result:\n" + result);
                    Preferences.nodes.get(host).decrBatteryLevel(Preferences.LOAD_SPENT_IN_TASK_DISTRIBUTION);
                }
            }
        }).start();
    }

    private void logMessage(String msgString) {
        Message logMsg = new Message(Preferences.LOGGER_NAME, "", "", msgString + "\n", host);
        logMsg.setLogSource(host);
        logMsg.setNormalMsgType(Message.NormalMsgType.LOG_MESSAGE);
        try {
            mp.send(logMsg);
        } catch (InvalidMessageException ex) {
            Logger.getLogger(TaskDistributor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendAndRetryTaskChunk(final TaskChunk taskChunk, final Message distMsg,
            final DistributedTask taskToDistribute, final int loadDistributed, final TaskLookup taskLookup) {

        (new Thread() {

            public void run() {
                while (taskChunk.getStatus() != Preferences.TASK_CHUNK_STATUS.RECEIVED
                        && taskChunk.getRetry() < Preferences.NUMBER_OF_RETRIES_BEFORE_QUITTING) {

                    try {
                        taskLookups.get(((DistributedTask) taskToDistribute).getTaskId())
                        	.addToResultTracker(taskChunk.getSequenceNumber(), taskChunk.getTaskAdvReplyId());
                        mp.send(distMsg);
                        taskChunk.incrRetry();
                        Thread.sleep(Preferences.WAIT_TIME_BEFORE_RETRYING);
                    } catch (InvalidMessageException ex) {
                        ex.printStackTrace();
                    } catch (InterruptedException e) {
                        // do nothing
                        e.printStackTrace();
                    }
                }
                if (taskChunk.getStatus() != Preferences.TASK_CHUNK_STATUS.RECEIVED) {
                    taskToDistribute.setTaskLoad(taskToDistribute.getTaskLoad() + loadDistributed);
                    if (taskToDistribute.getTaskLoad() > 0) {
                        taskLookup.setStatus(Preferences.TASK_STATUS.ADVERTISED);
                    }
                }
            }
        }).start();
    }

    private void keepSendingProfileUpdates() {
        (new Thread() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(Preferences.PROFILE_UPDATE_TIME_PERIOD);
                        try {
                            synchronized (Preferences.nodes) {
                                Node updateNode = Preferences.nodes.get(host);
                                Message profileUpdate = new Message(Preferences.LOGGER_NAME, "", "", updateNode, host);
                                profileUpdate.setNormalMsgType(Message.NormalMsgType.PROFILE_UPDATE);
                                mp.send(profileUpdate);
                            }
                        } catch (InvalidMessageException ex) {
                            Logger.getLogger(TaskDistributor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TaskDistributor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }).start();
    }

    public void distribute(String methodName, Integer taskLoadFromUI) {
        String nodeName = "";
        String msgKind = "";
        String msgId = "";
        taskNum++;
        String taskId = host + taskNum;
        Task newTask = new Task(taskLoadFromUI, taskId, host);
        logMessage("Task advertised");
        TaskLookup taskLookup = new TaskLookup(newTask);
        synchronized (taskLookups) {
            taskLookups.put(taskId, taskLookup);
        }
        Message advMsg = new Message(nodeName, msgKind, msgId,
                newTask, host);
        advMsg.setSource(host);
        advMsg.setNormalMsgType(Message.NormalMsgType.TASK_ADV);
        sendAndRetryTaskAdv(taskLookup, advMsg);
        Preferences.crashNode = "";
    }

    private void sendAndRetryTaskAdv(final TaskLookup taskLookup,
            final Message advMsg) {

        (new Thread() {

            public void run() {
                // if it still hasn't exhausted its retry and still hasn't got
                // enough reply
                while (taskLookup.getStatus() == Preferences.TASK_STATUS.ADVERTISED
                        && taskLookup.getRetry() < Preferences.NUMBER_OF_RETRIES_BEFORE_QUITTING) {

                    logMessage("Tasklookup status : " + taskLookup.getStatus()
                            + " Retries: " + taskLookup.getRetry());
                    // send task request advertisement, ask for bid
                    try {
                        synchronized(mp){
                            Collection<Node> nodes = Preferences.nodes.values();
                            for(Node n : nodes){
                                if(!n.getName().equalsIgnoreCase(host)){
                                advMsg.setDest(n.getName());
                                mp.send(advMsg);
                                Thread.sleep(100);
                                }
                            }
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TaskDistributor.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InvalidMessageException e) {
                        e.printStackTrace();
                    }
                    taskLookup.incrRetry();
                    try {
                        if (taskLookup.getRetry() < Preferences.NUMBER_OF_RETRIES_BEFORE_QUITTING) {
                            Thread.sleep(Preferences.WAIT_TIME_BEFORE_RETRYING);
                        }
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
    }
}
