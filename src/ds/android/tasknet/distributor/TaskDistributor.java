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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
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
    Map<String, Node> nodes = new HashMap<String, Node>();
    Map<Integer, String> node_names = new HashMap<Integer, String>();
    Map<String, InetAddress> node_addresses = new HashMap<String, InetAddress>();
    //global Network statistics
    int advReplyTotalCount = 0;
    int advReplyCount;
    float avgNodeLoad;
    float varianceNodeLoad;
    Map<String, Queue<BatteryInfo>> batteryLoadInfo = new HashMap<String, Queue<BatteryInfo>>();

    public TaskDistributor(String host_name, String conf_file, String IPaddress) {
        prop = new Properties();
        Preferences.setHostDetails(conf_file, host_name);
        try {
            prop.load(new FileInputStream(conf_file));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        mp = new MessagePasser(conf_file, host_name, IPaddress, this.nodes, this.node_names, this.node_addresses);
        host = host_name;
        taskLookups = new HashMap<String, TaskLookup>();

        listenForIncomingMessages();
        keepSendingProfileUpdates();
        clearDeadPromisedLoad();
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
                                        synchronized (nodes) {
                                            logMessage("Received task advertisement from: " + msg.getSource()
                                                    + " for load " + receivedTask.getTaskBatteryLoad());
                                            Node host_node = nodes.get(host);
                                            if (receivedTask.getTaskBatteryLoad() <= 0) {
                                                System.out.println(" node " + host_node.getName()
                                                        + " recevied task of battery load "
                                                        + receivedTask.getTaskBatteryLoad());
                                                break;
                                            }
                                            if (receivedTask.getTaskProcessorLoad()
                                                    > (Preferences.TOTAL_PROCESSOR_LOAD_AT_NODE
                                                    - host_node.getProcessorLoad()
                                                    - Preferences.RESERVED_PROCESSOR_AT_NODE)) {

                                                logMessage("Node Processor Overloaded: "
                                                        + receivedTask.taskId + " require "
                                                        + receivedTask.getTaskProcessorLoad()
                                                        + " but only available "
                                                        + (Preferences.TOTAL_PROCESSOR_LOAD_AT_NODE
                                                        - host_node.getProcessorLoad()
                                                        - Preferences.RESERVED_PROCESSOR_AT_NODE));

                                                break;
                                            }

                                            if (receivedTask.getTaskMemoryLoad()
                                                    > (Preferences.TOTAL_MEMORY_LOAD_AT_NODE
                                                    - host_node.getMemoryLoad()
                                                    - Preferences.RESERVED_MEMORY_AT_NODE)) {

                                                logMessage("Node Memory Overloaded: "
                                                        + receivedTask.taskId + " require "
                                                        + receivedTask.getTaskMemoryLoad()
                                                        + " but only available "
                                                        + (Preferences.TOTAL_MEMORY_LOAD_AT_NODE
                                                        - host_node.getMemoryLoad()
                                                        - Preferences.RESERVED_MEMORY_AT_NODE));

                                                break;
                                            }


                                            int remaining_battery_load = host_node.getBatteryLevel()
                                                    - (receivedTask.getTaskBatteryLoad()
                                                    + Preferences.BATTERY_SPENT_IN_TASK_CHUNK_EXECUTION
                                                    + host_node.getPromisedBatteryLoad());
                                            int batteryLoadCanServe = 0;
                                            if (remaining_battery_load > Preferences.RESERVED_BATTERY_AT_NODE) {
                                                batteryLoadCanServe = receivedTask.getTaskBatteryLoad();
                                            } else {
                                                // Need to change this to avoid fragmentation
                                                batteryLoadCanServe = host_node.getBatteryLevel()
                                                        - (Preferences.RESERVED_BATTERY_AT_NODE
                                                        + Preferences.BATTERY_SPENT_IN_TASK_CHUNK_EXECUTION
                                                        + host_node.getPromisedBatteryLoad());
                                            }
                                            if (batteryLoadCanServe > Preferences.MAX_TASK_CHUNK_LOAD_SIZE) {
                                                batteryLoadCanServe = Preferences.MAX_TASK_CHUNK_LOAD_SIZE;
                                            }

                                            if (batteryLoadCanServe > Preferences.MINIMUM_FRAGMENTATION_LOAD) {
                                                Integer tempTaskAdvReplyId = ++taskAdvReplyId;
                                                String tempTaskAdvReplyIdStr = host_node.getName()
                                                        + tempTaskAdvReplyId;
                                                receivedTask.setPromisedTaskBatteryLoad(batteryLoadCanServe
                                                        + Preferences.BATTERY_SPENT_IN_TASK_CHUNK_EXECUTION);
                                                receivedTask.setPromisedTimeStamp(new Date().getTime());
                                                host_node.addToAcceptedTask(tempTaskAdvReplyIdStr, receivedTask);
                                                host_node.incrPromisedBatteryLoad(receivedTask.getPromisedBatteryTaskLoad());

                                                //add processor, memory load
                                                host_node.incrProcessorLoad(receivedTask.getTaskProcessorLoad());
                                                host_node.incrMemoryLoad(receivedTask.getTaskMemoryLoad());

                                                TaskAdvReply taskAdvReply = new TaskAdvReply(
                                                        tempTaskAdvReplyIdStr,
                                                        receivedTask.getTaskId(),
                                                        host_node, batteryLoadCanServe);
                                                Message profileMsg = new Message(
                                                        msg.getSource(),
                                                        "", "", taskAdvReply, host);
                                                profileMsg.setNormalMsgType(Message.NormalMsgType.PROFILE_XCHG);
                                                try {
                                                    if (Preferences.DEBUG_MODE) {
                                                        System.out.println("Sent profile message from: "
                                                                + host
                                                                + " "
                                                                + nodes.get(host).getAdrress()
                                                                + " "
                                                                + nodes.get(host).getNodePort());
                                                    }
                                                    mp.send(profileMsg);
                                                } catch (InvalidMessageException ex) {
                                                    host_node.removeFromAcceptedTask(tempTaskAdvReplyIdStr);
                                                    host_node.decrProcessorLoad(receivedTask.getTaskProcessorLoad());
                                                    host_node.decrMemoryLoad(receivedTask.getTaskMemoryLoad());
                                                    host_node.decrPromisedBatteryLoad(
                                                            receivedTask.getPromisedBatteryTaskLoad());
                                                    ex.printStackTrace();
                                                } catch (UnknownHostException e) {
                                                    // block
                                                    e.printStackTrace();
                                                }
                                                logMessage("Sent message profile for task: "
                                                        + receivedTask.taskId
                                                        + " "
                                                        + remaining_battery_load);
                                            } else {
                                                System.out.println("Node Overloaded " + host_node.getName()
                                                        + " batteryLoadCanServe " + batteryLoadCanServe
                                                        + " remaining_battery_load " + remaining_battery_load
                                                        + " received load " + receivedTask.getTaskBatteryLoad()
                                                        + " prmised load " + host_node.getPromisedBatteryLoad());
                                                logMessage("Node Overloaded: "
                                                        + receivedTask.taskId + " "
                                                        + batteryLoadCanServe);
                                            }
                                        }
                                        break;
                                    case NORMAL:
                                        logMessage(msg.getData().toString());
                                        break;
                                    case PROFILE_XCHG:
                                        TaskAdvReply taskAdvReply = (TaskAdvReply) msg.getData();
                                        if (Preferences.DEBUG_MODE) {
                                            System.out.println("Getting profile_xchg message from: "
                                                    + taskAdvReply.getNode().getName()
                                                    + " "
                                                    + taskAdvReply.getNode().getAdrress()
                                                    + " "
                                                    + taskAdvReply.getNode().getNodePort());
                                        }
                                        TaskLookup taskLookup = taskLookups.get(taskAdvReply.getTaskId());
                                        taskLookup.setRetry(0);
                                        distributeTask(taskAdvReply);
                                        break;
                                    case DISTRIBUTED_TASK:
                                        TaskChunk taskChunk = (TaskChunk) msg.getData();
                                        // taMessages.append(taskChunk.toString());
                                        DistributedTask distTask = taskChunk.getDsTask();
                                        Node host_node = nodes.get(host);

                                        TaskResult result;
                                        Map<Integer, TaskResult> tempResults = taskResults.get(distTask.getTaskId());
                                        if (tempResults != null && tempResults.get(distTask.getSeqNumber()) != null) {
                                            result = taskResults.get(distTask.getTaskId()).get(taskChunk.getSequenceNumber());
                                        } else {
                                            //check if we have accepted its request already
                                            //if not, do nothing
                                            if (host_node.getAcceptedTaskByTaskId(taskChunk.getTaskAdvReplyId()) == null) {
                                                break;
                                            }
                                            Task acceptedTask = host_node.getAcceptedTaskByTaskId(taskChunk.getTaskAdvReplyId());
                                            acceptedTask.setInExecution(true);
                                            result = new TaskResult(
                                                    acceptedTask.getTaskProcessorLoad(),
                                                    acceptedTask.getTaskMemoryLoad(),
                                                    acceptedTask.getPromisedBatteryTaskLoad(),
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


                                            //we are finish with task-chunk execution
                                            //decrease promise
                                            int loadServed = host_node.getAcceptedTaskByTaskId(
                                                    taskChunk.getTaskAdvReplyId()).getPromisedBatteryTaskLoad();
                                            if (host_node.getAcceptedTaskByTaskId(taskChunk.getTaskAdvReplyId()) != null) {
                                                host_node.decrPromisedBatteryLoad(loadServed);
                                            }
                                            host_node.removeFromAcceptedTask(taskChunk.getTaskAdvReplyId());
                                            //decrease battery load
                                            nodes.get(host).decrBatteryLevel(loadServed);
                                            //decrease processor/memory load
                                            host_node.decrProcessorLoad(distTask.getTaskProcessorLoad());
                                            host_node.decrMemoryLoad(distTask.getTaskMemoryLoad());
                                        }

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
                                            taskLookup.getTaskGroup().get(taskLookup.getResultTracker().get(taskResult.getSeqNumber())).setStatus(Preferences.TASK_CHUNK_STATUS.RECEIVED);
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
                        e.printStackTrace();
                    }
                }
            }

            private synchronized void distributeTask(TaskAdvReply taskAdvReply) {
                final String taskId = taskAdvReply.getTaskId();
                Node node = taskAdvReply.getNode();
                TaskLookup taskLookup = taskLookups.get(taskId);
                Task taskToDistribute = (taskLookups.get(taskId)).getTask();

                // Check this code
                if (taskToDistribute.getTaskBatteryLoad() <= 0) {
                    return;
                }
                //------------------------
                if (taskLookup.getStatus() == Preferences.TASK_STATUS.DISTRIBUTED) {
                    return;
                }

                //should this node be assigned some task
                if (!isNodeSuitableForDistribution(node, taskLookup)) {
                    return;
                }

                final int loadDistributed = (int) Math.ceil((taskToDistribute.getTaskBatteryLoad()
                        > taskAdvReply.getLoadCanServe())
                        ? (taskAdvReply.getLoadCanServe()) : taskToDistribute.getTaskBatteryLoad());

                taskToDistribute.setTaskBatteryLoad(taskToDistribute.getTaskBatteryLoad() - loadDistributed);
//                if (taskToDistribute.getTaskBatteryLoad() <= Preferences.MINIMUM_FRAGMENTATION_LOAD) {
//                    if (taskToDistribute.getTaskBatteryLoad() > 0) {
//                        (new Thread() {
//
//                            public void run() {
//                                executeMethodLocally(taskId, loadDistributed);
//                            }
//                        }).start();
//                        taskLookup.setStatus(Preferences.TASK_STATUS.DISTRIBUTED);
//                        return;
//                    }
//                    taskLookup.setStatus(Preferences.TASK_STATUS.DISTRIBUTED);
//                }

                Serializable[] parameters = new Serializable[2];
                parameters[0] = 10;
                parameters[1] = 20;
                DistributedTask dsTask = new DistributedTask(taskToDistribute.getTaskProcessorLoad(),
                        taskToDistribute.getTaskMemoryLoad(),
                        loadDistributed, taskId,
                        taskToDistribute.getSource(),
                        taskLookup.getClassName(), taskLookup.getMethodName(), taskLookup.getParams());
//                DistributedTask dsTask = new DistributedTask(taskToDistribute.getTaskProcessorLoad(),
//                        taskToDistribute.getTaskMemoryLoad(),
//                        loadDistributed, taskId,
//                        taskToDistribute.getSource(),
//                        "ds.android.tasknet.application.SampleApplicationLocal", "method1", parameters);
                TaskChunk taskChunk = new TaskChunk(taskId, node, taskLookup.nextSequenceNumber(),
                        dsTask, taskAdvReply.getTaskAdvReplyId());
                taskLookup.addToTaskGroup(taskChunk.getTaskAdvReplyId(), taskChunk);
                Message distMsg = new Message(node.getName(), "", "", taskChunk, host);
                distMsg.setNormalMsgType(Message.NormalMsgType.DISTRIBUTED_TASK);

                if (Preferences.DEBUG_MODE) {
                    try {
                        System.out.println("Sending Distributed_task message from: "
                                + node.getName() + " " + node.getAdrress()
                                + " " + node.getNodePort());
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
                sendAndRetryTaskChunk(taskChunk, distMsg, dsTask, loadDistributed, taskLookup);
            }

            private boolean isNodeSuitableForDistribution(Node node, TaskLookup taskLookup) {
//                return isNodeSuitableForNaiveDistributionCheck(node, taskLookup);
                return isNodeSuitableForEfficientDistributionCheck(node, taskLookup);
            }

            private boolean isNodeSuitableForNaiveDistributionCheck(Node node, TaskLookup taskLookup) {
                //first come-first server
                //all nodes are suitable, doesn't consider node-health
                return true;
            }

            private boolean isNodeSuitableForEfficientDistributionCheck(Node node, TaskLookup taskLookup) {
                //keep global avg, std dev, count
                // if its within range, select and update avg, variance by weight 1 else by 0.5
                boolean isSuitable = true;
                float alpha = Preferences.ALPHA_MIN;
                float beta = Preferences.BETA;
                float gama = Preferences.GAMA;
                float weight = beta;
                BatteryInfo oldBatteryInfo = null;
                float lastAvgNodeLoad;

                Queue<BatteryInfo> nodeBatteryLoadInfo = batteryLoadInfo.get(node.getName());
                if (nodeBatteryLoadInfo == null) {
                    nodeBatteryLoadInfo = new LinkedList<BatteryInfo>();
                    batteryLoadInfo.put(node.getName(), nodeBatteryLoadInfo);
                }
                if (nodeBatteryLoadInfo.size() >= Preferences.NODE_BATTERY_INFO_QUEUE_SIZE) {
                    oldBatteryInfo = nodeBatteryLoadInfo.remove();
                }

                advReplyTotalCount++;

                if (advReplyCount > 1) {
                    double stdDevNodeLoad = Math.sqrt(varianceNodeLoad / advReplyCount);
                    //forget about old
                    if (oldBatteryInfo != null) {
                        advReplyCount--;
                        lastAvgNodeLoad = avgNodeLoad;
                        avgNodeLoad = avgNodeLoad
                                - (oldBatteryInfo.getWeight()
                                * ((oldBatteryInfo.getBatteryLoad() - avgNodeLoad) / advReplyCount));

                        varianceNodeLoad = varianceNodeLoad
                                - (oldBatteryInfo.getWeight() * ((oldBatteryInfo.getBatteryLoad() - lastAvgNodeLoad)
                                * (oldBatteryInfo.getBatteryLoad() - avgNodeLoad)));

                    }
                    //in start, avg can fluctuate very much
                    // so adjust alpha according to that
                    if (advReplyTotalCount < Preferences.NUMBER_PACKETS_NETWORK_STABLIZE) {
                        alpha = Preferences.ALPHA_MIN
                                + (((Preferences.NUMBER_PACKETS_NETWORK_STABLIZE - advReplyTotalCount)
                                / (Preferences.NUMBER_PACKETS_NETWORK_STABLIZE * 1.0f))
                                * (Preferences.ALPHA_MAX - Preferences.ALPHA_MIN));
                    }
                    //if number of nodes are less, network is more volatile
                    alpha = alpha + (22 - nodes.size()) * (Preferences.ALPHA_SAFE - Preferences.ALPHA_MIN) / 20.0f;

                    if ((node.getBatteryLevel() - avgNodeLoad) > (Preferences.ALPHA_POSITIVE_RANGE * stdDevNodeLoad)) {
                        //out of range don't choose this node
                        isSuitable = true;
                    } else if (Math.abs(node.getBatteryLevel() - avgNodeLoad) > (alpha * stdDevNodeLoad)) {
                        //out of range don't choose this node
                        isSuitable = false;
                    }
                    System.out.println("node " + node.getName() + ": battery "
                            + node.getBatteryLevel() + " avgNodeLoad " + avgNodeLoad
                            + " stdDevNodeLoad " + stdDevNodeLoad + " alpha " + alpha
                            + " isSuitable " + isSuitable);
                }

                advReplyCount++;
                lastAvgNodeLoad = avgNodeLoad;

                if (!isSuitable) {
                    weight = gama;
                }

                nodeBatteryLoadInfo.add(new BatteryInfo(node.getBatteryLevel(), weight));
                avgNodeLoad = avgNodeLoad + weight * ((node.getBatteryLevel() - avgNodeLoad) / advReplyCount);
                if (advReplyCount == 1) {
                    varianceNodeLoad = 0;
                } else {
                    varianceNodeLoad = varianceNodeLoad
                            + weight * ((node.getBatteryLevel() - lastAvgNodeLoad)
                            * (node.getBatteryLevel() - avgNodeLoad));
                }

                return isSuitable;
            }
        }).start();
    }

    void executeMethodLocally(String taskId, int loadDistributed) {
        System.out.println("Executing locally");
        TaskLookup taskLookup = taskLookups.get(taskId);
        Task taskToDistribute = (taskLookups.get(taskId)).getTask();
        Node host_node = nodes.get(host);
        int remaining_battery_load = host_node.getBatteryLevel()
                - (loadDistributed
                + Preferences.BATTERY_SPENT_IN_TASK_CHUNK_EXECUTION
                + host_node.getPromisedBatteryLoad());
        int batteryLoadCanServe = 0;
        if (remaining_battery_load > Preferences.RESERVED_BATTERY_AT_NODE) {
            batteryLoadCanServe = taskToDistribute.getPromisedBatteryTaskLoad();
        } else {
            // Need to change this to avoid fragmentation
            batteryLoadCanServe = host_node.getBatteryLevel()
                    - (Preferences.RESERVED_BATTERY_AT_NODE
                    + Preferences.BATTERY_SPENT_IN_TASK_CHUNK_EXECUTION
                    + host_node.getPromisedBatteryLoad());
        }
        if (batteryLoadCanServe < taskToDistribute.getPromisedBatteryTaskLoad()) {
            return;
        }
//        taskToDistribute.setPromisedTaskBatteryLoad(batteryLoadCanServe
//                + Preferences.BATTERY_SPENT_IN_TASK_CHUNK_EXECUTION);
        taskToDistribute.setPromisedTimeStamp(new Date().getTime());
        host_node.incrPromisedBatteryLoad(taskToDistribute.getPromisedBatteryTaskLoad());

        //add processor, memory load
        host_node.incrProcessorLoad(taskToDistribute.getTaskProcessorLoad());
        host_node.incrMemoryLoad(taskToDistribute.getTaskMemoryLoad());
        DistributedTask dsTask = new DistributedTask(taskToDistribute.getTaskProcessorLoad(),
                taskToDistribute.getTaskMemoryLoad(),
                taskToDistribute.getPromisedBatteryTaskLoad(), taskId,
                taskToDistribute.getSource(),
                taskLookup.getClassName(), taskLookup.getMethodName(), taskLookup.getParams());
        TaskChunk taskChunk = new TaskChunk(taskId, nodes.get(host), taskLookup.nextSequenceNumber(),
                dsTask, host + (++taskAdvReplyId));
        taskLookup.addToTaskGroup(taskChunk.getTaskAdvReplyId(), taskChunk);
        taskLookup.addToResultTracker(taskChunk.getSequenceNumber(), taskChunk.getTaskAdvReplyId());
        TaskResult taskResult = new TaskResult(dsTask.getTaskProcessorLoad(),
                dsTask.getTaskMemoryLoad(),
                dsTask.getTaskBatteryLoad(),
                dsTask.taskId, host,
                handleDistributedTask(dsTask),
                taskChunk.getSequenceNumber());
        //decrease battery load
        host_node.decrBatteryLevel(taskToDistribute.getPromisedBatteryTaskLoad());
        //decrease processor/memory load
        host_node.decrProcessorLoad(dsTask.getTaskProcessorLoad());
        host_node.decrMemoryLoad(dsTask.getTaskMemoryLoad());
        host_node.decrPromisedBatteryLoad(taskToDistribute.getPromisedBatteryTaskLoad());
        Integer seqNumber = taskResult.getSeqNumber();
        taskLookup = taskLookups.get(taskResult.getTaskId());
        synchronized (taskLookup) {
            taskLookup.getTaskGroup().get(taskLookup.getResultTracker().get(taskResult.getSeqNumber())).setStatus(Preferences.TASK_CHUNK_STATUS.RECEIVED);
            taskLookup.removeFromResultTracker(seqNumber);
            addAndMergeResults(taskResult);
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

    private void addAndMergeResults(TaskResult taskResult) {
        TaskLookup taskLookup = taskLookups.get(taskResult.getTaskId());
        taskLookup.getTaskResults().put(taskResult.getSeqNumber(), taskResult);
        if (taskLookup.getTask().getTaskBatteryLoad() <= 0
                && taskLookup.getTaskGroup().size() == taskLookup.getTaskResults().size()) {
            taskLookup.setStatus(Preferences.TASK_STATUS.RECEIVED_RESULTS);
        }
        //Merge results
        if (taskLookup.getStatus() == Preferences.TASK_STATUS.RECEIVED_RESULTS) {
            String result = "";
            for (Integer rs:taskLookup.getTaskResults().keySet()) {
                TaskResult tempTaskResult = taskLookup.getTaskResults().get(rs);
                System.out.println(host + ":: received result from " + tempTaskResult.getSource() + " for load " + (tempTaskResult.getTaskBatteryLoad() - Preferences.BATTERY_SPENT_IN_TASK_CHUNK_EXECUTION));
                result += (taskLookup.getTaskResults().get(rs)).toString() + " ";
            }
            logMessage("Result:\n" + result);
            nodes.get(host).decrBatteryLevel(Preferences.BATTERY_SPENT_IN_TASK_DISTRIBUTION);
        }
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
                        taskLookups.get(((DistributedTask) taskToDistribute).getTaskId()).addToResultTracker(taskChunk.getSequenceNumber(), taskChunk.getTaskAdvReplyId());
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
                    taskToDistribute.setTaskBatteryLoad(taskToDistribute.getTaskBatteryLoad() + loadDistributed);
                    if (taskToDistribute.getTaskBatteryLoad() > 0) {
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
                            synchronized (nodes) {
                                Node updateNode = nodes.get(host);
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

    public void distribute(String className, String methodName, Serializable[] params, Integer taskBatteryLoad) {
        String nodeName = "";
        String msgKind = "";
        String msgId = "";
        taskNum++;
        final String taskId = host + taskNum;
        Task newTask = new Task(Preferences.TASK_DEFAULT_CPU_LOAD, Preferences.TASK_DEFAULT_MEMORY_LOAD,
                taskBatteryLoad, taskId, host);
        logMessage("Task advertised");
        TaskLookup taskLookup = new TaskLookup(newTask, className, methodName, params);
        synchronized (taskLookups) {
            taskLookups.put(taskId, taskLookup);
        }
        if (newTask.getTaskBatteryLoad() <= Preferences.MINIMUM_FRAGMENTATION_LOAD) {
            if (newTask.getTaskBatteryLoad() > 0) {
                executeTaskLocally(newTask.getTaskId(), newTask.getTaskBatteryLoad());
                taskLookups.remove(taskId);
                return;
            }
        }
        Message advMsg = new Message(nodeName, msgKind, msgId, newTask, host);
        advMsg.setSource(host);
        advMsg.setNormalMsgType(Message.NormalMsgType.TASK_ADV);
        sendAndRetryTaskAdv(taskLookup, advMsg);
        Preferences.crashNode = "";
    }

    private void sendAndRetryTaskAdv(final TaskLookup taskLookup,
            final Message advMsg) {

        (new Thread() {

            public void run() {
                boolean cannotDistribute = false;
                // if it still hasn't exhausted its retry and still hasn't got
                // enough reply
                while (taskLookup.getStatus() == Preferences.TASK_STATUS.ADVERTISED
                        && taskLookup.getRetry() < Preferences.NUMBER_OF_RETRIES_BEFORE_QUITTING
                        && taskLookup.getTask().getTaskBatteryLoad()
                        > Preferences.MINIMUM_FRAGMENTATION_LOAD) {

                    logMessage("Tasklookup status : " + taskLookup.getStatus()
                            + " Retries: " + taskLookup.getRetry());
                    // send task request advertisement, ask for bid
                    try {
                        synchronized (mp) {
                            synchronized (nodes) {
                                Collection<Node> nodesTemp = nodes.values();
                                for (Node n : nodesTemp) {
                                    if (!n.getName().equalsIgnoreCase(host)) {
                                        advMsg.setDest(n.getName());
                                        if (taskLookup.getStatus() == Preferences.TASK_STATUS.DISTRIBUTED
                                                || taskLookup.getTask().getTaskBatteryLoad()
                                                <= Preferences.MINIMUM_FRAGMENTATION_LOAD) {
                                            break;
                                        }
                                        mp.send(advMsg);
                                        Thread.sleep(100);
                                    }
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
                        } else
                            cannotDistribute = true;
                    } catch (InterruptedException e) {
                    }
                }
                if (cannotDistribute==true || (taskLookup.getStatus() == Preferences.TASK_STATUS.ADVERTISED
                        && taskLookup.getTask().getTaskBatteryLoad() > 0
                        && taskLookup.getTask().getTaskBatteryLoad()
                        <= Preferences.MINIMUM_FRAGMENTATION_LOAD)) {
                    int load = taskLookup.getTask().getTaskBatteryLoad();
                    taskLookup.getTask().setTaskBatteryLoad(0);
                    taskLookup.getTask().setPromisedTaskBatteryLoad(load
                        + Preferences.BATTERY_SPENT_IN_TASK_CHUNK_EXECUTION);
                    executeMethodLocally(taskLookup.getTask().getTaskId(), load);
                }
            }
        }).start();
    }

    private void clearDeadPromisedLoad() {
        (new Thread() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(Preferences.WAIT_TIME_BEFORE_REMOVING_DEAD_PROMISES);
                        synchronized (nodes) {
                            Node host_node = nodes.get(host);
                            long currTime = new Date().getTime();
                            List<String> removedTasks = new ArrayList<String>();
                            for (String taskId : host_node.getAcceptedTasks().keySet()) {
                                Task task = host_node.getAcceptedTasks().get(taskId);
                                if (!task.isInExecution()
                                        && ((currTime - task.getPromisedTimeStamp())
                                        > Preferences.TIMEOUT_LOAD_PROMISE)) {
                                    removedTasks.add(taskId);
                                }
                            }
                            for (String taskId : removedTasks) {
                                Task task = host_node.getAcceptedTaskByTaskId(taskId);
                                //decrease promise
                                host_node.decrPromisedBatteryLoad(task.getPromisedBatteryTaskLoad());
                                host_node.removeFromAcceptedTask(taskId);

                                //decrease processor/memory load
                                host_node.decrProcessorLoad(task.getTaskProcessorLoad());
                                host_node.decrMemoryLoad(task.getTaskMemoryLoad());

                            }
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TaskDistributor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }).start();
    }

    private class BatteryInfo {

        private Integer batteryLoad;
        private float weight;

        public BatteryInfo(Integer batteryLoad, float weight) {
            super();
            this.batteryLoad = batteryLoad;
            this.weight = weight;
        }

        public Integer getBatteryLoad() {
            return batteryLoad;
        }

        public void setBatteryLoad(Integer batteryLoad) {
            this.batteryLoad = batteryLoad;
        }

        public float getWeight() {
            return weight;
        }

        public void setWeight(float weight) {
            this.weight = weight;
        }
    }

    public Map<String, Node> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, Node> nodes) {
        this.nodes = nodes;
    }

    public void executeTaskLocally(String methodName, Integer taskLoad) {
        int remainingLoad = Preferences.TOTAL_PROCESSOR_LOAD_AT_NODE - Preferences.RESERVED_PROCESSOR_AT_NODE;
        int taskLoops = (int) Math.ceil((float) taskLoad / remainingLoad);
        SampleApplicationLocal localApp = new SampleApplicationLocal();
        ArrayList<ArrayList<Double>> mfcc_parameters = new ArrayList<ArrayList<Double>>();
        for (int i = 0; i < taskLoops; i++) {
            mfcc_parameters.add(localApp.method1(10, 20));
        }
        logMessage("Local Result from " + host + ":" + mfcc_parameters.toString());
    }
}
