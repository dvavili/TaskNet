package ds.android.tasknet.infrastructure;

import ds.android.tasknet.application.SampleApplicationLocal;
import ds.android.tasknet.clock.ClockFactory;
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
    ClockFactory.ClockType clock;
    public static int taskNum = 0;
    HashMap<String, TaskLookup> taskLookups;
    Map<String, Map<Integer, TaskResult>> taskResults = new HashMap<String, Map<Integer, TaskResult>>();
    Integer taskAdvReplyId = 0;

    public TaskDistributor(String host_name, String conf_file, String clockType) {
        prop = new Properties();
        Preferences.setHostDetails(conf_file, host_name);
        try {
            prop.load(new FileInputStream(conf_file));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (clockType.equalsIgnoreCase("default")) {
            clockType = prop.getProperty("clock.type");
        }
        if (clockType.equalsIgnoreCase("logical")) {
            clock = ClockFactory.ClockType.LOGICAL;
            mp = new MessagePasser(conf_file, host_name, ClockFactory.ClockType.LOGICAL, 1);
        } else if (clockType.equalsIgnoreCase("vector")) {
            clock = ClockFactory.ClockType.VECTOR;
            mp = new MessagePasser(conf_file, host_name, ClockFactory.ClockType.VECTOR, Preferences.nodes.size());
        }
        host = host_name;
//        host_index = Preferences.n
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
                            if (msg instanceof MulticastMessage) {
                                switch (((MulticastMessage) msg).getMessageType()) {
                                    case TASK_ADV:
                                        Task receivedTask = (Task) msg.getData();
                                        synchronized (Preferences.nodes) {
                                            logMessage("Received task advertisement from: " + ((MulticastMessage)msg).getSource());
                                            Node host_node = Preferences.nodes.get(host);
                                            float remaining_load = Preferences.TOTAL_LOAD_AT_NODE
                                                    - (receivedTask.taskLoad + host_node.getProcessorLoad());
                                            float loadCanServe = 0;
                                            if (remaining_load > Preferences.host_reserved_load) {
                                                loadCanServe = receivedTask.taskLoad;
                                            } else {
                                                // Need to change this to avoid fragmentation
                                                loadCanServe = Preferences.TOTAL_LOAD_AT_NODE
                                                        - (Preferences.host_reserved_load + host_node.getProcessorLoad());
                                            }
                                            if (loadCanServe > Preferences.MINIMUM_LOAD_REQUEST) {
                                                Integer tempTaskAdvReplyId = ++taskAdvReplyId;
                                                String tempTaskAdvReplyIdStr = host_node.getName() + tempTaskAdvReplyId;
                                                host_node.addToAcceptedTask(tempTaskAdvReplyIdStr, receivedTask);
                                                TaskAdvReply taskAdvReply =
                                                        new TaskAdvReply(tempTaskAdvReplyIdStr, receivedTask.getTaskId(), host_node, loadCanServe);
                                                Message profileMsg = new Message(((MulticastMessage) msg).getSource(),
                                                        "", "", taskAdvReply);
                                                profileMsg.setNormalMsgType(Message.NormalMsgType.PROFILE_XCHG);
                                                try {
                                                    mp.send(profileMsg);
                                                } catch (InvalidMessageException ex) {
                                                    host_node.removeFromAcceptedTask(tempTaskAdvReplyIdStr);
                                                    ex.printStackTrace();
                                                }
                                                logMessage("Sent message profile for task: "
                                                        + receivedTask.taskId + " " + remaining_load);
                                            } else {
                                                logMessage("Node Overloaded: " + receivedTask.taskId + " " + loadCanServe);
                                            }
                                        }
                                        break;
                                }
                            } else {
                                switch (msg.getNormalMsgType()) {
                                    case NORMAL:
                                        logMessage(msg.getData().toString());
                                        break;
                                    case PROFILE_XCHG:
                                        TaskAdvReply taskAdvReply = (TaskAdvReply) msg.getData();
//                                        taMessages.append(taskAdvReply + "\n");
                                        TaskLookup taskLookup = taskLookups.get(taskAdvReply.getTaskId());
                                        taskLookup.setRetry(0);
                                        // Mistake here taskLookup maintains Sequence Number
                                        // TaskAdvReplyId is in String
                                        // Instead use getSequenceNumber method. Sequence Number is unique to each task.
//                                    	if(!taskLookup.getTaskGroup().containsKey(taskAdvReply.getTaskAdvReplyId())) {
                                        distributeTask(taskAdvReply);
//                                    	}
                                        break;
                                    case DISTRIBUTED_TASK:
                                        TaskChunk taskChunk = (TaskChunk) msg.getData();
//                                        taMessages.append(taskChunk.toString());
                                        DistributedTask distTask = taskChunk.getDsTask();
                                        TaskResult result;
                                        Map<Integer, TaskResult> tempResults = taskResults.get(distTask.getTaskId());
//                                        if (tempResults != null
//                                                && tempResults.get(distTask.getSeqNumber()) != null) {
//                                            result = taskResults.get(distTask.getTaskId()).get(taskChunk.getSequenceNumber());
//                                        } else {
                                        result = new TaskResult(distTask.getTaskLoad(),
                                                distTask.taskId, host,
                                                handleDistributedTask(distTask), taskChunk.getSequenceNumber());

                                        if (tempResults == null) {
                                            tempResults = new HashMap<Integer, TaskResult>();
                                        }

                                        tempResults.put(taskChunk.getSequenceNumber(), result);
                                        taskResults.put(distTask.getTaskId(), tempResults);
//                                        }
                                        Node host_node = Preferences.nodes.get(host);
                                        host_node.removeFromAcceptedTask(taskChunk.getTaskAdvReplyId());
                                        //decrease promise

                                        logMessage(result.getTaskResult().toString());
                                        Message resultMsg = new Message(distTask.getSource(), "", "", result);
                                        resultMsg.setNormalMsgType(Message.NormalMsgType.TASK_RESULT);
                                        try {
                                            mp.send(resultMsg);
                                        } catch (InvalidMessageException ex) {
                                            Logger.getLogger(TaskDistributor.class.getName()).log(Level.SEVERE, null, ex);
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
                    if (gotTask.getParameters() != null && ((mthdDef.get(gotTask.getMethodName())).length != gotTask.getParameters().length)) {
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

                int loadDistributed = (int) Math.ceil((taskToDistribute.getTaskLoad() > taskAdvReply.getLoadCanServe())
                        ? (taskAdvReply.getLoadCanServe()) : taskToDistribute.getTaskLoad());

                //synchronized (taskLookups) {
                taskToDistribute.setTaskLoad(taskToDistribute.getTaskLoad() - loadDistributed);
                //}
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
                Message distMsg = new Message(node.getName(), "", "", taskChunk);
                distMsg.setNormalMsgType(Message.NormalMsgType.DISTRIBUTED_TASK);

                sendAndRetryTaskChunk(taskChunk, distMsg, dsTask, loadDistributed, taskLookup);

            }

            private void addAndMergeResults(TaskResult taskResult) {
                TaskLookup taskLookup = taskLookups.get(taskResult.getTaskId());
                taskLookup.getTaskResults().put(taskResult.getSeqNumber(), taskResult);
                if (taskLookup.getTask().getTaskLoad() <= 0 && taskLookup.getTaskGroup().size() == taskLookup.getTaskResults().size()) {
                    taskLookup.setStatus(Preferences.TASK_STATUS.RECEIVED_RESULTS);
                }
                //Merge results
                if (taskLookup.getStatus() == Preferences.TASK_STATUS.RECEIVED_RESULTS) {
                    String result = "";
                    for (int i = 0; i < taskLookup.getTaskResults().size(); i++) {
                        result += (taskLookup.getTaskResults().get(i)).toString() + " ";
                    }
                    logMessage("Result:\n" + result);
                }
            }
        }).start();
    }

    private void logMessage(String msgString) {
        Message logMsg = new Message(Preferences.LOGGER_NAME, "", "", msgString + "\n");
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
                //		synchronized (taskLookup) {
//        			synchronized (taskChunk) {
                //      				synchronized (taskToDistribute) {

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
                    taskToDistribute.setTaskLoad(taskToDistribute.getTaskLoad() + loadDistributed);
                    if (taskToDistribute.getTaskLoad() > 0) {
                        taskLookup.setStatus(Preferences.TASK_STATUS.ADVERTISED);
                    }
                }
            }
            //    			}
            //    		}
            //}
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
                                updateNode.setBatteryLevel(1);
                                Message profileUpdate = new Message(Preferences.LOGGER_NAME, "", "", updateNode);
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

    public void distributeTask(Integer taskLoadFromUI) {
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

        sendAndRetryTaskAdv(taskLookup, nodeName, msgKind, msgId, newTask);
        Preferences.crashNode = "";
    }

    private void sendAndRetryTaskAdv(final TaskLookup taskLookup, final String nodeName,
            final String msgKind, final String msgId, final Task newTask) {

        (new Thread() {

            public void run() {
                //synchronized (taskLookup) {
                //if it still hasn't exhausted its retry and still hasn't got enough reply
                MulticastMessage advMsg;
                while (taskLookup.getStatus() == Preferences.TASK_STATUS.ADVERTISED
                        && taskLookup.getRetry() < Preferences.NUMBER_OF_RETRIES_BEFORE_QUITTING) {

                    logMessage("Tasklookup status : " + taskLookup.getStatus() + " Retries: " + taskLookup.getRetry());
                    //send task request advertisement, ask for bid
                    try {
                        advMsg = new MulticastMessage(nodeName, msgKind, msgId,
                                newTask, mp.getClock(), true, MulticastMessage.MessageType.TASK_ADV, host);
                        mp.send(advMsg);                        
                        System.out.println("Multicast time: " + advMsg.getClockService().getTime());
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
                //}
            }
        }).start();
    }

    /**
     * Method to start listening for incoming connections
     */
    public void startListening() {
        mp.start();
    }
}
